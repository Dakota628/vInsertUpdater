//Created by Dakota628

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteGlassLookAndFeel;


public class vInsertUpdater {
	public static String GITHUB_USER = "vInsertOfficial";
	public static String GITHUB_REPO = "vInsert";

	public static String GITHUB_COMMITS_URL = "https://api.github.com/repos/" + GITHUB_USER +"/" + GITHUB_REPO + "/commits";
	public static String GITHUB_DOWNLOAD_URL = "https://github.com/" + GITHUB_USER +"/" + GITHUB_REPO + "/archive/master.zip";

	public static final File STORAGE_DIR = new File(System.getProperty("user.home") + File.separator + "vInsert" + File.separator + "updater");
	public static final File REPO_ZIP = new File(STORAGE_DIR,"repo.zip");
	public static final File REPO_FOLDER = new File(STORAGE_DIR,"repo" + File.separator + GITHUB_REPO + "-master");
	public static final File REPO_JAR = new File(STORAGE_DIR,"repo" + File.separator + GITHUB_REPO + "-master" + File.separator + "build" + File.separator + "vinsert.jar");
	public static final File JAR = new File(STORAGE_DIR,"vinsert.jar");
	public static final File LAST_COMMIT_FILE = new File(STORAGE_DIR + File.separator + "lastcommit.txt");
	
	public static JProgressBar pb = new JProgressBar();
	public static JFrame frame;

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		UIManager.setLookAndFeel(new SubstanceGraphiteGlassLookAndFeel());
		JFrame.setDefaultLookAndFeelDecorated(true);
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				frame = new JFrame("vInsert Updater");
				pb.updateUI();
				pb.setPreferredSize(new Dimension(200, 80));
				pb.setStringPainted(true);
				pb.setIndeterminate(true);
				pb.setBounds(0, 0, 240, 80);

				frame.getContentPane().add(pb, BorderLayout.CENTER);

				frame.pack();
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setSize(250, 100);
				frame.setVisible(true);
				frame.setResizable(false);
			}});

		if(!STORAGE_DIR.exists()) STORAGE_DIR.mkdirs();

		setStatus("Checking for updates...");

		if(!isUpToDate()) {
			setStatus("Not up to date...");
			update();
			launch(args);
		} else {
			setStatus("Up to date...");
			launch(args);
		}
	}

	private static void launch(String[] args) throws Exception {
		setStatus("Launching...");
		Runtime.getRuntime().exec((String[]) ArrayUtils.addAll(new String[]{"java","-jar",JAR.getAbsolutePath()},args));
		exitFrame();
		System.exit(0);
	}

	private static void update() throws Exception {
		setStatus("Downloading repo...");
		nioDownload(new URL(GITHUB_DOWNLOAD_URL),REPO_ZIP);
		setStatus("Unpacking repo...");
		extractFolder(REPO_ZIP.getAbsolutePath());

		setStatus("Building repo...");
		File buildFile = new File(REPO_FOLDER,"build.xml");
		Project p = new Project();
		p.setUserProperty("ant.file", buildFile.getAbsolutePath());
		p.init();
		ProjectHelper helper = ProjectHelper.getProjectHelper();
		p.addReference("ant.projectHelper", helper);
		helper.parse(p, buildFile);
		p.executeTarget("jar");
		
		if(!LAST_COMMIT_FILE.exists()) LAST_COMMIT_FILE.createNewFile();
		FileUtils.write(LAST_COMMIT_FILE, getLatestCommit());
		
		JAR.delete();
		REPO_JAR.renameTo(JAR);
		FileUtils.deleteDirectory(REPO_FOLDER);
		REPO_ZIP.delete();
	}

	private static boolean isUpToDate() throws Exception {
		if(!LAST_COMMIT_FILE.exists() || !JAR.exists()) return false;
		String last_sha = IOUtils.readLines(new FileInputStream(LAST_COMMIT_FILE)).get(0).trim();
		System.out.println(last_sha);
		System.out.println(getLatestCommit());
		return getLatestCommit().trim().equals(last_sha);
	}

	private static String getLatestCommit() throws Exception {
		URL commitsURL = new URL(GITHUB_COMMITS_URL);
		String commitsStr = IOUtils.toString(commitsURL.openStream());
		JSONArray commitsJSON = (JSONArray) JSONSerializer.toJSON(commitsStr);
		return ((JSONObject) commitsJSON.get(0)).getString("sha");
	}

	private static void nioDownload(URL url, File save) throws Exception {
		ReadableByteChannel rbc = Channels.newChannel(url.openStream());
		FileOutputStream fos = new FileOutputStream(save);
		fos.getChannel().transferFrom(rbc, 0, 1 << 24);
		fos.close();
	}

	//http://stackoverflow.com/questions/981578/how-to-unzip-files-recursively-in-java
	static public void extractFolder(String zipFile) throws ZipException, IOException 
	{
		int BUFFER = 2048;
		File file = new File(zipFile);
		ZipFile zip = new ZipFile(file);
		String newPath = zipFile.substring(0, zipFile.length() - 4);
		new File(newPath).mkdir();
		Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();
		while (zipFileEntries.hasMoreElements())
		{
			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
			String currentEntry = entry.getName();
			File destFile = new File(newPath, currentEntry);
			File destinationParent = destFile.getParentFile();
			destinationParent.mkdirs();
			if (!entry.isDirectory())
			{
				BufferedInputStream is = new BufferedInputStream(zip
						.getInputStream(entry));
				int currentByte;
				byte data[] = new byte[BUFFER];
				FileOutputStream fos = new FileOutputStream(destFile);
				BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
				while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
					dest.write(data, 0, currentByte);
				}
				dest.flush();
				dest.close();
				is.close();
			}

			if (currentEntry.endsWith(".zip"))
			{
				extractFolder(destFile.getAbsolutePath());
			}
		}
		zip.close();
	}

	public static void setStatus(final String s) {
		System.out.println(s);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				pb.setString(s);
				pb.updateUI();
			}});
	}
	
	public static void exitFrame() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				frame.setVisible(false);
				frame.dispose();
			}});
	}
}
