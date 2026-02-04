package com.rubberjam.protobuf.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.Test;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.rubberjam.protobuf.compiler.JavaCodeGenerator.GeneratedJavaFile;
import com.rubberjam.protobuf.compiler.runtime.RuntimeJavaGenerator;
import com.rubberjam.protobuf.io.Tokenizer;


public class FailCasesMigrationTest
{


	@Test
	public void testFieldCommentProto2Speed() throws Exception
	{
		verifyParity("syntax = \"proto2\";\n"
				+ "\n"
				+ "package ComprehensiveTest.EdgeCasesMinimal;\n"
				+ "\n"
				+ "option java_package = \"com.rubberjam.protobuf.compiler.test.edge\";\n"
				+ "option java_outer_classname = \"ComprehensiveTestEdgeCasesMinimal\";\n"
				+ "option optimize_for = SPEED;\n"
				+ "\n"
				+ "// Message with field number edge cases\n"
				+ "message EdgeCaseMinimal {\n"
				+ "\n"
				+ "  optional string field19000 = 18000;  // High field number\n"
				+ "\n"
				+ "}\n"
				+ "", loadExpectedContent("FIELD_COMMENT_PROTO2_SPEED.txt"));
	}
	
	@Test
	public void testFieldCommentProto3Speed() throws Exception
	{
		verifyParity("syntax = \"proto3\";\n"
				+ "\n"
				+ "package ComprehensiveTest.EdgeCasesMinimal;\n"
				+ "\n"
				+ "option java_package = \"com.rubberjam.protobuf.compiler.test.edge\";\n"
				+ "option java_outer_classname = \"ComprehensiveTestEdgeCasesMinimal\";\n"
				+ "option optimize_for = SPEED;\n"
				+ "\n"
				+ "// Message with field number edge cases\n"
				+ "message EdgeCaseMinimal {\n"
				+ "\n"
				+ "  optional string field19000 = 18000;  // High field number\n"
				+ "\n"
				+ "}\n"
				+ "", loadExpectedContent("FIELD_COMMENT_PROTO3_SPEED.txt"));
	}
	
	@Test
	public void testRepeatedFieldProto2Speed() throws Exception
	{
		verifyParity("syntax = \"proto2\";\n"
				+ "\n"
				+ "package ComprehensiveTest.EdgeCasesMinimal;\n"
				+ "\n"
				+ "option java_package = \"com.rubberjam.protobuf.compiler.test.edge\";\n"
				+ "option java_outer_classname = \"ComprehensiveTestEdgeCasesMinimal\";\n"
				+ "option optimize_for = SPEED;\n"
				+ "\n"
				+ "// Message with many repeated fields\n"
				+ "message ManyRepeatedFields {\n"
				+ "  repeated int32 repeated1 = 1;\n"
				+ "  repeated string repeated_string1 = 11;\n"
				+ "  repeated bool repeated_bool1 = 14;\n"
				+ "}\n"
				+ "\n"
				+ "", loadExpectedContent("REPEATED_FIELD_PROTO2_SPEED.txt"));
	}
	
	@Test
	public void testRepeatedFieldProto3Speed() throws Exception
	{
		verifyParity("syntax = \"proto3\";\n"
				+ "\n"
				+ "package ComprehensiveTest.EdgeCasesMinimal;\n"
				+ "\n"
				+ "option java_package = \"com.rubberjam.protobuf.compiler.test.edge\";\n"
				+ "option java_outer_classname = \"ComprehensiveTestEdgeCasesMinimal\";\n"
				+ "option optimize_for = SPEED;\n"
				+ "\n"
				+ "// Message with many repeated fields\n"
				+ "message ManyRepeatedFields {\n"
				+ "  repeated int32 repeated1 = 1;\n"
				+ "  repeated string repeated_string1 = 11;\n"
				+ "  repeated bool repeated_bool1 = 14;\n"
				+ "}\n"
				+ "\n"
				+ "", loadExpectedContent("REPEATED_FIELD_PROTO3_SPEED.txt"));
	}
	
