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
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

import katokorbo.TomcatRunner.StatusObserver;

public class Katokorbo {

	public static String appTitle;
	public static String warUrl;
	public static File warFile;
	public static int port;

	static File globalConfigDir;
	static File appConfigDir;
	static StatusWindow window;
	static TomcatRunner tomcat;

	private static boolean shuttingdown;

	public static void main(String[] args) throws Exception {
		parseArgs(Arrays.asList(args));
		readConfig();
		window = new StatusWindow();
		checkExclusiveLock();
		downloadWar();
		startTomcat();
	}

	private static void readConfig() {
		globalConfigDir = new File(System.getProperty("user.home") + "/.katokorbo");
		Utl.createDirectory(globalConfigDir);

		String app = warUrl == null ? warFile.getPath() : warUrl;
		appConfigDir = new File(globalConfigDir.getPath() + "/" + Utl.toFileCompatibleString(app));
		Utl.createDirectory(appConfigDir);
	}

	private static void downloadWar() {
		if (warUrl == null) return;
		WarDownloader downloader = new WarDownloader(warUrl);
		warFile = downloader.downloadIfNew();
	}

	private static void parseArgs(List<String> args) {
		if (args.isEmpty()) {
			Utl.log("DEVELOPER MODE");
			port = 9060;
			appTitle = "Katokorbo";
			warFile = new File("/home/witek/inbox/kunagi.war");
			return;
		}
		String war = args.get(args.size() - 1);
		if (war.contains("://")) {
			warUrl = war;
		} else {
			warFile = new File(war);
		}

		int titleIdx = args.indexOf("--title");
		if (titleIdx >= 0) appTitle = args.get(titleIdx + 1);

		int portIdx = args.indexOf("--port");
		if (portIdx >= 0) port = Integer.parseInt(args.get(portIdx + 1));
	}

	private static void startTomcat() {
		if (shuttingdown) return;
		tomcat = new TomcatRunner(port, warFile, new StatusObserver() {

			@Override
			public void onStartFailed(String message) {
				error(message);
			}

			@Override
			public void onStopped() {
				if (!shuttingdown) error("Tomcat stopped.");
			}

			@Override
			public void onStarting() {
				window.setStatus("Starting...");
			}

			@Override
			public void onStarted(int port) {
				window.setReady(port);
			}

		});
	}

	private static void checkExclusiveLock() {
		File file = new File(globalConfigDir.getPath() + "/singleton.lock");
		try {
			new ExclusiveFileLock(file.getAbsoluteFile());
		} catch (ExclusiveFileLock.FileLockedException ex) {
			error(appTitle + " is already running.");
		}
	}

	public static void error(Throwable ex) {
		error(ex.getMessage());
	}

	public static void error(String message) {
		System.err.println("ERROR: " + message);
		if (shuttingdown) return;
		JOptionPane.showMessageDialog(window, message, "Error", JOptionPane.ERROR_MESSAGE);
		shutdown(1);
	}

	public static void shutdown(int retValue) {
		shuttingdown = true;
		if (window != null) window.setStatus("Shutting down...");
		if (tomcat != null) tomcat.stopTomcat();
		System.exit(retValue);
	}

}
