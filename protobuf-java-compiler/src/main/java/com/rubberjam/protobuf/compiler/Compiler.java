package com.rubberjam.protobuf.compiler;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FileDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.rubberjam.protobuf.compiler.cpp.CppCodeGenerator;
import com.rubberjam.protobuf.compiler.csharp.CSharpCodeGenerator;

/** An API for compiling .proto files. */
public final class Compiler
{

	public interface ProtoImportResolver
	{
		/**
		 * Resolves a proto file path to its content.
		 * 
		 * @param path
		 *            The path of the proto file (e.g.,
		 *            "google/protobuf/descriptor.proto").
		 * @return The content of the proto file, or null if not found.
		 */
		String resolve(String path);
	}

	private static final class InMemoryGeneratorContext implements GeneratorContext
	{
		private final Map<String, ByteArrayOutputStream> files = new HashMap<>();

		@Override
		public OutputStream open(String filename)
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
				result.put(entry.getKey(), entry.getValue().toString());
			}
			return Collections.unmodifiableMap(result);
		}
	}

	public Map<String, String> compile(
			Map<String, String> protoFileContents, List<String> languages)
			throws CompilationException
	{
		return compile(protoFileContents, languages, null);
	}

	public Map<String, String> compile(
			Map<String, String> protoFileContents,
			List<String> languages,
			ProtoImportResolver importResolver)
			throws CompilationException
	{
		InMemoryGeneratorContext context = new InMemoryGeneratorContext();
		Map<String, FileDescriptorProto> fileDescriptorProtos = new HashMap<>();
		// Initial set of files to compile
		for (Map.Entry<String, String> entry : protoFileContents.entrySet())
		{
			parseProto(entry.getKey(), entry.getValue(), fileDescriptorProtos);
		}

		// Expand dependencies
		if (importResolver != null)
		{
			List<String> toProcess = new ArrayList<>(fileDescriptorProtos.keySet());
			Set<String> processed = new HashSet<>(toProcess);

			// We iterate using an index because toProcess grows
			for (int i = 0; i < toProcess.size(); i++)
			{
				String fileName = toProcess.get(i);
				FileDescriptorProto proto = fileDescriptorProtos.get(fileName);
				for (String dependency : proto.getDependencyList())
				{
					if (!processed.contains(dependency))
					{
						if (fileDescriptorProtos.containsKey(dependency))
						{
							// Already parsed but not in processed set? Should
							// not happen with logic above
							// unless passed in initial map but missed in
							// toProcess init.
							// But toProcess init took all keys.
							processed.add(dependency);
							continue;
						}

						String content = importResolver.resolve(dependency);
						if (content != null)
						{
							parseProto(dependency, content, fileDescriptorProtos);
							toProcess.add(dependency);
							processed.add(dependency);
						}
						// If content is null, we hope it's provided in the
						// initial map or we fail later at buildFrom
					}
				}
			}
		}

		Map<String, FileDescriptor> fileDescriptors = new HashMap<>();
		Set<String> building = new HashSet<>(); // To detect cycles

		// Build descriptors for the originally requested files
		for (String fileName : protoFileContents.keySet())
		{
			buildFileDescriptor(fileName, fileDescriptorProtos, fileDescriptors, building);
		}

		for (FileDescriptor fileDescriptor : fileDescriptors.values())
		{
			// Only generate code for the requested files, not dependencies
			// loaded via resolver
			if (protoFileContents.containsKey(fileDescriptor.getName()))
			{
				for (String language : languages)
				{
					CodeGenerator codeGenerator;
					if (language.equals("java"))
					{
						codeGenerator = new JavaCodeGenerator();
					}
					else if (language.equals("cpp"))
					{
						codeGenerator = new CppCodeGenerator();
					}
					else if (language.equals("csharp"))
					{
						codeGenerator = new CSharpCodeGenerator();
					}
					else
					{
						throw new CompilationException("Unsupported language: " + language);
					}

					try
					{
						codeGenerator.generate(fileDescriptor, "", context);
					}
					catch (CodeGenerator.GenerationException e)
					{
						throw new CompilationException("Error generating code", e);
					}
				}
			}
		}
		return context.getFiles();
	}

	private FileDescriptor buildFileDescriptor(
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

	private void parseProto(String fileName, String content, Map<String, FileDescriptorProto> outProtos)
			throws CompilationException
	{
		FileDescriptorProto.Builder fileBuilder = FileDescriptorProto.newBuilder();
		fileBuilder.setName(fileName);

		List<String> errors = new ArrayList<>();
		ErrorCollector errorCollector = (line, column, message) ->
		{
			errors.add(fileName + ":" + line + ":" + column + ": " + message);
		};

		Tokenizer tokenizer = new Tokenizer(new StringReader(content), errorCollector);
		Parser parser = new Parser(errorCollector, new SourceLocationTable());
		if (!parser.parse(tokenizer, fileBuilder) || !errors.isEmpty())
		{
			throw new CompilationException("Error parsing proto file " + fileName + ":\n" + String.join("\n", errors));
		}
		outProtos.put(fileName, fileBuilder.build());
	}
}
