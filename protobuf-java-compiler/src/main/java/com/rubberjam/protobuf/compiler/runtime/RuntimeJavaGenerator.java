package com.rubberjam.protobuf.compiler.runtime;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.compiler.CompilationException;
import com.rubberjam.protobuf.compiler.java.FileGenerator;
import com.rubberjam.protobuf.compiler.java.Options;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper for generating Java source at runtime from descriptor protos.
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

		Options options = Options.fromParameter(parameter);
		applyDefaultOptions(options);
		validateOptions(options);

		boolean immutableApi = !options.generateMutableCode;
		FileGenerator fileGenerator = new FileGenerator(fileDescriptor, options, immutableApi);

		StringWriter stringWriter = new StringWriter();
		try (PrintWriter writer = new PrintWriter(stringWriter))
		{
			fileGenerator.generate(writer);
		}

		String packageName = fileGenerator.getJavaPackage();
		String className = fileGenerator.getClassName();
		String packageDir = packageName.replace('.', '/');
		String fileName = packageDir.isEmpty() ? className + ".java" : packageDir + "/" + className + ".java";
		return new GeneratedJavaFile(fileName, packageName, className, stringWriter.toString());
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

	private static void applyDefaultOptions(Options options)
	{
		if (!options.generateImmutableCode
				&& !options.generateMutableCode
				&& !options.generateSharedCode)
		{
			options.generateImmutableCode = true;
			options.generateSharedCode = true;
		}
	}

	private static void validateOptions(Options options) throws CompilationException
	{
		if (options.enforceLite && options.generateMutableCode)
		{
			throw new CompilationException("lite runtime generator option cannot be used with mutable API.");
		}
		if (options.generateImmutableCode && options.generateMutableCode)
		{
			throw new CompilationException("Immutable and mutable generation cannot both be enabled for runtime generation.");
		}
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
		catch (Descriptors.DescriptorValidationException e)
		{
			throw new CompilationException("Error building file descriptor for " + fileName, e);
		}
	}
}

