package net.yapbam.deployment;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SrcDescription {
	private File updaterFile;
	private File zipFile;
	private File exeFile;
	private File relNotesFile;
	private File relNotesFrFile;
	private String newVersion;
	private String oldVersion;
	private Date releaseDate;

	public SrcDescription(File folder, String newVersion, Date releaseDate, String oldVersion) {
		this.updaterFile = new File (folder, "updater.jar");
		this.zipFile = new File (folder, "yapbam-"+newVersion+".zip");
		this.exeFile = new File (folder, "yapbam-"+newVersion+".exe");
		this.relNotesFile = new File (folder, "src/localization/relnotes.txt");
		this.relNotesFrFile = new File (folder, "src/localization/fr/relnotes.txt");
		this.newVersion = newVersion;
		this.oldVersion = oldVersion;
		this.releaseDate = releaseDate;
	}

	/**
	 * @return the zipFile
	 */
	public File getZipFile() {
		return zipFile;
	}

	/**
	 * @return the exeFile
	 */
	public File getExeFile() {
		return exeFile;
	}

	/**
	 * @return the relNotesFile
	 */
	public File getRelNotesFile() {
		return relNotesFile;
	}

	/**
	 * @return the relNotesFrFile
	 */
	public File getRelNotesFrFile() {
		return relNotesFrFile;
	}

	public String getNewVersion() {
		return this.newVersion;
	}

	/**
	 * @return the updaterFile
	 */
	public File getUpdaterFile() {
		return updaterFile;
	}

	/**
	 * @return the oldVersion
	 */
	public String getOldVersion() {
		return oldVersion;
	}

	/**
	 * @return the releaseDate
	 */
	public Date getReleaseDate() {
		return releaseDate;
	}

	/** Gets all the files referenced by this description, except the updater file.
	 * @return An unmodifiable list of files (zip, exe, release notes).
	 */
	public List<File> getFiles() {
		return Collections.unmodifiableList(Arrays.asList(zipFile, exeFile, relNotesFile, relNotesFrFile));
	}
}
