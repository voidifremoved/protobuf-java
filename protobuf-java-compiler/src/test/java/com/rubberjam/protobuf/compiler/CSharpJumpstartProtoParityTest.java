package com.rubberjam.protobuf.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.rubberjam.protobuf.compiler.Compiler;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;

public class CSharpJumpstartProtoParityTest
{
	@Test
	public void testJumpstartProtoGeneratesExpectedCSharpSource() throws Exception
	{
		String protoFile =
				"package JumpstartServer.Messages.JumpstartTestMessages;\n"
						+ "\n"
						+ "option java_package = \"com.rubberjam.jumpstart.server.messages\";\n"
						+ "option java_outer_classname = \"JumpstartTestMessages\";\n"
						+ "option cc_generic_services = false;\n"
						+ "option java_generic_services = false;\n"
						+ "option optimize_for = SPEED;\n"
						+ "\n"
						+ "\n"
						+ "message TestAcknowledgement {\n"
						+ "\toptional string source = 1;\n"
						+ "\toptional uint32 errorCode = 2;\n"
						+ "}\n"
						+ "message TestAssetDownloadInfo {\n"
						+ "	repeated TestAssetDownloadRegion assetDownloadRegions = 1;\n"
						+ "	optional string clientString = 2;\n"
						+ "	optional TestAssetDownloadSettings assetDownloadSettings = 3;\n"
						+ "	\n"
						+ "	message TestAssetDownloadRegion {\n"
						+ "		optional string liveUrl = 2;\n"
						+ "		optional string testUrl = 3;\n"
						+ "		optional string url = 4;\n"
						+ "	}\n"
						+ "	\n"
						+ "	message TestAssetDownloadSettings {\n"
						+ "		optional bool downloadIsOptional = 1 [default=false];\n"
						+ "  		optional bool performConnectionTest = 2 [default=false];\n"
						+ "  		optional uint32 maxRetries = 3 [default=10];\n"
						+ "  		optional bool logErrors = 4 [default=false];\n"
						+ "	}\n"
						+ "}";

		Compiler compiler = new Compiler();
		Map<String, String> files =
				compiler.compile(
						Collections.singletonMap("jumpstart_test.proto", protoFile),
						Collections.singletonList("csharp"));

		String fileName = "JumpstartTest.cs";
		assertTrue("Expected generated file not found: " + files.keySet(), files.containsKey(fileName));

		String actual = files.get(fileName).trim();
		String expected = readResource("expected/jumpstart_test_expected.cs").trim();

		// Normalize line endings to avoid issues across platforms
		actual = actual.replace("\r\n", "\n");
		expected = expected.replace("\r\n", "\n");

		String[] acAr = actual.split("\n");
		String[] exAr = expected.split("\n");

		for (int i = 0; i < exAr.length && i< acAr.length; i++)
		{
			if (!exAr[i].equals(acAr[i]))
			{
				System.out.println("Mismatch at line " + (i+1) + ":");
				System.out.println("Expected: " + exAr[i]);
				System.out.println("Actual:   " + acAr[i]);
			}
		}

		assertEquals(expected, actual);
	}

	private static String readResource(String path) throws Exception
	{
		try (InputStream stream = CSharpJumpstartProtoParityTest.class.getClassLoader().getResourceAsStream(path))
		{
			if (stream == null)
			{
				throw new IllegalStateException("Missing resource: " + path);
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
			{
				return reader.lines().collect(Collectors.joining("\n"));
			}
		}
	}
}
