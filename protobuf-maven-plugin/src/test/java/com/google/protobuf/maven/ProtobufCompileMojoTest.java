package com.google.protobuf.maven;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Collections;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.rubberjam.protobuf.maven.ProtobufCompileMojo;

public class ProtobufCompileMojoTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private ProtobufCompileMojo mojo;
  private MavenProject project;
  private File sourceDir;
  private File outputDir;

  @Before
  public void setUp() throws Exception {
    mojo = new ProtobufCompileMojo();
    project = mock(MavenProject.class);

    sourceDir = tempFolder.newFolder("src", "main", "proto");
    outputDir = tempFolder.newFolder("target", "generated-sources", "protobuf");

    setField(mojo, "project", project);
    setField(mojo, "sourceDirectory", sourceDir);
    setField(mojo, "outputDirectory", outputDir);

    when(project.getCompileClasspathElements()).thenReturn(Collections.emptyList());
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  @Test
  public void testExecute() throws Exception {
    // Create a dummy proto file
    File protoFile = new File(sourceDir, "simple.proto");
    try (FileWriter writer = new FileWriter(protoFile)) {
      writer.write("syntax = \"proto3\";\npackage com.example;\nmessage SimpleMessage { string name = 1; }");
    }

    mojo.execute();

    // simple.proto -> SimpleProto (Outer Class) - verified by test run
    // message SimpleMessage (Inner Class)

    File generatedFile = new File(outputDir, "com/example/SimpleProto.java");

    assertTrue("Generated file should exist: " + generatedFile.getAbsolutePath(), generatedFile.exists());
    String content = new String(Files.readAllBytes(generatedFile.toPath()));
    assertTrue(content.contains("public final class SimpleProto"));
    assertTrue(content.contains("SimpleMessage"));
  }
}
