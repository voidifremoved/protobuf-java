package com.rubberjam.protobuf.another.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SubprocessTest
{
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testCommunicateEcho() throws IOException
  {
    // 'cat' is usually available. If not, this test might fail on Windows without tools.
    // But we assume Linux-like environment.
    Subprocess subprocess = new Subprocess();
    subprocess.start("cat", Subprocess.SearchMode.SEARCH_PATH);

    CodeGeneratorRequest input = CodeGeneratorRequest.newBuilder()
        .setParameter("test_param")
        .build();
    CodeGeneratorRequest.Builder output = CodeGeneratorRequest.newBuilder();
    StringBuilder error = new StringBuilder();

    boolean result = subprocess.communicate(input, output, error);

    assertTrue("Communicate should succeed", result);
    assertEquals("", error.toString());
    assertEquals("test_param", output.build().getParameter());
  }

  @Test
  public void testCommunicateError() throws IOException
  {
    // Create a script that exits with non-zero
    File script = tempFolder.newFile("fail.sh");
    try (FileWriter fw = new FileWriter(script)) {
      fw.write("#!/bin/sh\n");
      fw.write("echo 'some error' >&2\n");
      fw.write("exit 1\n");
    }
    script.setExecutable(true);

    Subprocess subprocess = new Subprocess();
    subprocess.start(script.getAbsolutePath(), Subprocess.SearchMode.EXACT_NAME);

    CodeGeneratorRequest input = CodeGeneratorRequest.getDefaultInstance();
    CodeGeneratorRequest.Builder output = CodeGeneratorRequest.newBuilder();
    StringBuilder error = new StringBuilder();

    boolean result = subprocess.communicate(input, output, error);

    assertFalse("Communicate should fail", result);
    assertTrue(error.toString().contains("some error"));
    assertTrue(error.toString().contains("status code 1"));
  }
}
