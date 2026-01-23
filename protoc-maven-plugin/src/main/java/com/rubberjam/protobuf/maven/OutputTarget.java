package com.rubberjam.protobuf.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.plugins.annotations.Parameter;

public class OutputTarget {
	@Parameter
	public String type = "java";

	@Parameter
	public String addSources = "main";

	@Parameter
	public boolean cleanOutputFolder = false;

	@Parameter
	public String pluginPath;

	@Parameter
	public String pluginArtifact;

	@Parameter
	public File outputDirectory;

	@Parameter
	public String outputDirectorySuffix;

	@Parameter
	public String outputOptions;

	@Override
	public String toString() {
		return type + " -> " + outputDirectory + " (add: " + addSources + ", clean: " + cleanOutputFolder + ", plugin: " + pluginPath + ", outputOptions: " + outputOptions + ")";
	}
}

