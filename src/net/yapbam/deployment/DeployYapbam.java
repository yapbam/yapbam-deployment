package net.yapbam.deployment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

import net.yapbam.util.SecureDownloader;
import net.yapbam.util.SecureDownloader.DownloadInfo;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

/** Deploys the Yapbam updates on its web site.
 * <br>This class requires that the Yapbam build has been already done with success.
 * <br>It verifies that all material is ready to be deployed.
 * @author Jean-Marc-Marc Astesana
 */
public class DeployYapbam implements AutoCloseable {
	private static final String RELEASE_ROOT = "sftp://web.sourceforge.net/home/pfs/project/yapbam";
	/** Permissions for temp files: readable and writable only by the owner. */
	private static final FileAttribute<java.util.Set<PosixFilePermission>> TEMP_FILE_PERMISSIONS =
			java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.util.EnumSet.of(
					PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
	private final String webRoot;
	private boolean onlyBeta; 
	private DefaultFileSystemManager fsManager;
	private FileSystemOptions sfOpts;
	private FileSystemOptions webOpts;
	private SrcDescription src;
	private FileSelector dummySelector;
	
	DeployYapbam(String sfUser, String sfPassword, String webRoot, String webUser, String webPassword, String srcPath, String newVersion, String oldVersion, boolean onlyBeta) throws FileSystemException {
		this.src = new SrcDescription(new File(srcPath), newVersion, new Date(), oldVersion);
		this.onlyBeta = onlyBeta;
		this.webRoot = webRoot;
		fsManager = (DefaultFileSystemManager) VFS.getManager();
		sfOpts = createSftpOpts(sfUser, sfPassword);
		webOpts = createSftpOpts(webUser, webPassword);
	}

	private static FileSystemOptions createSftpOpts(String user, String password) throws FileSystemException {
		FileSystemOptions opts = new FileSystemOptions();
		SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");
		SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, false); // Use absolute paths
		StaticUserAuthenticator auth = new StaticUserAuthenticator(null, user, password);
		DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
		// SourceForge's SSH server requires keyboard-interactive authentication.
		// JSch needs a UserInfo to answer the keyboard-interactive prompts, otherwise
		// it fails with "Auth cancel" before ever trying the plain password method.
		SftpFileSystemConfigBuilder.getInstance().setUserInfo(opts, new PasswordUserInfo(password));
		return opts;
	}

	/** A simple UserInfo that provides the password for keyboard-interactive authentication. */
	private static final class PasswordUserInfo implements UserInfo, UIKeyboardInteractive {
		private final String password;

		PasswordUserInfo(String password) {
			this.password = password;
		}

		@Override
		public String getPassphrase() {
			return null;
		}

		@Override
		public String getPassword() {
			return password;
		}

		@Override
		public boolean promptPassword(String message) {
			return true;
		}

		@Override
		public boolean promptPassphrase(String message) {
			return false;
		}

		@Override
		public boolean promptYesNo(String message) {
			return true;
		}

		@Override
		public void showMessage(String message) {
			// Do nothing
		}

