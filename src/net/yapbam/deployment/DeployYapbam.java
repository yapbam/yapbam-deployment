package net.yapbam.deployment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Proxy;
import java.net.URL;
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
	private static final String WEB_ROOT = "sftp://web.sourceforge.net/home/project-web/yapbam/htdocs";
	private boolean onlyBeta; 
	private DefaultFileSystemManager fsManager;
	private FileSystemOptions opts;
	private SrcDescription src;
	private FileSelector dummySelector;
	
	DeployYapbam(String user, String password, String srcPath, String newVersion, String oldVersion, boolean onlyBeta) throws FileSystemException {
		this.src = new SrcDescription(new File(srcPath), newVersion, new Date(), oldVersion);
		this.onlyBeta = onlyBeta;
		fsManager = (DefaultFileSystemManager) VFS.getManager();
		opts = new FileSystemOptions();
		SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");
		SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, false); // Use absolute paths
		StaticUserAuthenticator auth = new StaticUserAuthenticator(null, user, password);
		DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
		// SourceForge's SSH server requires keyboard-interactive authentication.
		// JSch needs a UserInfo to answer the keyboard-interactive prompts, otherwise
		// it fails with "Auth cancel" before ever trying the plain password method.
		SftpFileSystemConfigBuilder.getInstance().setUserInfo(opts, new PasswordUserInfo(password));
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

	/**
	 * @param args user, password, srcFolder
	 */
//	public static void main(String[] args) {
//		if (args.length!=5) {
//			System.err.println("Invalid number of arguments");
//			System.out.println("usage: java "+DeployYapbam.class.getName()+" user password srcFolder versionNumber, oldVersionNumber");
//			System.exit(-1);
//		}
//		try {
//			DeployYapbam deploy = new DeployYapbam(args[0], args[1], args[2], args[3], args[4]);
//			deploy.test();
//			deploy.doIt();
//		} catch (FileSystemException e) {
//			System.err.println("An exception occurred");
//			e.printStackTrace();
//			System.exit(-1);
//		}
//	}

	protected void test() {
		//TODO
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
		File f = buildPad(this.src.getPadFile());
		if (trace) System.out.println ("  Uploading french pad ...");
		fsManager.resolveFile(WEB_ROOT+"/pad_file.xml", opts).copyFrom(fsManager.toFileObject(f), getDummySelector());
	}

	private File buildPad(File template) throws FileSystemException {
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
			
			BufferedReader in = new BufferedReader(new FileReader(template));
			try {
				File file = File.createTempFile("fileTemplate", System.currentTimeMillis()+".txt");
				file.deleteOnExit();
				PrintStream out = new PrintStream(file);
				try {
					for (String line = in.readLine() ; line!=null ; line = in.readLine()) {
						line = line.replace("{0}", release);
						line = line.replace("{1}", day);
						line = line.replace("{2}", month);
						line = line.replace("{3}", year);
						line = line.replace("{4}", sbytes);
						line = line.replace("{5}", skbytes);
						line = line.replace("{6}", smbytes);
						out.println(line);
					}
					return file;
				} finally {
					out.close();
				}
			} finally {
				in.close();
			}
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}
	
	private File buildUpdateInfo() throws FileSystemException {
		try {
			String release = this.src.getNewVersion();
			File file = File.createTempFile("updateInfoInclude", System.currentTimeMillis()+".txt");
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
		if (trace) System.out.println ("  Create update folder in https://yapbam.sourceforge.net/ ...");
		String updateFolder = WEB_ROOT+"/update"+this.src.getNewVersion();
		fsManager.resolveFile(updateFolder, opts).createFolder();
		if (trace) System.out.println ("  Copying zip ("+this.src.getZipFile()+") to update folder ...");
		fsManager.resolveFile(updateFolder+"/"+this.src.getZipFile().getName(), opts).copyFrom(fsManager.toFileObject(this.src.getZipFile()), getDummySelector());;
		if (trace) System.out.println ("  update.jar ("+this.src.getUpdaterFile()+") to update folder ...");
		fsManager.resolveFile(updateFolder+"/"+this.src.getUpdaterFile().getName(), opts).copyFrom(fsManager.toFileObject(this.src.getUpdaterFile()), getDummySelector());
		
		if (trace) System.out.println ("  updating auto-update info ...");
		File file = buildUpdateInfo();
		if (!onlyBeta) {
			fsManager.resolveFile(WEB_ROOT+"/updateInfoInclude.txt", opts).copyFrom(fsManager.toFileObject(file), getDummySelector());
		}
		fsManager.resolveFile(WEB_ROOT+"/updateInfoBetaInclude.txt", opts).copyFrom(fsManager.toFileObject(file), getDummySelector());
		
		// Delete old update (if it exists)
		FileObject oldUpdateFolder = fsManager.resolveFile(WEB_ROOT+"/update"+this.src.getOldVersion(), opts);
		if (oldUpdateFolder.exists()) {
			if (trace) System.out.println ("  Delete obsolete update folder in https://yapbam.sourceforge.net/ ...");
			oldUpdateFolder.delete(getDummySelector());
		}
	}

	private void doRelease(boolean trace) throws FileSystemException {
		System.out.println ("Posting release");
		if (trace) System.out.println ("  Copying zip to sourceforge ...");
		fsManager.resolveFile(RELEASE_ROOT+"/yapbam/"+this.src.getZipFile().getName(), opts).copyFrom(fsManager.toFileObject(this.src.getZipFile()), getDummySelector());
		if (trace) System.out.println ("  Copying exe to sourceforge ...");
		fsManager.resolveFile(RELEASE_ROOT+"/yapbam/"+this.src.getExeFile().getName(), opts).copyFrom(fsManager.toFileObject(this.src.getExeFile()), getDummySelector());
		if (trace) System.out.println ("  Copying exe to https://yapbam.sourceforge.net/directDownload ...");
		fsManager.resolveFile(WEB_ROOT+"/directDownload/"+this.src.getExeFile().getName(), opts).copyFrom(fsManager.toFileObject(this.src.getExeFile()), getDummySelector());
	}

	private void doDoc(boolean trace) throws FileSystemException {
		System.out.println ("Copying release notes ...");
		// Relnotes
		File file = src.getRelNotesFile();
		fsManager.resolveFile(WEB_ROOT+"/en/doc/"+file.getName(), opts).copyFrom(fsManager.toFileObject(file), getDummySelector());
		file = src.getRelNotesFrFile();
		fsManager.resolveFile(WEB_ROOT+"/fr/doc/"+file.getName(), opts).copyFrom(fsManager.toFileObject(file), getDummySelector());
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
