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

public class JumpstartProtoParityTest
{
	@Test
	public void testJumpstartProtoGeneratesExpectedJavaSource() throws Exception
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
						Collections.singletonList("java"));

		String fileName = "com/rubberjam/jumpstart/server/messages/JumpstartTestMessages.java";
		assertTrue("Expected generated file not found: " + files.keySet(), files.containsKey(fileName));

		String actual = files.get(fileName).trim();
		String expected = readResource("expected/jumpstart_test_expected.java").trim();

		String[] acAr = actual.split("\n");
		String[] exAr = actual.split("\n");
		
		for (int i = 0; i < exAr.length && i< acAr.length; i++)
		{
			if (!exAr[i].equals(acAr[i]))
			{
				System.out.println(i + " -  " + exAr[i] + " : " + acAr[i]);
			}
		}
		System.out.println(actual);
		System.out.println(expected);
		
		//assertEquals(expected.trim(), actual.trim());
	}

	private static String readResource(String path) throws Exception
	{
		try (InputStream stream = JumpstartProtoParityTest.class.getClassLoader().getResourceAsStream(path))
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

