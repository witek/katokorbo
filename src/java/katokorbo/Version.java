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

import java.util.StringTokenizer;

/**
 * Data type for a version like 1.0.12
 */
public final class Version implements Comparable<Version> {

	private int major;
	private int minor;
	private int build;

	public Version(int major, int minor, int build) {
		this.major = major;
		this.minor = minor;
		this.build = build;
	}

	public Version(String s) {
		StringTokenizer tok = new StringTokenizer(s, ".");
		if (!tok.hasMoreElements()) return;
		major = Integer.parseInt(tok.nextToken());
		if (!tok.hasMoreElements()) return;
		minor = Integer.parseInt(tok.nextToken());
		if (!tok.hasMoreElements()) return;
		build = Integer.parseInt(tok.nextToken());
	}

	public Version(Version version) {
		this(version.major, version.minor, version.build);
	}

	public void incrementMajor() {
		major++;
	}

	public void incrementMinor() {
		minor++;
	}

	public void icrementBuild() {
		build++;
	}

	@Override
	public int compareTo(Version o) {
		if (major != o.major) return new Integer(major).compareTo(new Integer(o.major));
		if (minor != o.minor) return new Integer(minor).compareTo(new Integer(o.minor));
		if (build != o.build) return new Integer(build).compareTo(new Integer(o.build));
		return 0;
	}

	@Override
	public int hashCode() {
		return (major * 7) + (minor * 7) + (build * 7);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Version)) return false;
		Version o = (Version) obj;
		return major == o.major && minor == o.minor && build == o.build;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(major);
		if (minor != 0 || build != 0) {
			sb.append('.').append(minor);
			if (build != 0) {
				sb.append('.').append(build);
			}
		}
		return sb.toString();
	}

}
