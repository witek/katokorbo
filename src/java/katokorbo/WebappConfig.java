/*
 * Copyright 2011 Witoslaw Koczewsi <wi@koczewski.de>
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero
 * General Public License as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package katokorbo;

import java.io.File;
import java.util.Properties;

import javax.swing.JOptionPane;

public class WebappConfig {

	private File configDir;

	private String title = "Katokorbo";
	private int port = 8085;
	private File windowIconFile;

	private File propertiesFile;
	private Properties properties;
	private File warFile;
	private File eulaFile;

	public void load(File configDir) {
		this.configDir = configDir;

		Utl.createDirectory(configDir);
		windowIconFile = new File(configDir.getPath() + "/windowIcon.png");
		propertiesFile = new File(configDir.getPath() + "/version.properties");
		warFile = new File(configDir.getPath() + "/webapp.war");
		eulaFile = new File(configDir.getPath() + "/acceptedEula.txt");

		properties = propertiesFile.exists() ? Utl.loadProperties(propertiesFile) : new Properties();
		updateLocal();
	}

	public void update(String url) {
		Katokorbo.window.setStatus("Checking for new version...");
		WebappDownloader downloader = WebappDownloader.load(url);
		if (downloader == null) {
			if (warFile.exists()) return;
			Katokorbo.error("Downloading from <" + url + "> failed.");
			return;
		}

		String availableVersion = getVersion(downloader.getProperties());
		Utl.log("Available version:", availableVersion);

		String installedVersion = getVersion(properties);
		Utl.log("Installed version:", installedVersion);

		if (isVersionNewer(availableVersion, installedVersion)) {

			if (!installedVersion.equals("0")) {
				if (!askForDownload(availableVersion, installedVersion)) return;
			}

			if (!checkEula(downloader.downloadEula())) {
				Katokorbo.shutdown(0);
				return;
			}

			try {
				downloader.downloadWar(warFile);
			} catch (Throwable ex) {
				Utl.log("Downloading war file <" + url + "> failed:", ex);
				if (warFile.exists()) return;
				throw new RuntimeException(ex);
			}

			properties = downloader.getProperties();
			Utl.saveProperties(properties, propertiesFile);

			downloader.downloadWindowIcon(windowIconFile);

			updateLocal();
		}

		Utl.log("Available version is not newer. Using installed.");
	}

	public boolean checkEula(String eula) {
		if (eula == null) return true;
		if (eulaFile.exists()) {
			String acceptedEula = Utl.readFile(eulaFile);
			if (eula.equals(acceptedEula)) return true;
		}
		if (!EulaDialog.ask(eula)) return false;
		Utl.writeFile(eulaFile, eula);
		return true;
	}

	private void updateLocal() {
		if (properties.containsKey("title")) title = properties.getProperty("title");
		if (properties.containsKey("port")) port = Integer.parseInt(properties.getProperty("port"));
		if (Katokorbo.window != null) Katokorbo.window.update();
	}

	private boolean askForDownload(String availableVersion, String installedVersion) {
		String title = "New version available";
		String msg = "Your current version is " + installedVersion + ". The newer version " + availableVersion
				+ " is available.\n\n Do you want to download the new version?";
		return JOptionPane.showConfirmDialog(Katokorbo.window, msg, title, JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
	}

	private boolean isVersionNewer(String a, String b) {
		if (a == null || b == null) return false;
		return new Version(a).compareTo(new Version(b)) > 0;
	}

	private String getVersion(Properties p) {
		String version = p.getProperty("version");
		if (version == null) return "0";
		return version;
	}

	public File getWindowIconFile() {
		return windowIconFile;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String appTitle) {
		this.title = appTitle;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getPort() {
		return port;
	}

	public File getWarFile() {
		return warFile;
	}

}
