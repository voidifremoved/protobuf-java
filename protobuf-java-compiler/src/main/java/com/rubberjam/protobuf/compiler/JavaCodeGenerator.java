package com.rubberjam.protobuf.compiler;

import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.compiler.java.FileGenerator;
import com.rubberjam.protobuf.compiler.java.Options;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class JavaCodeGenerator extends CodeGenerator
{
	@Override
	public void generate(FileDescriptor file, String parameter, GeneratorContext generatorContext)
			throws GenerationException
	{
		generate(file, null, parameter, generatorContext);
	}

	public void generate(FileDescriptor file, com.google.protobuf.DescriptorProtos.FileDescriptorProto sourceProto, String parameter, GeneratorContext generatorContext)
			throws GenerationException
	{
		Options fileOptions = Options.fromParameter(parameter);

		if (fileOptions.enforceLite && fileOptions.generateMutableCode)
		{
			throw new GenerationException(
					"lite runtime generator option cannot be used with mutable API.");
		}

		// By default we generate immutable code and shared code for immutable
		// API.
		if (!fileOptions.generateImmutableCode
				&& !fileOptions.generateMutableCode
				&& !fileOptions.generateSharedCode)
		{
			fileOptions.generateImmutableCode = true;
			fileOptions.generateSharedCode = true;
		}

		List<String> allFiles = new ArrayList<>();
		List<String> allAnnotations = new ArrayList<>();

		List<FileGenerator> fileGenerators = new ArrayList<>();
		if (fileOptions.generateImmutableCode)
		{
			fileGenerators.add(new FileGenerator(file, sourceProto, fileOptions, /*
																	 * immutable=
																	 */ true));
		}
		if (fileOptions.generateMutableCode)
		{
			fileGenerators.add(new FileGenerator(file, sourceProto, fileOptions, /*
																	 * immutable=
																	 */ false));
		}

		for (FileGenerator fileGenerator : fileGenerators)
		{
			if (!fileGenerator.validate(
					error ->
					{
						try
						{
							throw new GenerationException(error);
						}
						catch (GenerationException e)
						{
							throw new RuntimeException(e);
						}
					}))
			{
				// Validation failed, and the error has been thrown.
				return;
			}
		}

		try
		{
			for (FileGenerator fileGenerator : fileGenerators)
			{
				String packageDir = fileGenerator.getJavaPackage().replace('.', '/');
				String javaFilename = packageDir + "/" + fileGenerator.getClassName() + ".java";
				allFiles.add(javaFilename);
				String infoFullPath = javaFilename + ".pb.meta";
				if (fileOptions.annotateCode)
				{
					allAnnotations.add(infoFullPath);
				}

				// Generate main java file.
				try (java.io.PrintWriter writer = new java.io.PrintWriter(generatorContext.open(javaFilename)))
				{
					fileGenerator.generate(writer);
				}

				// Generate sibling files.
				fileGenerator.generateSiblings(packageDir, generatorContext, allFiles, allAnnotations);
			}

			// Generate output list if requested.
			if (fileOptions.outputListFile != null)
			{
				try (java.io.PrintWriter writer = new java.io.PrintWriter(generatorContext.open(fileOptions.outputListFile)))
				{
					for (String fileName : allFiles)
					{
						writer.println(fileName);
					}
				}
			}

			if (fileOptions.annotationListFile != null)
			{
				try (java.io.PrintWriter writer = new java.io.PrintWriter(generatorContext.open(fileOptions.annotationListFile)))
				{
					for (String fileName : allAnnotations)
					{
						writer.println(fileName);
					}
				}
			}
		}
		catch (java.io.IOException e)
		{
			throw new GenerationException(e);
		}
	}

	@Override
	public long getSupportedFeatures()
	{
		return Feature.FEATURE_PROTO3_OPTIONAL.getValue();
	}

	@Override
	public Edition getMinimumEdition()
	{
		return Edition.EDITION_PROTO2;
	}

	@Override
	public Edition getMaximumEdition()
	{
		return Edition.EDITION_2023;
	}
}
