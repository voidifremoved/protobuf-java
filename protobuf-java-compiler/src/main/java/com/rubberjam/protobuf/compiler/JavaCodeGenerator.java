package com.rubberjam.protobuf.compiler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.compiler.java.FileGenerator;
import com.rubberjam.protobuf.compiler.java.Options;
import com.rubberjam.protobuf.io.Printer;

public class JavaCodeGenerator extends CodeGenerator
{
	
	public List<GeneratedJavaFile> generateJavaFiles(FileDescriptor file, String parameter, GeneratorContext generatorContext)
	throws GenerationException
	{
		
		List<GeneratedJavaFile> target = new ArrayList<>();
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

			GeneratedJavaFile f = new GeneratedJavaFile(javaFilename, fileGenerator.getJavaPackage(), fileGenerator.getClassName(), printer.toString());
			target.add(f);

			
		}
		return target;

	}
	
	@Override
	public void generate(FileDescriptor file, String parameter, GeneratorContext generatorContext)
			throws GenerationException
	{
		List<GeneratedJavaFile> javaFiles = generateJavaFiles(file, parameter, generatorContext);

		
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

		try
		{
			for (GeneratedJavaFile f : javaFiles)
			{
				byte[] bytes = f.getSource().getBytes(StandardCharsets.UTF_8);
				
				
				try (java.io.OutputStream out = generatorContext.open(f.getFileName()))
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
					for (GeneratedJavaFile f : javaFiles)
					{
						writer.println(f.getFileName());
					}
				}
			}

			String annotationListFile = fileOptions.getAnnotationListFile();
			if (annotationListFile != null && !annotationListFile.isEmpty())
			{
				try (java.io.PrintWriter writer = new java.io.PrintWriter(
						new java.io.OutputStreamWriter(generatorContext.open(annotationListFile), StandardCharsets.UTF_8)))
				{
					for (GeneratedJavaFile f : javaFiles)
					{
						if (fileOptions.isAnnotateCode())
						{
							writer.println(f.getFileName());
						}
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
	
	public static final class GeneratedJavaFile
	{
		private final String fileName;
		private final String packageName;
		private final String className;
		private final String source;

		public GeneratedJavaFile(String fileName, String packageName, String className, String source)
		{
			this.fileName = fileName;
			this.packageName = packageName;
			this.className = className;
			this.source = source;
		}

		public String getFileName()
		{
			return fileName;
		}

		public String getPackageName()
		{
			return packageName;
		}

		public String getClassName()
		{
			return className;
		}

		public String getSource()
		{
			return source;
		}
	}
}
