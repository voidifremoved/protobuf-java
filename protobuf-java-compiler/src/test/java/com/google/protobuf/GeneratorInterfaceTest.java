package com.google.protobuf;

import com.rubberjam.protobuf.compiler.CommandLineInterface;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

public class GeneratorInterfaceTest {
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;

  @Before
  public void setUpStreams() {
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
  }

  @After
  public void restoreStreams() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @Test
  public void testCppGeneration() throws Exception {
    File protoFile = new File("test_cpp.proto");
    try {
      java.io.FileWriter writer = new java.io.FileWriter(protoFile);
      writer.write("syntax = \"proto3\";\n");
      writer.write("message TestCpp {}");
      writer.close();

      String outDir = "build/test/gen_cpp";
      new File(outDir).mkdirs();

      CommandLineInterface.run(new String[] {"--cpp_out=" + outDir, "test_cpp.proto"});

      File headerFile = new File(outDir, "test_cpp.pb.h");
      File sourceFile = new File(outDir, "test_cpp.pb.cc");

      Assert.assertTrue("Header file should exist", headerFile.exists());
      Assert.assertTrue("Source file should exist", sourceFile.exists());

      String headerContent = new String(java.nio.file.Files.readAllBytes(headerFile.toPath()));
      Assert.assertTrue("Header should contain class definition", headerContent.contains("class TestCpp"));

      String sourceContent = new String(java.nio.file.Files.readAllBytes(sourceFile.toPath()));
      Assert.assertTrue("Source should contain method stubs", sourceContent.contains("TestCpp::TestCpp()"));
    } finally {
      protoFile.delete();
    }
  }

  @Test
  public void testCSharpGeneration() throws Exception {
    File protoFile = new File("test_csharp.proto");
    try {
      java.io.FileWriter writer = new java.io.FileWriter(protoFile);
      writer.write("syntax = \"proto3\";\n");
      writer.write("message TestCSharp {}");
      writer.close();

      String outDir = "build/test/gen_csharp";
      new File(outDir).mkdirs();

      CommandLineInterface.run(new String[] {"--csharp_out=" + outDir, "test_csharp.proto"});

      File csharpFile = new File(outDir, "TestCsharp.cs");
      Assert.assertTrue("C# file should exist: " + csharpFile.getAbsolutePath(), csharpFile.exists());

      String csharpContent = new String(java.nio.file.Files.readAllBytes(csharpFile.toPath()));
      Assert.assertTrue("C# file should contain class definition", csharpContent.contains("class TestCSharp"));
    } finally {
      protoFile.delete();
    }
  }
}
