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

public class WarDownloader {

	private String url;
	private File warFile;
	private Properties availableProperties;
	private File installedPropertiesFile;

	public WarDownloader(String url) {
		super();
		this.url = url;

		installedPropertiesFile = new File(Katokorbo.appConfigDir.getPath() + "/version.properties");
		warFile = new File(Katokorbo.appConfigDir.getPath() + "/webapp.war");
	}

	public File downloadIfNew() {
		if (url.endsWith(".war")) return downloadWar(url);

		try {
			availableProperties = Utl.downloadProperties(url);
		} catch (Throwable ex) {
			Utl.log("Downloading release properties from <" + url + "> failed:", ex);
			if (warFile.exists()) return warFile;
		}
		String availableVersion = getVersion(availableProperties);
		Utl.log("Available version:", availableVersion);

		Properties installedProperties = installedPropertiesFile.exists() ? Utl.loadProperties(installedPropertiesFile)
				: new Properties();
		String installedVersion = getVersion(installedProperties);
		Utl.log("Installed version:", installedVersion);

		if (isVersionNewer(availableVersion, installedVersion)) {
			if (!installedVersion.equals("0")) {
				if (!askForDownload(availableVersion, installedVersion)) return warFile;
			}

			File file = downloadWar(getWarUrl(availableProperties));
			installedProperties.setProperty("version", availableVersion);
			Utl.saveProperties(installedProperties, installedPropertiesFile);
			return file;
		}

		Utl.log("Available version is not newer. Using installed.");

		return warFile;
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

	private String getWarUrl(Properties p) {
		return p.getProperty("warUrl");
	}

	private File downloadWar(String url) {
		Utl.log("Downloading .war:", url);
		Katokorbo.window.setStatus("Downloading web application <" + url + ">...");
		try {
			Utl.downloadUrlToFile(url, warFile, null);
		} catch (Throwable ex) {
			Utl.log("Downloading war file <" + url + "> failed:", ex);
			if (warFile.exists()) return warFile;
			throw new RuntimeException(ex);
		}

		Utl.log(".war downloaded:", warFile.getAbsolutePath());
		return warFile;
	}
}
