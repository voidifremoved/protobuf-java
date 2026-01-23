package com.rubberjam.protobuf.compiler.runtime;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.compiler.CompilationException;
import com.rubberjam.protobuf.compiler.java.FileGenerator;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper for generating Java source at runtime from descriptor protos.
 * Uses the Compiler class to ensure proper code generation.
 */
public final class RuntimeJavaGenerator
{
	private RuntimeJavaGenerator()
	{
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

	public static GeneratedJavaFile generateJavaSource(
			FileDescriptorProto rootProto,
			Map<String, FileDescriptorProto> dependencyProtos,
			String parameter) throws CompilationException
	{
		if (rootProto == null)
		{
			throw new CompilationException("rootProto must not be null");
		}
		if (rootProto.getName() == null || rootProto.getName().isEmpty())
		{
			throw new CompilationException("rootProto.name must be set");
		}

		// Build FileDescriptor from protos
		Map<String, FileDescriptorProto> allProtos = new HashMap<>();
		if (dependencyProtos != null)
		{
			allProtos.putAll(dependencyProtos);
		}
		allProtos.put(rootProto.getName(), rootProto);

		FileDescriptor fileDescriptor = buildFileDescriptor(
				rootProto.getName(),
				allProtos,
				new HashMap<>(),
				new HashSet<>());

		// Use Compiler's JavaCodeGenerator approach (same as Compiler.compile())
		InMemoryGeneratorContext context = new InMemoryGeneratorContext();
		
		com.rubberjam.protobuf.compiler.JavaCodeGenerator codeGenerator = 
				new com.rubberjam.protobuf.compiler.JavaCodeGenerator();
		
		try
		{
			codeGenerator.generate(fileDescriptor, parameter, context);
		}
		catch (com.rubberjam.protobuf.compiler.CodeGenerator.GenerationException e)
		{
			throw new CompilationException("Error generating code", e);
		}

		Map<String, String> generatedFiles = context.getFiles();
		if (generatedFiles.isEmpty())
		{
			throw new CompilationException("No files were generated");
		}

		// Find the main Java file (should be only one for single proto)
		String fileName = null;
		String source = null;
		for (Map.Entry<String, String> entry : generatedFiles.entrySet())
		{
			if (entry.getKey().endsWith(".java") && !entry.getKey().endsWith(".pb.meta"))
			{
				fileName = entry.getKey();
				source = entry.getValue();
				break;
			}
		}

		if (fileName == null || source == null)
		{
			throw new CompilationException("No Java file was generated");
		}

		// Extract package and class name from FileGenerator
		com.rubberjam.protobuf.compiler.java.Options options = 
				com.rubberjam.protobuf.compiler.java.Options.fromParameter(parameter);
		boolean immutableApi = !options.generateMutableCode;
		FileGenerator fileGenerator = new FileGenerator(fileDescriptor, options, immutableApi);
		
		String packageName = fileGenerator.getJavaPackage();
		String className = fileGenerator.getClassName();

		return new GeneratedJavaFile(fileName, packageName, className, source);
	}

	public static Map<String, GeneratedJavaFile> generateJavaSources(
			Map<String, FileDescriptorProto> rootProtos,
			Map<String, FileDescriptorProto> dependencyProtos,
			String parameter) throws CompilationException
	{
		if (rootProtos == null || rootProtos.isEmpty())
		{
			return Collections.emptyMap();
		}
		Map<String, GeneratedJavaFile> results = new HashMap<>();
		for (FileDescriptorProto rootProto : rootProtos.values())
		{
			GeneratedJavaFile file = generateJavaSource(rootProto, dependencyProtos, parameter);
			results.put(file.getFileName(), file);
		}
		return results;
	}

	private static FileDescriptor buildFileDescriptor(
			String fileName,
			Map<String, FileDescriptorProto> protos,
			Map<String, FileDescriptor> cache,
			Set<String> building) throws CompilationException
	{
		if (cache.containsKey(fileName))
		{
			return cache.get(fileName);
		}
		if (building.contains(fileName))
		{
			throw new CompilationException("Circular dependency detected involving: " + fileName);
		}
		building.add(fileName);

		FileDescriptorProto proto = protos.get(fileName);
		if (proto == null)
		{
			throw new CompilationException("Missing dependency: " + fileName);
		}

		FileDescriptor[] dependencies = new FileDescriptor[proto.getDependencyCount()];
		for (int i = 0; i < proto.getDependencyCount(); i++)
		{
			String depName = proto.getDependency(i);
			dependencies[i] = buildFileDescriptor(depName, protos, cache, building);
		}

		try
		{
			FileDescriptor result = FileDescriptor.buildFrom(proto, dependencies);
			cache.put(fileName, result);
			building.remove(fileName);
			return result;
		}
		catch (com.google.protobuf.Descriptors.DescriptorValidationException e)
		{
			throw new CompilationException("Error building file descriptor for " + fileName, e);
		}
	}

	private static class InMemoryGeneratorContext implements com.rubberjam.protobuf.compiler.GeneratorContext
	{
		private final Map<String, ByteArrayOutputStream> files = new HashMap<>();

		@Override
		public java.io.OutputStream open(String filename)
		{
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			files.put(filename, stream);
			return stream;
		}

		public Map<String, String> getFiles()
		{
			Map<String, String> result = new HashMap<>();
			for (Map.Entry<String, ByteArrayOutputStream> entry : files.entrySet())
			{
				result.put(entry.getKey(), entry.getValue().toString(StandardCharsets.UTF_8));
			}
			return Collections.unmodifiableMap(result);
		}
	}
}

