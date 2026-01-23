package com.rubberjam.protobuf.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.compiler.csharp.CSharpCodeGenerator;

@RunWith(Parameterized.class)
public class ComprehensiveCSharpProtoParityTest
{
	private final String protoFileName;
	private final String expectedCsFileName;

	public ComprehensiveCSharpProtoParityTest(String protoFileName, String expectedCsFileName)
	{
		this.protoFileName = protoFileName;
		this.expectedCsFileName = expectedCsFileName;
	}

	@Parameters(name = "{0}")
	public static Object[][] data()
	{
		return new Object[][] {
			{ "comprehensive_test_edge_cases.proto", "ComprehensiveTestEdgeCases.cs" },
			{ "comprehensive_test_extensions.proto", "ComprehensiveTestExtensions.cs" },
			{ "comprehensive_test_nested.proto", "ComprehensiveTestNested.cs" },
			{ "comprehensive_test_v2.proto", "ComprehensiveTestV2.cs" },
			{ "comprehensive_test_v3.proto", "ComprehensiveTestV3.cs" },
		};
	}

	@Test
	public void testProtoGeneratesExpectedCSharpSource() throws Exception
	{
		// Read the proto file
		String protoContent = readProtoFile(protoFileName);
		assertNotNull("Proto file not found: " + protoFileName, protoContent);

		// Parse the proto file into FileDescriptorProto
		FileDescriptorProto fileDescriptorProto = parseProtoFile(protoFileName, protoContent);

		// Generate C# source
		String generatedSource = generateCSharpSource(
			fileDescriptorProto,
			Collections.emptyMap(),
			""
		);

		assertNotNull("Generated C# source should not be null", generatedSource);

		// Read the expected C# file
		String expected = readExpectedCSharpFile(expectedCsFileName);
		assertNotNull("Expected C# file not found: " + expectedCsFileName, expected);

		// Compare the generated source with expected
		String actual = generatedSource.trim();
		String expectedTrimmed = expected.trim();

		// Compare line by line for better error messages
		String[] actualLines = actual.split("\n");
		String[] expectedLines = expectedTrimmed.split("\n");

		int maxLines = Math.max(actualLines.length, expectedLines.length);
		for (int i = 0; i < maxLines; i++)
		{
			String actualLine = i < actualLines.length ? actualLines[i] : null;
			String expectedLine = i < expectedLines.length ? expectedLines[i] : null;

			if (actualLine == null)
			{
				assertEquals("Line " + (i + 1) + ": Expected line missing in generated code", expectedLine, actualLine);
			}
			else if (expectedLine == null)
			{
				assertEquals("Line " + (i + 1) + ": Extra line in generated code", expectedLine, actualLine);
			}
			else if (!actualLine.trim().equals(expectedLine.trim()))
			{
				// Print context for debugging - show 5 lines before and after
				System.out.println("Mismatch at line " + (i + 1) + ":");
				int start = Math.max(0, i - 5);
				int end = Math.min(maxLines, i + 6);
				for (int j = start; j < end; j++)
				{
					String expectedPart = j < expectedLines.length ? expectedLines[j] : "<missing>";
					String actualPart = j < actualLines.length ? actualLines[j] : "<missing>";
					if (j == i)
					{
						System.out.println(">>> " + (j + 1) + " Expected: " + expectedPart);
						System.out.println(">>> " + (j + 1) + " Actual:   " + actualPart);
					}
					else
					{
						System.out.println("    " + (j + 1) + " Expected: " + expectedPart);
						System.out.println("    " + (j + 1) + " Actual:   " + actualPart);
					}
				}
				// System.out.println("FULL EXPECTED FILE:\n" + expected);
				// System.out.println("FULL ACTUAL FILE:\n" + actual);
				assertEquals("Line " + (i + 1) + " mismatch", expectedLine.trim(), actualLine.trim());
			}
		}

		// Full comparison as fallback
		assertEquals("Generated C# source does not match expected", expectedTrimmed, actual);
	}

