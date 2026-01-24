package com.rubberjam.protobuf.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.rubberjam.protobuf.compiler.runtime.RuntimeJavaGenerator;
import com.rubberjam.protobuf.compiler.runtime.RuntimeJavaGenerator.GeneratedJavaFile;

@RunWith(Parameterized.class)
public class ComprehensiveProtoParityTest
{
	private final String protoFileName;
	private final String expectedJavaFileName;

	public ComprehensiveProtoParityTest(String protoFileName, String expectedJavaFileName)
	{
		this.protoFileName = protoFileName;
		this.expectedJavaFileName = expectedJavaFileName;
	}

	@Parameters(name = "{0}")
	public static Object[][] data()
	{
		return new Object[][] {
			{ "comprehensive_test_edge_cases.proto", "ComprehensiveTestEdgeCases.java" },
			{ "comprehensive_test_extensions.proto", "ComprehensiveTestExtensions.java" },
			{ "comprehensive_test_nested.proto", "ComprehensiveTestNested.java" },
			{ "comprehensive_test_v2.proto", "ComprehensiveTestV2.java" },
			{ "comprehensive_test_v3.proto", "ComprehensiveTestV3.java" },
		};
	}

	@Test
	public void testProtoGeneratesExpectedJavaSource() throws Exception
	{
		// Read the proto file
		String protoContent = readProtoFile(protoFileName);
		assertNotNull("Proto file not found: " + protoFileName, protoContent);

		// Parse the proto file into FileDescriptorProto using Compiler
		// We'll compile with an empty language list to just parse, then extract the FileDescriptorProto
		Compiler compiler = new Compiler();
		Map<String, String> protoFiles = Collections.singletonMap(protoFileName, protoContent);
		
		// Use Compiler to parse - compile with empty language list to just parse
		// Then we need to extract the FileDescriptorProto from the compiler
		// Actually, let's use a simpler approach - parse directly but with error limit
		FileDescriptorProto fileDescriptorProto = parseProtoFile(protoFileName, protoContent);

		// Generate Java source using RuntimeJavaGenerator
		GeneratedJavaFile generated = RuntimeJavaGenerator.generateJavaSource(
			fileDescriptorProto,
			Collections.emptyMap(), // No dependencies for these test files
			"" // Empty parameter for default options
		);

		assertNotNull("Generated Java file should not be null", generated);
		assertNotNull("Generated source should not be null", generated.getSource());

		// Read the expected Java file
		String expected = readExpectedJavaFile(expectedJavaFileName);
		assertNotNull("Expected Java file not found: " + expectedJavaFileName, expected);

		// Compare the generated source with expected
		String actual = generated.getSource().trim();
		String expectedTrimmed = expected.trim();

		File expectedFile = new File("target/" + expectedJavaFileName + "_Expected.txt");
		Files.writeString(expectedFile.toPath(), expectedTrimmed);
		File actualFile = new File("target/" + expectedJavaFileName + "_Actual.txt");
		Files.writeString(actualFile.toPath(), actual);
		
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
				//System.out.println("FULL EXPECTED FILE:\n" + expected);
				//System.out.println("FULL ACTUAL FILE:\n" + actual);
				assertEquals("Line " + (i + 1) + " mismatch", expectedLine.trim(), actualLine.trim());
			}
		}

		// Full comparison as fallback
		assertEquals("Generated Java source does not match expected", expectedTrimmed, actual);
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

	private String readExpectedJavaFile(String fileName) throws Exception
	{
		File javaFile = new File("src/test/resources/expected/java", fileName);
		if (!javaFile.exists())
		{
			// Try relative to project root
			javaFile = new File("protobuf-java-compiler/src/test/resources/expected/java", fileName);
		}
		if (!javaFile.exists())
		{
			// Try as resource
			try (InputStream stream = ComprehensiveProtoParityTest.class.getClassLoader()
				.getResourceAsStream("expected/java/" + fileName))
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
		if (javaFile.exists())
		{
			return new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);
		}
		return null;
	}

	private FileDescriptorProto parseProtoFile(String fileName, String content)
		throws Exception
	{
		// Use Compiler to parse - compile with empty language list to just parse
		// Then extract the FileDescriptorProto from internal state
		// Actually, let's parse manually but with error limit to prevent infinite loops
		FileDescriptorProto.Builder fileBuilder = FileDescriptorProto.newBuilder();
		fileBuilder.setName(fileName);

		java.util.List<String> errors = new java.util.ArrayList<>();
		final int MAX_ERRORS = 100; // Limit errors to prevent infinite loops
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
}

