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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

import katokorbo.TomcatRunner.StatusObserver;

public class Katokorbo {

	static WebappConfig webappConfig = new WebappConfig();
	static StatusWindow window;
	static TomcatRunner tomcat;

	static String url;
	static File globalConfigDir;

	private static boolean shuttingdown;

	public static void main(String[] args) {
		parseArgs(Arrays.asList(args));
		loadConfig();
		window = new StatusWindow();
		try {
			checkExclusiveLock();
			webappConfig.update(url);
			if (shuttingdown || !webappConfig.getWarFile().exists()) return;
			startTomcat();
		} catch (Throwable ex) {
			window.setFailed("Startup failed.");
			error(ex);
		}
	}

	private static void loadConfig() {
		globalConfigDir = new File(System.getProperty("user.home") + "/.katokorbo");
		Utl.createDirectory(globalConfigDir);
		webappConfig.load(new File(globalConfigDir.getPath() + "/" + Utl.toFileCompatibleString(url)));
	}

	private static void parseArgs(List<String> args) {
		args = new ArrayList<String>(args);

		int titleIdx = args.indexOf("--title");
		if (titleIdx >= 0) {
			webappConfig.setTitle(args.get(titleIdx + 1));
			args.remove(titleIdx);
			args.remove(titleIdx);
		}

		int portIdx = args.indexOf("--port");
		if (portIdx >= 0) {
			webappConfig.setPort(Integer.parseInt(args.get(portIdx + 1)));
			args.remove(portIdx);
			args.remove(portIdx);
		}

		if (args.isEmpty()) {
			error("Missing command line argument <webapp-url>", true);
			return;
		}
		url = args.get(args.size() - 1);
	}

	private static void startTomcat() {
		if (shuttingdown) return;
		tomcat = new TomcatRunner(webappConfig.getPort(), webappConfig.getWarFile(), new StatusObserver() {

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
			error(webappConfig.getTitle() + " is already running.", true);
		}
	}

	public static void error(Throwable ex) {
		error(ex, false);
	}

	public static void error(Throwable ex, boolean shutdown) {
		error(ex.getMessage(), shutdown);
	}

	public static void error(String message) {
		error(message, false);
	}

	public static void error(String message, boolean shutdown) {
		System.err.println("ERROR: " + message);
		if (shutdown && window != null) window.setFailed(message);
		if (shuttingdown) return;
		JOptionPane.showMessageDialog(window, message, "Error", JOptionPane.ERROR_MESSAGE);
		if (shutdown) shutdown(1);
	}

	public static void shutdown(int retValue) {
		shuttingdown = true;
		if (window != null) window.setStatus("Shutting down...");
		if (tomcat != null) tomcat.stopTomcat();
		System.exit(retValue);
	}

}
