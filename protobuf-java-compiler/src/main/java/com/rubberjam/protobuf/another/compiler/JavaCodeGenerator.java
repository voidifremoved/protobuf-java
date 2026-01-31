package com.rubberjam.protobuf.another.compiler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.another.compiler.java.FileGenerator;
import com.rubberjam.protobuf.another.compiler.java.Options;
import com.rubberjam.protobuf.io.Printer;

public class JavaCodeGenerator extends CodeGenerator
{
	@Override
	public void generate(FileDescriptor file, String parameter, GeneratorContext generatorContext)
			throws GenerationException
	{
		Options fileOptions = Options.fromParameter(parameter);

		if (fileOptions.isEnforceLite() && fileOptions.isGenerateMutableCode())
		{
			throw new GenerationException(
					"lite runtime generator option cannot be used with mutable API.");
		}

		// By default we generate immutable code and shared code for immutable API.
		if (!fileOptions.isGenerateImmutableCode()
				&& !fileOptions.isGenerateMutableCode()
				&& !fileOptions.isGenerateSharedCode())
		{
			fileOptions.setGenerateImmutableCode(true);
			fileOptions.setGenerateSharedCode(true);
		}

		List<String> allFiles = new ArrayList<>();
		List<String> allAnnotations = new ArrayList<>();

		List<FileGenerator> fileGenerators = new ArrayList<>();
		if (fileOptions.isGenerateImmutableCode())
		{
			fileGenerators.add(new FileGenerator(file, fileOptions));
		}
		if (fileOptions.isGenerateMutableCode() && !fileOptions.isGenerateImmutableCode())
		{
			// another.compiler.java FileGenerator currently only supports immutable API.
			fileGenerators.add(new FileGenerator(file, fileOptions));
		}

		List<String> validationErrors = new ArrayList<>();
		for (FileGenerator fileGenerator : fileGenerators)
		{
			if (!fileGenerator.validate(validationErrors))
			{
				break;
			}
		}
		if (!validationErrors.isEmpty())
		{
			throw new GenerationException(validationErrors.get(0));
		}

		try
		{
			for (FileGenerator fileGenerator : fileGenerators)
			{
				String packageDir = fileGenerator.getJavaPackage().replace('.', '/');
				String javaFilename = packageDir + "/" + fileGenerator.getClassName() + ".java";
				allFiles.add(javaFilename);
				String infoFullPath = javaFilename + ".pb.meta";
				if (fileOptions.isAnnotateCode())
				{
					allAnnotations.add(infoFullPath);
				}

				// Generate main java file using another.compiler's Printer (buffer then write).
				Printer.Options printerOptions = new Printer.Options();
				Printer printer = new Printer(printerOptions);
				fileGenerator.generate(printer);
				byte[] bytes = printer.toString().getBytes(StandardCharsets.UTF_8);
				try (java.io.OutputStream out = generatorContext.open(javaFilename))
				{
					out.write(bytes);
				}
			}

			// Generate output list if requested.
			String outputListFile = fileOptions.getOutputListFile();
			if (outputListFile != null && !outputListFile.isEmpty())
			{
				try (java.io.PrintWriter writer = new java.io.PrintWriter(
						new java.io.OutputStreamWriter(generatorContext.open(outputListFile), StandardCharsets.UTF_8)))
				{
					for (String fileName : allFiles)
					{
						writer.println(fileName);
					}
				}
			}

			String annotationListFile = fileOptions.getAnnotationListFile();
			if (annotationListFile != null && !annotationListFile.isEmpty())
			{
				try (java.io.PrintWriter writer = new java.io.PrintWriter(
						new java.io.OutputStreamWriter(generatorContext.open(annotationListFile), StandardCharsets.UTF_8)))
				{
					for (String fileName : allAnnotations)
					{
						writer.println(fileName);
					}
				}
			}
		}
		catch (IOException e)
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
