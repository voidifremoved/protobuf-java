
package com.rubberjam.protobuf.maven.protoc;

public class ProtocVersion
{
	public final String group;
	public final String artifact;
	public final String version;

	public static final ProtocVersion PROTOC_VERSION = new ProtocVersion(null, null, "4.28.3");

	public static ProtocVersion getVersion(String spec)
	{
		if (!spec.startsWith("-v")) return null;
		ProtocVersion version = null;
		String[] as = spec.split(":");
		if (as.length == 4 && as[0].equals("-v"))
			version = new ProtocVersion(as[1], as[2], as[3]);
		else version = new ProtocVersion(null, null, spec.substring(2));
		if (version.version.length() == 3)
		{ // "123" -> "1.2.3"
			String dotVersion = version.version.charAt(0) + "." + version.version.charAt(1) + "." + version.version.charAt(2);
			version = new ProtocVersion(version.group, version.artifact, dotVersion);
		}
		return version;
	}

	public ProtocVersion(String group, String artifact, String version)
	{
		this.group = group;
		this.artifact = artifact;
		this.version = version;
	}

	@Override
	public String toString()
	{
		if (artifact == null) return version;
		return group + ":" + artifact + ":" + version;
	}

}

