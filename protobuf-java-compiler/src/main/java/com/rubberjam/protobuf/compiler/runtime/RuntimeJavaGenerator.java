package com.rubberjam.protobuf.compiler.runtime;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.compiler.CompilationException;
import com.rubberjam.protobuf.compiler.JavaCodeGenerator;
import com.rubberjam.protobuf.compiler.JavaCodeGenerator.GeneratedJavaFile;

/**
 * Helper for generating Java source at runtime from descriptor protos.
 * Uses the Compiler class to ensure proper code generation.
 */
public final class RuntimeJavaGenerator
{
	private RuntimeJavaGenerator()
	{
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
		
		JavaCodeGenerator codeGenerator = 
				new JavaCodeGenerator();
		
		List<JavaCodeGenerator.GeneratedJavaFile> javaFiles;
		try
		{
			javaFiles = codeGenerator.generateJavaFiles(fileDescriptor, parameter, context);
		}
		catch (com.rubberjam.protobuf.compiler.CodeGenerator.GenerationException e)
		{
			throw new CompilationException("Error generating code", e);
		}


		if (javaFiles.isEmpty())
		{
			throw new CompilationException("No files were generated");
		}

		return javaFiles.getFirst();
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
			// Check if this is a well-known type
			if (fileName.startsWith("google/protobuf/"))
			{
				FileDescriptor wellKnownDescriptor = getWellKnownTypeDescriptor(fileName);
				if (wellKnownDescriptor != null)
				{
					// Convert FileDescriptor to FileDescriptorProto and add to map
					proto = wellKnownDescriptor.toProto();
					protos.put(fileName, proto);
				}
			}
			
			if (proto == null)
			{
				throw new CompilationException("Missing dependency: " + fileName);
			}
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

	/**
	 * Gets the FileDescriptor for a well-known type from the protobuf runtime.
	 * Returns null if the type is not a well-known type or not found.
	 */
	private static FileDescriptor getWellKnownTypeDescriptor(String fileName)
	{
		try
		{
			// Map well-known type file names to their corresponding message classes
			// and get their FileDescriptors
			switch (fileName)
			{
				case "google/protobuf/any.proto":
					return com.google.protobuf.Any.getDescriptor().getFile();
				case "google/protobuf/timestamp.proto":
					return com.google.protobuf.Timestamp.getDescriptor().getFile();
				case "google/protobuf/duration.proto":
					return com.google.protobuf.Duration.getDescriptor().getFile();
				case "google/protobuf/struct.proto":
					return com.google.protobuf.Struct.getDescriptor().getFile();
				case "google/protobuf/empty.proto":
					return com.google.protobuf.Empty.getDescriptor().getFile();
				case "google/protobuf/field_mask.proto":
					return com.google.protobuf.FieldMask.getDescriptor().getFile();
				case "google/protobuf/wrappers.proto":
					return com.google.protobuf.BoolValue.getDescriptor().getFile();
				case "google/protobuf/api.proto":
					return com.google.protobuf.Api.getDescriptor().getFile();
				case "google/protobuf/type.proto":
					return com.google.protobuf.Type.getDescriptor().getFile();
				case "google/protobuf/source_context.proto":
					return com.google.protobuf.SourceContext.getDescriptor().getFile();
				case "google/protobuf/descriptor.proto":
					// This is special - it's the descriptor proto itself
					// Get it via FileDescriptorProto which is defined in descriptor.proto
					return com.google.protobuf.DescriptorProtos.FileDescriptorProto.getDescriptor().getFile();
				default:
					return null;
			}
		}
		catch (Exception e)
		{
			// If we can't get the descriptor, return null
			return null;
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

