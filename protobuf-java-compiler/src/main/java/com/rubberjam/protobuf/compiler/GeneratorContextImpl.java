package com.rubberjam.protobuf.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class GeneratorContextImpl implements GeneratorContext
{
	private final String outputDirectory;

	public GeneratorContextImpl(String outputDirectory)
	{
		this.outputDirectory = outputDirectory;
	}

	@Override
	public OutputStream open(String filename) throws FileNotFoundException
	{
		File file = new File(outputDirectory, filename);
		file.getParentFile().mkdirs();
		return new FileOutputStream(file);
	}
}