	@Test
	public void testNestedMessageProto2Speed() throws Exception
	{
		verifyParity("syntax = \"proto2\";\n"
				+ "\n"
				+ "package ComprehensiveTest.EdgeCasesMinimal;\n"
				+ "\n"
				+ "option java_package = \"com.rubberjam.protobuf.compiler.test.edge\";\n"
				+ "option java_outer_classname = \"ComprehensiveTestEdgeCasesMinimal\";\n"
				+ "option optimize_for = SPEED;\n"
				+ "\n"
				+ "// Deeply nested messages\n"
				+ "message Level1 {\n"
				+ "  optional string level1_field = 1;\n"
				+ "  \n"
				+ "  message Level2 {\n"
				+ "    optional string level2_field = 1;\n"
				+ "    \n"
				+ "    message Level3 {\n"
				+ "      optional string level3_field = 1;\n"
				+ "      \n"
				+ "\n"
				+ "\n"
				+ "    }\n"
				+ "    \n"
				+ "    optional Level3 level3_message = 2;\n"
				+ "    repeated Level3 repeated_level3 = 3;\n"
				+ "    \n"
				+ "  }\n"
				+ "  \n"
				+ "  optional Level2 level2_message = 2;\n"
				+ "\n"
				+ "\n"
				+ "}\n"
				+ "", loadExpectedContent("NESTED_MESSAGES_PROTO2_SPEED.txt"));
	}
	
	@Test
	public void testNestedMessageProto3Speed() throws Exception
	{
		verifyParity("syntax = \"proto3\";\n"
				+ "\n"
				+ "package ComprehensiveTest.EdgeCasesMinimal;\n"
				+ "\n"
				+ "option java_package = \"com.rubberjam.protobuf.compiler.test.edge\";\n"
				+ "option java_outer_classname = \"ComprehensiveTestEdgeCasesMinimal\";\n"
				+ "option optimize_for = SPEED;\n"
				+ "\n"
				+ "// Deeply nested messages\n"
				+ "message Level1 {\n"
				+ "  optional string level1_field = 1;\n"
				+ "  \n"
				+ "  message Level2 {\n"
				+ "    optional string level2_field = 1;\n"
				+ "    \n"
				+ "    message Level3 {\n"
				+ "      optional string level3_field = 1;\n"
				+ "      \n"
				+ "\n"
				+ "\n"
				+ "    }\n"
				+ "    \n"
				+ "    optional Level3 level3_message = 2;\n"
				+ "    repeated Level3 repeated_level3 = 3;\n"
				+ "    \n"
				+ "  }\n"
				+ "  \n"
				+ "  optional Level2 level2_message = 2;\n"
				+ "\n"
				+ "\n"
				+ "}\n"
				+ "", loadExpectedContent("NESTED_MESSAGES_PROTO3_SPEED.txt"));
	}

	private static String loadExpectedContent(String resourceName) throws IOException {
		try (InputStream in = FailCasesMigrationTest.class.getResourceAsStream(resourceName)) {
			if (in == null) {
				throw new IOException("Resource not found: " + resourceName);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private void verifyParity(String protoContent, String expected) throws Exception
	{

		// Parse the proto file into FileDescriptorProto using Compiler
		FileDescriptorProto fileDescriptorProto = parseProtoFile(protoContent);

		// Generate Java source using RuntimeJavaGenerator
		GeneratedJavaFile generated = RuntimeJavaGenerator.generateJavaSource(
				fileDescriptorProto,
				Collections.emptyMap(), // No dependencies for these test files
				"" // Empty parameter for default options
		);

		assertNotNull("Generated Java file should not be null", generated);
		assertNotNull("Generated source should not be null", generated.getSource());


		// Compare the generated source with expected
		String actual = generated.getSource().trim();
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
			else if (!actualLine.equals(expectedLine))
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
				assertEquals("Line " + (i + 1) + " mismatch", expectedLine, actualLine);
			}
		}

		// Full comparison as fallback
		assertEquals("Generated Java source does not match expected", expectedTrimmed, actual);
	}
	
	private FileDescriptorProto parseProtoFile(String content)
			throws Exception
	{
		
		String fileName = "comprehensive_test_edge_cases_minimal.proto";
		FileDescriptorProto.Builder fileBuilder = FileDescriptorProto.newBuilder();
		fileBuilder.setName(fileName);

		java.util.List<String> errors = new java.util.ArrayList<>();
		final int MAX_ERRORS = 100; // Limit errors to prevent infinite loops
		ErrorCollector errorCollector = (line, column, message) ->
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

		Tokenizer tokenizer = new Tokenizer(
				new java.io.StringReader(content), errorCollector);
		Parser parser = new Parser();

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
			throw new CompilationException(errorMessage);
		}

		return fileBuilder.build();
	}
}
