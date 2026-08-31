package net.yapbam.deployment;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import net.yapbam.util.SecureDownloader;

/** Prepares the deployment source folder.
 * <br>It copies all the files needed by the deployment (zip, exe, release notes, pad file, etc.)
 * into a temporary folder, and resolves the updater jar:
 * <ul>
 *   <li>If a local updater jar is provided, it is copied as updater.jar.</li>
 *   <li>Otherwise, the updater is downloaded from Maven Central using the version
 *       found in the updater.version file located in the source folder.</li>
 * </ul>
 * The resulting temporary folder can then be passed to {@link SrcDescription}.
 */
public class DeploymentPreparation {
	private static final String MAVEN_CENTRAL_BASE = "https://repo1.maven.org/maven2/com/fathzer/yapbam-updater/";
	private static final String UPDATER_VERSION_FILE = "updater.version";

	private final File srcFolder;
	private final String newVersion;
	private final File forcedUpdater;

	/** Creates a preparation context.
	 * @param srcFolder The folder containing the yapbam build artifacts (zip, exe, updater.version, etc.).
	 * @param newVersion The version to deploy.
	 * @param forcedUpdater A local updater jar to use instead of downloading from Maven Central, or null.
	 */
	public DeploymentPreparation(File srcFolder, String newVersion, File forcedUpdater) {
		this.srcFolder = srcFolder;
		this.newVersion = newVersion;
		this.forcedUpdater = forcedUpdater;
	}

	/** Prepares a temporary folder with all the files needed for deployment.
	 * @return The temporary folder containing all the deployment artifacts.
	 * @throws IOException If a file cannot be copied or the updater cannot be resolved.
	 */
	public File prepare() throws IOException {
		File tempFolder = Files.createTempDirectory("yapbam-deploy").toFile();
		tempFolder.deleteOnExit();
		// Build a SrcDescription for the source folder to know which files to copy
		SrcDescription srcDesc = new SrcDescription(srcFolder, newVersion, null, null);
		for (File srcFile : srcDesc.getFiles()) {
			copyToTemp(srcFile, srcFolder, tempFolder);
		}
		// Build a SrcDescription for the temp folder to know where the updater should go
		SrcDescription tempDesc = new SrcDescription(tempFolder, newVersion, null, null);
		resolveUpdater(tempDesc.getUpdaterFile());
		return tempFolder;
	}

	/** Copies a file from the source folder to the temp folder, preserving its relative path.
	 * @param srcFile The source file (as defined by SrcDescription).
	 * @param srcFolder The source folder used to compute the relative path.
	 * @param tempFolder The destination folder.
	 * @throws IOException If the file cannot be copied.
	 */
	private static void copyToTemp(File srcFile, File srcFolder, File tempFolder) throws IOException {
		String relativePath = srcFolder.toPath().relativize(srcFile.getParentFile().toPath()).toString();
		File destParent = relativePath.isEmpty() ? tempFolder : new File(tempFolder, relativePath);
		destParent.mkdirs();
		File dest = new File(destParent, srcFile.getName());
		Files.copy(srcFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	private void resolveUpdater(File updaterDest) throws IOException {
		if (forcedUpdater != null && forcedUpdater.exists()) {
			Files.copy(forcedUpdater.toPath(), updaterDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} else {
			downloadUpdaterFromMavenCentral(updaterDest);
		}
	}

	private void downloadUpdaterFromMavenCentral(File updaterDest) throws IOException {
		String version = readUpdaterVersion();
		String url = MAVEN_CENTRAL_BASE + version + "/yapbam-updater-" + version + ".jar";
		SecureDownloader sd = new SecureDownloader(Proxy.NO_PROXY);
		SecureDownloader.DownloadInfo info = sd.download(toURL(url), updaterDest);
		if (info == null) {
			throw new IOException("Updater download was cancelled");
		}
	}

	private String readUpdaterVersion() throws IOException {
		File versionFile = new File(srcFolder, UPDATER_VERSION_FILE);
		String content = new String(Files.readAllBytes(versionFile.toPath()), StandardCharsets.UTF_8).trim();
		if (content.isEmpty()) {
			throw new IOException("Updater version file is empty: " + versionFile);
		}
		return content;
	}

	private static URL toURL(String url) throws MalformedURLException {
		try {
			return new URI(url).toURL();
		} catch (URISyntaxException e) {
			throw new MalformedURLException(e.getMessage());
		}
	}
}
