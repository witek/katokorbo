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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class Utl {

	public static void log(Object... message) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Object s : message) {
			if (first) {
				first = false;
			} else {
				sb.append(" ");
			}
			sb.append(s);
		}
		System.out.println(sb.toString());
	}

	public static String toFileCompatibleString(String s) {
		if (s == null) return null;
		s = s.replace('/', '-');
		s = s.replace('\\', '-');
		s = s.replace(':', '_');
		s = s.replace(';', '_');
		s = s.replace('&', '@');
		s = s.replace('?', '@');
		s = s.replace('=', '_');
		return s;
	}

	public static URLConnection openUrlConnection(String url) {
		URLConnection connection;
		try {
			connection = new URL(url).openConnection();
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
		if (connection instanceof HttpsURLConnection) {
			HttpsURLConnection sconnection = (HttpsURLConnection) connection;
			sconnection.setHostnameVerifier(new HostnameVerifier() {

				@Override
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}

			});
		}
		return connection;
	}

	public static InputStream openUrlInputStream(String url) {
		try {
			return openUrlConnection(url).getInputStream();
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
	}

	public static URL getResource(String resourceName) {
		return Utl.class.getClassLoader().getResource(resourceName);
	}

	public static void downloadUrlToFile(String url, File file, CopyObserver observer) {
		InputStream in;
		try {
			URLConnection connection = new URL(url).openConnection();
			connection.connect();
			int length = connection.getContentLength();
			if (observer != null && length > 0) observer.totalSizeDetermined(length);
			in = connection.getInputStream();
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}

		try {
			copyDataToFile(in, file, observer);
		} finally {
			close(in);
		}
	}

	public static void copyDataToFile(InputStream is, File dst, CopyObserver observer) {
		createDirectory(dst.getParentFile());
		File tmp = new File(dst.getPath() + "~" + System.currentTimeMillis());

		BufferedInputStream in;
		try {
			if (is instanceof BufferedInputStream) {
				in = (BufferedInputStream) is;
			} else {
				in = new BufferedInputStream(is);
			}
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmp, false));
			copyData(in, out, observer);
			in.close();
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			tmp.delete();
			throw new RuntimeException(ex);
		}
		if (dst.exists()) if (!dst.delete()) {
			delete(tmp);
			throw new RuntimeException("Overwriting file '" + dst + "' failed.");
		}
		if (!tmp.renameTo(dst)) {
			delete(tmp);
			throw new RuntimeException("Moving '" + tmp + "' to '" + dst + "' failed.");
		}
	}

	public static void copyData(InputStream in, OutputStream out, CopyObserver observer) {
		byte[] block = new byte[1000];
		try {
			while (true) {
				if (observer != null && observer.isAbortRequested()) return;
				int amountRead;
				amountRead = in.read(block);
				if (amountRead == -1) {
					break;
				}
				out.write(block, 0, amountRead);
				if (observer != null) observer.dataCopied(amountRead);
			}

			out.flush();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void createDirectory(File dir) {
		if (dir.exists()) {
			if (dir.isDirectory()) return;
			throw new RuntimeException("A file already exists: " + dir.getPath());
		}
		if (!dir.mkdirs()) throw new RuntimeException("Failed to create directory: " + dir.getPath());
	}

	public static void close(InputStream in) {
		if (in == null) return;
		try {
			in.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void unzip(File zipFile, File destinationDir) throws ZipException, IOException {
		int BUFFER = 2048;

		ZipFile zip = new ZipFile(zipFile);

		String destinationPath = destinationDir.getPath();
		createDirectory(destinationDir);
		Enumeration zipFileEntries = zip.entries();

		while (zipFileEntries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();

			String currentEntry = entry.getName();

			File destFile = new File(destinationPath + "/" + currentEntry);
			File destinationParent = destFile.getParentFile();

			destinationParent.mkdirs();
			if (!entry.isDirectory()) {
				BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
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
		}
	}

	public static void delete(File f) {
		if (!f.exists()) return;
		if (f.isDirectory()) {
			File[] fa = f.listFiles();
			for (int i = 0; i < fa.length; i++) {
				delete(fa[i]);
			}
		}
		if (!f.delete()) throw new RuntimeException("Deleting file failed: " + f.getPath());
	}

	public interface CopyObserver {

		boolean isAbortRequested();

		void totalSizeDetermined(long bytes);

		void dataCopied(long bytes);

	}

	public static void startBrowser(String url) {
		String command = isWindows() ? "explorer" : "firefox";
		try {
			Runtime.getRuntime().exec(new String[] { command, url });
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static boolean isWindows() {
		return !isUnixFileSystem();
	}

	private static Boolean unixFileSystem;

	public static boolean isUnixFileSystem() {
		if (unixFileSystem == null) {
			File[] roots = File.listRoots();
			unixFileSystem = roots.length == 1 && "/".equals(roots[0].getPath());
		}
		return unixFileSystem;
	}

	public static Properties downloadProperties(String url) {
		Properties p = new Properties();
		InputStream is = openUrlInputStream(url);
		try {
			p.load(is);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} finally {
			close(is);
		}
		return p;
	}

	public static Properties loadProperties(File file) {
		Properties p = new Properties();
		InputStream is = null;
		try {
			is = new BufferedInputStream(new FileInputStream(file));
			p.load(is);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} finally {
			close(is);
		}
		return p;
	}

	public static void saveProperties(Properties p, File file) {
		createDirectory(file.getParentFile());
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(file));
			p.store(out, null);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} finally {
			close(out);
		}
	}

	public static void close(OutputStream out) {
		if (out == null) return;
		try {
			out.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