	private String readProtoFile(String fileName) throws Exception
	{
		File protoFile = new File("src/test/protobuf", fileName);
		if (!protoFile.exists())
		{
			// Try relative to project root
			protoFile = new File("protobuf-java-compiler/src/test/protobuf", fileName);
		}
		if (!protoFile.exists())
		{
			return null;
		}
		return new String(Files.readAllBytes(protoFile.toPath()), StandardCharsets.UTF_8);
	}

	private String readExpectedCSharpFile(String fileName) throws Exception
	{
		File csFile = new File("src/test/resources/expected/csharp", fileName);
		if (!csFile.exists())
		{
			// Try relative to project root
			csFile = new File("protobuf-java-compiler/src/test/resources/expected/csharp", fileName);
		}
		if (!csFile.exists())
		{
			// Try as resource
			try (InputStream stream = ComprehensiveCSharpProtoParityTest.class.getClassLoader()
				.getResourceAsStream("expected/csharp/" + fileName))
			{
				if (stream != null)
				{
					try (BufferedReader reader = new BufferedReader(
						new InputStreamReader(stream, StandardCharsets.UTF_8)))
					{
						return reader.lines().collect(Collectors.joining("\n"));
					}
				}
			}
		}
		if (csFile.exists())
		{
			return new String(Files.readAllBytes(csFile.toPath()), StandardCharsets.UTF_8);
		}
		return null;
	}

	private FileDescriptorProto parseProtoFile(String fileName, String content)
		throws Exception
	{
		FileDescriptorProto.Builder fileBuilder = FileDescriptorProto.newBuilder();
		fileBuilder.setName(fileName);

		java.util.List<String> errors = new java.util.ArrayList<>();
		final int MAX_ERRORS = 100;
		com.rubberjam.protobuf.compiler.ErrorCollector errorCollector = (line, column, message) ->
		{
			if (errors.size() < MAX_ERRORS)
			{
				errors.add(fileName + ":" + line + ":" + column + ": " + message);
			}
			else if (errors.size() == MAX_ERRORS)
			{
				errors.add("... (error limit reached, possible infinite loop)");
			}
		};

		com.rubberjam.protobuf.compiler.Tokenizer tokenizer =
			new com.rubberjam.protobuf.compiler.Tokenizer(
				new java.io.StringReader(content), errorCollector);
		com.rubberjam.protobuf.compiler.Parser parser =
			new com.rubberjam.protobuf.compiler.Parser(
				errorCollector, new com.rubberjam.protobuf.compiler.SourceLocationTable());

		boolean parseSuccess = parser.parse(tokenizer, fileBuilder);

		if (!parseSuccess || !errors.isEmpty())
		{
			String errorMessage = "Error parsing proto file " + fileName;
			if (!errors.isEmpty())
			{
				errorMessage += ":\n" + String.join("\n", errors);
			}
			if (errors.size() >= MAX_ERRORS)
			{
				errorMessage += "\n\nToo many errors detected - possible infinite loop in parser.";
			}
			throw new com.rubberjam.protobuf.compiler.CompilationException(errorMessage);
		}

		return fileBuilder.build();
	}

	private String generateCSharpSource(
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

		InMemoryGeneratorContext context = new InMemoryGeneratorContext();

		CSharpCodeGenerator codeGenerator = new CSharpCodeGenerator();

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

		// Find the main C# file
		String source = null;
		for (Map.Entry<String, String> entry : generatedFiles.entrySet())
		{
			if (entry.getKey().endsWith(".cs"))
			{
				source = entry.getValue();
				break;
			}
		}

		if (source == null)
		{
			throw new CompilationException("No C# file was generated");
		}

		return source;
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
			if (fileName.startsWith("google/protobuf/"))
			{
				FileDescriptor wellKnownDescriptor = getWellKnownTypeDescriptor(fileName);
				if (wellKnownDescriptor != null)
				{
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

	private FileDescriptor getWellKnownTypeDescriptor(String fileName)
	{
		try
		{
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
					return com.google.protobuf.DescriptorProtos.FileDescriptorProto.getDescriptor().getFile();
				default:
					return null;
			}
		}
		catch (Exception e)
		{
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
