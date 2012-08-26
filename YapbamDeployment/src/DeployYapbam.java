import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Proxy;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import net.yapbam.util.SecureDownloader;

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

/** Deploys the javaluator updates on its web site.
 * <br>This class requires that the Javaluator build has been already done with success.
 * <br>It verifies that all material is ready to be deployed. 
 * @author Jean-Marc-Marc Astesana
 */
public class DeployYapbam {
	private static final String RELEASE_ROOT = "sftp://web.sourceforge.net/home/pfs/project/yapbam";
	private static final String WEB_ROOT = "sftp://web.sourceforge.net/home/project-web/yapbam/htdocs";
	private DefaultFileSystemManager fsManager;
	private FileSystemOptions opts;
	private SrcDescription src;
	private FileSelector dummySelector;
	
	private DeployYapbam(String user, String password, String srcPath, String newVersion, String oldVersion) throws FileSystemException {
		this.src = new SrcDescription(new File(srcPath), newVersion, new Date(), oldVersion);
		fsManager = (DefaultFileSystemManager) VFS.getManager();
		opts = new FileSystemOptions();
		SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");
		SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, false); // Use absolute paths
		StaticUserAuthenticator auth = new StaticUserAuthenticator(null, user, password);
		DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		System.out.println ("Closing source forge connection");
		fsManager.close();
	}

	/**
	 * @param args user, password, srcFolder
	 */
	public static void main(String[] args) {
		if (args.length!=5) {
			System.err.println("Invalid number of arguments");
			System.out.println("usage: java "+DeployYapbam.class.getName()+" user password srcFolder versionNumber, oldVersionNumber");
			System.exit(-1);
		}
		try {
			DeployYapbam deploy = new DeployYapbam(args[0], args[1], args[2], args[3], args[4]);
			deploy.test();
			deploy.doIt();
		} catch (FileSystemException e) {
			System.err.println("An exception occurred");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	protected void test() {
		//TODO
	}

	protected void doIt() throws FileSystemException {
		boolean trace = true;
		doAutoUpdate(trace);
		doRelease(trace);
		doDoc(trace);
		doPad(trace);
		System.out.println ("Finished :-)");
	}
	
	private void doPad(boolean trace) throws FileSystemException {
		System.out.println ("Updating pad files");
		File f = buildPad(this.src.getFrenchPad());
		if (trace) System.out.println ("  Uploading french pad ...");
		fsManager.resolveFile(WEB_ROOT+"/pad_file.xml", opts).copyFrom(fsManager.toFileObject(f), getDummySelector());
		f = buildPad(this.src.getEnglishPad());
		if (trace) System.out.println ("  Uploading english pad ...");
		fsManager.resolveFile(WEB_ROOT+"/pad_file_en.xml", opts).copyFrom(fsManager.toFileObject(f), getDummySelector());
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
			DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
			String date = format.format(new Date());
			File file = File.createTempFile("updateInfoInclude", System.currentTimeMillis()+".txt");
			file.deleteOnExit();
			PrintStream out = new PrintStream(file);
			try {
				out.println ("lastestRelease="+release+" ("+date+")");
				out.println ("updateURL=http://sourceforge.net/project/platformdownload.php?group_id=276272");
				out.println ();
				SecureDownloader sd = new SecureDownloader(Proxy.NO_PROXY);
				String zipURL = "http://www.yapbam.net/update"+release+"/yapbam-"+release+".zip";
				out.println ("autoUpdateURL="+zipURL);
				sd.download(new URL(zipURL), null);
				out.println ("autoUpdateCHKSUM="+sd.getCheckSum());
				out.println ("autoUpdateSize="+sd.getDownloadedSize());
				out.println ();
		
				String updaterURL = "http://www.yapbam.net/update"+release+"/updater.jar";
				out.println ("autoUpdateUpdaterURL="+updaterURL);
				sd.download(new URL(updaterURL), null);
				out.println ("autoUpdateUpdaterCHKSUM="+sd.getCheckSum());
				out.println ("autoUpdateUpdaterSize="+sd.getDownloadedSize());
				return file;
			} finally {
				out.close();
			}
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	private void doAutoUpdate(boolean trace) throws FileSystemException {
		System.out.println ("Setting up auto update");
		if (trace) System.out.println ("  Create update folder in http://www.yapbam.net/ ...");
		String updateFolder = WEB_ROOT+"/update"+this.src.getNewVersion();
		fsManager.resolveFile(updateFolder, opts).createFolder();
		if (trace) System.out.println ("  Copying zip to update folder ...");
		fsManager.resolveFile(updateFolder+"/"+this.src.getZipFile().getName(), opts).copyFrom(fsManager.toFileObject(this.src.getZipFile()), getDummySelector());;
		if (trace) System.out.println ("  update.jar to update folder ...");
		fsManager.resolveFile(updateFolder+"/"+this.src.getUpdaterFile().getName(), opts).copyFrom(fsManager.toFileObject(this.src.getUpdaterFile()), getDummySelector());
		
		if (trace) System.out.println ("  updating auto-update info ...");
		File file = buildUpdateInfo();
		fsManager.resolveFile(WEB_ROOT+"/updateInfoInclude.txt", opts).copyFrom(fsManager.toFileObject(file), getDummySelector());
		
		// Delete old update (if it exists)
		FileObject oldUpdateFolder = fsManager.resolveFile(WEB_ROOT+"/update"+this.src.getOldVersion(), opts);
		if (oldUpdateFolder.exists()) {
			if (trace) System.out.println ("  Delete obsolete update folder in http://www.yapbam.net/ ...");
			oldUpdateFolder.delete(getDummySelector());
		}
	}

	private void doRelease(boolean trace) throws FileSystemException {
		System.out.println ("Posting release");
		if (trace) System.out.println ("  Copying zip to sourceforge ...");
		fsManager.resolveFile(RELEASE_ROOT+"/yapbam/"+this.src.getZipFile().getName(), opts).copyFrom(fsManager.toFileObject(this.src.getZipFile()), getDummySelector());
		if (trace) System.out.println ("  Copying exe to sourceforge ...");
		fsManager.resolveFile(RELEASE_ROOT+"/yapbam/"+this.src.getExeFile().getName(), opts).copyFrom(fsManager.toFileObject(this.src.getExeFile()), getDummySelector());
		if (trace) System.out.println ("  Copying exe to http://www.yapbam.net/directDownload ...");
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