		@Override
		public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo) {
			// SourceForge sends a single password prompt; answer it with the configured password.
			if (prompt == null || prompt.length == 0) {
				return new String[0];
			}
			String[] responses = new String[prompt.length];
			for (int i = 0; i < prompt.length; i++) {
				responses[i] = password;
			}
			return responses;
		}
	}
	
	@Override
	public void close() throws IOException {
		System.out.println ("Closing source forge connection");
		fsManager.close();
	}

	protected void doIt() throws IOException {
		boolean trace = true;
		doAutoUpdate(trace);
		if (!onlyBeta) {
			doRelease(trace);
			doDoc(trace);
			doPad(trace);
		}
		System.out.println ("Finished :-)");
	}
	
	private void doPad(boolean trace) throws FileSystemException {
		System.out.println ("Updating pad file");
		File f = buildPad();
		if (trace) System.out.println ("  Uploading pad file ...");
		fsManager.resolveFile(webRoot+"/pad_file.xml", webOpts).copyFrom(fsManager.toFileObject(f), getDummySelector());
	}

	private File buildPad() throws FileSystemException {
		try {
			String release = this.src.getNewVersion();
			String date = new SimpleDateFormat("ddMMyyyy").format(this.src.getReleaseDate());
			String day = date.substring(0, 2);
			String month = date.substring(2, 4);
			String year = date.substring(4);
			long bytes = this.src.getZipFile().length();
			String sbytes = Long.toString(bytes);
			String skbytes = Long.toString(bytes/1024);
			String smbytes = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.US)).format(1.0*bytes/1024/1024);
			
			try (BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/pad_file.xml"), StandardCharsets.UTF_8))) {
				File file = Files.createTempFile("fileTemplate", ".txt", TEMP_FILE_PERMISSIONS).toFile();
				file.deleteOnExit();
				PrintStream out = new PrintStream(file);
				try {
					String line;
					while ((line = in.readLine()) != null) {
						String replaced = line.replace("{0}", release);
						replaced = replaced.replace("{1}", day);
						replaced = replaced.replace("{2}", month);
						replaced = replaced.replace("{3}", year);
						replaced = replaced.replace("{4}", sbytes);
						replaced = replaced.replace("{5}", skbytes);
						replaced = replaced.replace("{6}", smbytes);
						out.println(replaced);
					}
					return file;
				} finally {
					out.close();
				}
			}
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}
	
	private File buildUpdateInfo() throws FileSystemException {
		try {
			String release = this.src.getNewVersion();
			File file = Files.createTempFile("updateInfoInclude", ".txt", TEMP_FILE_PERMISSIONS).toFile();
			file.deleteOnExit();
			try (PrintStream out = new PrintStream(file)) {
				out.println ("lastestRelease="+getVersion(src.getZipFile().getAbsolutePath()));
				out.println ("updateURL=https://sourceforge.net/project/platformdownload.php?group_id=276272");
				out.println ();
				SecureDownloader sd = new SecureDownloader(Proxy.NO_PROXY);
				String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
				String zipURL = "https://yapbam.sourceforge.net/update"+release+"/yapbam-"+release+".zip?timestamp="+timestamp;
				out.println ("autoUpdateURL="+zipURL);
				DownloadInfo info = sd.download(new URL(zipURL), null);
				out.println ("autoUpdateCHKSUM="+info.getCheckSum());
				out.println ("autoUpdateSize="+info.getDownloadedSize());
				out.println ();
		
				String updaterURL = "https://yapbam.sourceforge.net/update"+release+"/updater.jar?timestamp="+timestamp;
				out.println ("autoUpdateUpdaterURL="+updaterURL);
				info = sd.download(new URL(updaterURL), null);
				out.println ("autoUpdateUpdaterCHKSUM="+info.getCheckSum());
				out.println ("autoUpdateUpdaterSize="+info.getDownloadedSize());
				return file;
			}
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}
	
	private String getVersion(String zipPath) throws IOException {
		String fname = "jar:zip:file://"+zipPath+"!/App/program.jar!/net/yapbam/update/version.txt";
		try (FileObject fileObject = fsManager.resolveFile(fname)) {
			try (InputStream in = fileObject.getContent().getInputStream()) {
			    try (Scanner s = new Scanner(in).useDelimiter("^.+=")) {
			        return s.next();
			    }
			}
		}
	}


	private void doAutoUpdate(boolean trace) throws FileSystemException {
		System.out.println ("Setting up auto update");
		if (trace) System.out.println ("  Create update folder in "+webRoot+" ...");
		String updateFolder = webRoot+"/update"+this.src.getNewVersion();
		fsManager.resolveFile(updateFolder, webOpts).createFolder();
		if (trace) System.out.println ("  Copying zip ("+this.src.getZipFile()+") to update folder ...");
		fsManager.resolveFile(updateFolder+"/"+this.src.getZipFile().getName(), webOpts).copyFrom(fsManager.toFileObject(this.src.getZipFile()), getDummySelector());;
		if (trace) System.out.println ("  update.jar ("+this.src.getUpdaterFile()+") to update folder ...");
		fsManager.resolveFile(updateFolder+"/"+this.src.getUpdaterFile().getName(), webOpts).copyFrom(fsManager.toFileObject(this.src.getUpdaterFile()), getDummySelector());
		
		if (trace) System.out.println ("  updating auto-update info ...");
		File file = buildUpdateInfo();
		if (!onlyBeta) {
			fsManager.resolveFile(webRoot+"/updateInfoInclude.txt", webOpts).copyFrom(fsManager.toFileObject(file), getDummySelector());
		}
		fsManager.resolveFile(webRoot+"/updateInfoBetaInclude.txt", webOpts).copyFrom(fsManager.toFileObject(file), getDummySelector());
		
		// Delete old update (if it exists)
		FileObject oldUpdateFolder = fsManager.resolveFile(webRoot+"/update"+this.src.getOldVersion(), webOpts);
		if (oldUpdateFolder.exists()) {
			if (trace) System.out.println ("  Delete obsolete update folder in "+webRoot+" ...");
			oldUpdateFolder.delete(getDummySelector());
		}
	}

	private void doRelease(boolean trace) throws FileSystemException {
		System.out.println ("Posting release");
		if (trace) System.out.println ("  Copying zip to sourceforge ...");
		fsManager.resolveFile(RELEASE_ROOT+"/yapbam/"+this.src.getZipFile().getName(), sfOpts).copyFrom(fsManager.toFileObject(this.src.getZipFile()), getDummySelector());
		if (trace) System.out.println ("  Copying exe to sourceforge ...");
		fsManager.resolveFile(RELEASE_ROOT+"/yapbam/"+this.src.getExeFile().getName(), sfOpts).copyFrom(fsManager.toFileObject(this.src.getExeFile()), getDummySelector());
		if (trace) System.out.println ("  Copying exe to "+webRoot+"/directDownload ...");
		fsManager.resolveFile(webRoot+"/directDownload/"+this.src.getExeFile().getName(), webOpts).copyFrom(fsManager.toFileObject(this.src.getExeFile()), getDummySelector());
	}

	private void doDoc(boolean trace) throws FileSystemException {
		System.out.println ("Copying release notes ...");
		// Relnotes
		File file = src.getRelNotesFile();
		fsManager.resolveFile(webRoot+"/en/doc/"+file.getName(), webOpts).copyFrom(fsManager.toFileObject(file), getDummySelector());
		file = src.getRelNotesFrFile();
		fsManager.resolveFile(webRoot+"/fr/doc/"+file.getName(), webOpts).copyFrom(fsManager.toFileObject(file), getDummySelector());
	}

	private FileSelector getDummySelector() {
		if (dummySelector==null) {
			dummySelector = new FileSelector() {
				@Override
				public boolean traverseDescendents(FileSelectInfo arg0) throws Exception {
					return true;
				}
				
				@Override
				public boolean includeFile(FileSelectInfo arg0) throws Exception {
					return true;
				}
			};
		}
		return dummySelector;
	}
}
