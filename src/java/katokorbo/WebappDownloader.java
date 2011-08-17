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

public class WebappDownloader {

	private Properties properties;

	private WebappDownloader(Properties properties) {
		this.properties = properties;
	}

	public static WebappDownloader load(String url) {
		Utl.log("Downloading webapp properties: " + url + "");
		try {
			return new WebappDownloader(Utl.downloadProperties(url));
		} catch (Throwable ex) {
			Utl.log("Downloading version properties from <" + url + "> failed:", ex);
			return null;
		}
	}

	public void downloadWar(File warFile) {
		String url = properties.getProperty("warUrl");
		Katokorbo.window.setStatus("Downloading web application <" + url + ">...");
		Utl.downloadUrlToFile(url, warFile, null);
		Utl.log(".war downloaded:", warFile.getAbsolutePath());
	}

	public void downloadWindowIcon(File file) {
		downloadImage(properties.getProperty("windowIconUrl"), file);
	}

	private void downloadImage(String url, File file) {
		if (url == null) return;
		Katokorbo.window.setStatus("Downloading image <" + url + ">...");
		Utl.downloadUrlToFile(url, file, null);
	}

	public String downloadEula() {
		String url = properties.getProperty("eulaUrl");
		if (url == null) return null;
		return Utl.downloadUrlToString(url);
	}

	public Properties getProperties() {
		return properties;
	}

}
