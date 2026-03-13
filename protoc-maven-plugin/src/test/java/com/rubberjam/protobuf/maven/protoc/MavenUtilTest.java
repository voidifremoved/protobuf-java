package com.rubberjam.protobuf.maven.protoc;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MavenUtilTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test(expected = IOException.class)
	public void testParseMavenSettingsWithXXE() throws IOException {
		File xxeFile = tempFolder.newFile("settings.xml");
		try (FileWriter writer = new FileWriter(xxeFile)) {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			             "<!DOCTYPE settings [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>\n" +
			             "<settings>\n" +
			             "  <mirrors>\n" +
			             "    <mirror>\n" +
			             "      <id>xxe</id>\n" +
			             "      <url>http://example.com/&xxe;</url>\n" +
			             "      <mirrorOf>central</mirrorOf>\n" +
			             "    </mirror>\n" +
			             "  </mirrors>\n" +
			             "</settings>");
		}

		MavenUtil.parseMavenSettings(xxeFile);
	}
}
