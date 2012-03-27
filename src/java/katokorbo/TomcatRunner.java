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
import java.net.InetAddress;
import java.net.URL;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Embedded;

public class TomcatRunner {

	private Embedded container = null;
	private File warfile;
	private File webappdir;
	private File workdir;
	private File webappsdir;
	private int port;
	private StatusObserver observer;

	public TomcatRunner(int port, File webappdir, StatusObserver observer) {
		this(port, new File("tomcat-work"), webappdir, observer);
	}

	public TomcatRunner(int port, File workdir, File warfile, StatusObserver observer) {
		if (workdir == null) throw new NullPointerException("workdir");
		if (warfile == null) throw new NullPointerException("warfile");
		this.observer = observer;
		this.port = port;
		this.warfile = warfile;
		this.workdir = workdir;

		Utl.createDirectory(workdir);

		webappsdir = new File(workdir.getPath() + "/webapps");
		Utl.createDirectory(webappsdir);
		webappdir = new File(webappsdir.getPath() + "/ROOT");

		prepareWorkdir();
		extractWebapp();

		try {
			doStartTomcat();
		} catch (Throwable ex) {
			ex.printStackTrace();
			observer.onStartFailed(ex.getMessage());
		}
	}

	private void prepareWorkdir() {
		File confdir = new File(workdir.getPath() + "/conf");
		Utl.createDirectory(confdir);
		URL webxml = Utl.getResource("katokorbo/default-web.xml");
		File webxmlfile = new File(confdir.getPath() + "/web.xml");
		Utl.downloadUrlToFile(webxml.toString(), webxmlfile, null);
	}

	private void extractWebapp() {
		Utl.log("Extracting web application <" + warfile.getAbsolutePath() + "> to <" + webappdir + ">");
		try {
			if (webappdir.exists()) Utl.delete(webappdir);
			Utl.unzip(warfile, webappdir);
		} catch (Exception ex) {
			throw new RuntimeException("Extracting webapp file <" + warfile.getAbsolutePath() + "> to <"
					+ webappdir.getAbsolutePath() + "> failed.", ex);
		}
	}

	private void doStartTomcat() throws LifecycleException, InterruptedException {
		Utl.log("Starting Tomcat with webapp context <" + webappdir.getAbsolutePath() + ">");
		int threadsBefore = Thread.activeCount();

		container = new Embedded();
		container.setCatalinaHome(workdir.getPath());

		Context context = container.createContext("/", webappdir.getAbsolutePath());
		context.setLoader(new WebappLoader(getClass().getClassLoader()));

		// create host
		Host localHost = container.createHost("localHost", webappsdir.getAbsolutePath());
		localHost.addChild(context);

		// create engine
		Engine engine = container.createEngine();
		engine.setName("localEngine");
		engine.addChild(localHost);
		engine.setDefaultHost(localHost.getName());
		container.addEngine(engine);

		// create http connector
		Connector httpConnector = container.createConnector((InetAddress) null, port, false);
		container.addConnector(httpConnector);

		container.setAwait(true);

		container.addLifecycleListener(new LifecycleListener() {

			@Override
			public void lifecycleEvent(LifecycleEvent event) {
				String type = event.getType();
				if (type.equals("start")) {
					observer.onStarting();
					return;
				}
				if (type.equals("stop")) {
					observer.onStopped();
					return;
				}
				System.out.println("Unknown event -> " + event.getType());
			}
		});

		// start server
		container.start();

		// add shutdown hook to stop server
		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				stopTomcat();
			}
		});

		int newThreads = Thread.activeCount() - threadsBefore;
		if (newThreads > 2) {
			observer.onStarted(port);
		} else {
			observer.onStartFailed("Starting tomcat failed.");
		}
	}

	public void stopTomcat() {
		try {
			if (container != null) container.stop();
		} catch (Throwable ex) {}
	}

	public int getPort() {
		return port;
	}

	public static interface StatusObserver {

		void onStarting();

		void onStarted(int port);

		void onStartFailed(String message);

		void onStopped();

	}

}
