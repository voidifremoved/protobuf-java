package com.rubberjam.protobuf.compiler.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.rubberjam.protobuf.compiler.CompilationException;
import com.rubberjam.protobuf.compiler.ErrorCollector;
import com.rubberjam.protobuf.compiler.JavaCodeGenerator.GeneratedJavaFile;
import com.rubberjam.protobuf.compiler.Parser;
import com.rubberjam.protobuf.compiler.runtime.RuntimeJavaGenerator;
import com.rubberjam.protobuf.io.Tokenizer;

public abstract class AbstractProtoParityTest {
	protected void verifyParity(String protoFileName, String expectedJavaFileName) throws Exception {
		// Read the proto file
		String protoContent = readProtoFile(protoFileName);
		assertNotNull("Proto file not found: " + protoFileName, protoContent);

		// Parse the proto file into FileDescriptorProto using Compiler
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
		if (expectedFile.getParentFile() != null) {
			expectedFile.getParentFile().mkdirs();
		}
		Files.writeString(expectedFile.toPath(), expectedTrimmed);

		File actualFile = new File("target/" + expectedJavaFileName + "_Actual.txt");
		if (actualFile.getParentFile() != null) {
			actualFile.getParentFile().mkdirs();
		}
		Files.writeString(actualFile.toPath(), actual);
		
		

		// Compare line by line for better error messages
		String[] actualLines = actual.split("\n");
		String[] expectedLines = expectedTrimmed.split("\n");

		List<String> expectedLinesList = Arrays.asList(expectedLines);
		List<String> actualLinesList = Arrays.asList(actualLines);
		Patch<String> diff = DiffUtils.diff(expectedLinesList, actualLinesList);
		
		if (!diff.getDeltas().isEmpty())
		{
			
			System.out.println("OUTPUT MISMATCH IN " + expectedJavaFileName + ", DIFF:");
			List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(expectedJavaFileName + "_Expected.txt", expectedJavaFileName + "_Actual.txt", expectedLinesList, diff, 3);
			
			
			
			for (String string : unifiedDiff) {			
				System.out.println(string);
			}
			
			

			System.out.println("DIFF ENDS");
		}
		
		
		
		
		// Full comparison as fallback
		assertEquals("Generated Java source does not match expected", diff.getDeltas().isEmpty(), true);
	}

	protected String readProtoFile(String fileName) throws Exception {
		File protoFile = new File("src/test/protobuf", fileName);
		if (!protoFile.exists()) {
			// Try relative to project root
			protoFile = new File("protobuf-java-compiler/src/test/protobuf", fileName);
		}
		if (!protoFile.exists()) {
			return null;
		}
		return new String(Files.readAllBytes(protoFile.toPath()), StandardCharsets.UTF_8);
	}

	protected String readExpectedJavaFile(String fileName) throws Exception {
		File javaFile = new File("src/test/resources/expected/java", fileName);
		if (!javaFile.exists()) {
			// Try relative to project root
			javaFile = new File("protobuf-java-compiler/src/test/resources/expected/java", fileName);
		}
		if (!javaFile.exists()) {
			// Try as resource
			try (InputStream stream = AbstractProtoParityTest.class.getClassLoader()
					.getResourceAsStream("expected/java/" + fileName)) {
				if (stream != null) {
					try (BufferedReader reader = new BufferedReader(
							new InputStreamReader(stream, StandardCharsets.UTF_8))) {
						return reader.lines().collect(Collectors.joining("\n"));
					}
				}
			}
		}
		if (javaFile.exists()) {
			return new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);
		}
		return null;
	}

	protected FileDescriptorProto parseProtoFile(String fileName, String content)
			throws Exception {
		FileDescriptorProto.Builder fileBuilder = FileDescriptorProto.newBuilder();
		fileBuilder.setName(fileName);

		java.util.List<String> errors = new java.util.ArrayList<>();
		final int MAX_ERRORS = 100; // Limit errors to prevent infinite loops
		ErrorCollector errorCollector = (line, column, message) -> {
			if (errors.size() < MAX_ERRORS) {
				errors.add(fileName + ":" + line + ":" + column + ": " + message);
			} else if (errors.size() == MAX_ERRORS) {
				errors.add("... (error limit reached, possible infinite loop)");
			}
		};

		Tokenizer tokenizer = new Tokenizer(
				new java.io.StringReader(content), errorCollector);
		Parser parser = new Parser();

		boolean parseSuccess = parser.parse(tokenizer, fileBuilder);

		if (!parseSuccess || !errors.isEmpty()) {
			String errorMessage = "Error parsing proto file " + fileName;
			if (!errors.isEmpty()) {
				errorMessage += ":\n" + String.join("\n", errors);
			}
			if (errors.size() >= MAX_ERRORS) {
				errorMessage += "\n\nToo many errors detected - possible infinite loop in parser.";
			}
			throw new CompilationException(errorMessage);
		}

		return fileBuilder.build();
	}
}
