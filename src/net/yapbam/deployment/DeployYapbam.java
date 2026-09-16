package net.yapbam.deployment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
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

import net.yapbam.util.CheckSum;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
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
	
	DeployYapbam(String sfUser, String sfPassword, String webRoot, String webUser, String webPassword, String srcPath, String newVersion, String oldVersion, boolean onlyBeta) {
		this.src = new SrcDescription(new File(srcPath), newVersion, new Date(), oldVersion);
		this.onlyBeta = onlyBeta;
		this.webRoot = webRoot;
		sfOpts = createSftpOpts(sfUser, sfPassword);
		webOpts = createSftpOpts(webUser, webPassword);
	}

	/** Lazily creates the file system manager on first use.
	 * <br>We use our own manager (not the VFS.getManager() singleton) so close() can
	 * close this deployment's connections without breaking subsequent deployments.
	 * @return The file system manager.
	 * @throws FileSystemException if the manager cannot be initialized.
	 */
	private DefaultFileSystemManager getFsManager() throws FileSystemException {
		if (fsManager == null) {
			StandardFileSystemManager manager = new StandardFileSystemManager();
			try {
				manager.init();
			} catch (FileSystemException e) {
				manager.close();
				throw e;
			}
			fsManager = manager;
		}
		return fsManager;
	}

	private static FileSystemOptions createSftpOpts(String user, String password) {
		FileSystemOptions opts = new FileSystemOptions();
		try {
			SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");
		} catch (FileSystemException e) {
			// "no" is a valid constant, this can't happen
			throw new IllegalStateException(e);
		}
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
		if (fsManager != null) {
		System.out.println ("Closing connections");
			fsManager.close();
		}
	}

	protected void doIt() throws IOException {
		boolean trace = true;
		checkConnections();
		doAutoUpdate(trace);
		if (!onlyBeta) {
			doRelease(trace);
			doDoc(trace);
			doPad(trace);
		}
		System.out.println ("Finished :-)");
	}
	
	/** Validates that the required connections can be established before any transfer.
	 * <br>This fails fast on wrong credentials or unreachable hosts, avoiding partial deployments.
	 * @throws FileSystemException if a connection fails or a root folder does not exist.
	 */
	private void checkConnections() throws FileSystemException {
		// exists() forces an SSH connect + authentication and a stat on the remote folder.
		// It also caches the type of the web root as FOLDER, so createFolder on the update
		// subfolder won't try to recreate the parent (which fails with Permission denied
		// if the user can't write to the web root's parent directory).
		FileObject webRootObj = getFsManager().resolveFile(webRoot, webOpts);
		if (!webRootObj.exists()) {
			throw new FileSystemException("vfs.provider/create-folder.error", webRoot,
					new IOException("Web root " + webRoot + " does not exist. Please use an absolute path"));
		}
		if (!onlyBeta) {
			// Verify the SourceForge connection and release root before deploying anything.
			FileObject releaseRoot = getFsManager().resolveFile(RELEASE_ROOT, sfOpts);
			if (!releaseRoot.exists()) {
				throw new FileSystemException("vfs.provider/create-folder.error", RELEASE_ROOT,
						new IOException("SourceForge release root does not exist: " + RELEASE_ROOT));
			}
		}
	}
	
	private void doPad(boolean trace) throws FileSystemException {
		System.out.println ("Updating pad file");
		File f = buildPad();
		if (trace) System.out.println ("  Uploading pad file ...");
		getFsManager().resolveFile(webRoot+"/pad_file.xml", webOpts).copyFrom(getFsManager().toFileObject(f), getDummySelector());
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
				String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
				String zipURL = "https://yapbam.sourceforge.net/update"+release+"/yapbam-"+release+".zip?timestamp="+timestamp;
				out.println ("autoUpdateURL="+zipURL);
				out.println ("autoUpdateCHKSUM="+CheckSum.toString(CheckSum.getChecksum(src.getZipFile())));
				out.println ("autoUpdateSize="+src.getZipFile().length());
				out.println ();
		
				String updaterURL = "https://yapbam.sourceforge.net/update"+release+"/updater.jar?timestamp="+timestamp;
				out.println ("autoUpdateUpdaterURL="+updaterURL);
				out.println ("autoUpdateUpdaterCHKSUM="+CheckSum.toString(CheckSum.getChecksum(src.getUpdaterFile())));
				out.println ("autoUpdateUpdaterSize="+src.getUpdaterFile().length());
				return file;
			}
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}
	
	private String getVersion(String zipPath) throws IOException {
		String fname = "jar:zip:file://"+zipPath+"!/App/program.jar!/net/yapbam/update/version.txt";
		try (FileObject fileObject = getFsManager().resolveFile(fname)) {
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
		FileObject updateFolderObj = getFsManager().resolveFile(updateFolder, webOpts);
		if (!updateFolderObj.exists()) {
			updateFolderObj.createFolder();
		}
		if (trace) System.out.println ("  Copying zip ("+this.src.getZipFile()+") to update folder ...");
		getFsManager().resolveFile(updateFolder+"/"+this.src.getZipFile().getName(), webOpts).copyFrom(getFsManager().toFileObject(this.src.getZipFile()), getDummySelector());;
		if (trace) System.out.println ("  update.jar ("+this.src.getUpdaterFile()+") to update folder ...");
		getFsManager().resolveFile(updateFolder+"/"+this.src.getUpdaterFile().getName(), webOpts).copyFrom(getFsManager().toFileObject(this.src.getUpdaterFile()), getDummySelector());
		
		if (trace) System.out.println ("  updating auto-update info ...");
		File file = buildUpdateInfo();
		if (!onlyBeta) {
			getFsManager().resolveFile(webRoot+"/updateInfoInclude.txt", webOpts).copyFrom(getFsManager().toFileObject(file), getDummySelector());
		}
		getFsManager().resolveFile(webRoot+"/updateInfoBetaInclude.txt", webOpts).copyFrom(getFsManager().toFileObject(file), getDummySelector());
		
		// Delete old update (if it exists)
		FileObject oldUpdateFolder = getFsManager().resolveFile(webRoot+"/update"+this.src.getOldVersion(), webOpts);
		if (oldUpdateFolder.exists()) {
			if (trace) System.out.println ("  Delete obsolete update folder in "+webRoot+" ...");
			oldUpdateFolder.delete(getDummySelector());
		}
	}

	private void doRelease(boolean trace) throws FileSystemException {
		System.out.println ("Posting release");
		if (trace) System.out.println ("  Copying zip to sourceforge ...");
		getFsManager().resolveFile(RELEASE_ROOT+"/yapbam/"+this.src.getZipFile().getName(), sfOpts).copyFrom(getFsManager().toFileObject(this.src.getZipFile()), getDummySelector());
		if (trace) System.out.println ("  Copying exe to sourceforge ...");
		getFsManager().resolveFile(RELEASE_ROOT+"/yapbam/"+this.src.getExeFile().getName(), sfOpts).copyFrom(getFsManager().toFileObject(this.src.getExeFile()), getDummySelector());
		if (trace) System.out.println ("  Copying exe to "+webRoot+"/directDownload ...");
		getFsManager().resolveFile(webRoot+"/directDownload/"+this.src.getExeFile().getName(), webOpts).copyFrom(getFsManager().toFileObject(this.src.getExeFile()), getDummySelector());
	}

	private void doDoc(boolean trace) throws FileSystemException {
		System.out.println ("Copying release notes ...");
		// Relnotes
		File file = src.getRelNotesFile();
		getFsManager().resolveFile(webRoot+"/en/doc/"+file.getName(), webOpts).copyFrom(getFsManager().toFileObject(file), getDummySelector());
		file = src.getRelNotesFrFile();
		getFsManager().resolveFile(webRoot+"/fr/doc/"+file.getName(), webOpts).copyFrom(getFsManager().toFileObject(file), getDummySelector());
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
