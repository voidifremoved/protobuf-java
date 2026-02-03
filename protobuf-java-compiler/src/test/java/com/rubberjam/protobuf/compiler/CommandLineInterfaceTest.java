package com.rubberjam.protobuf.compiler;

import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.compiler.CodeGenerator;
import com.rubberjam.protobuf.compiler.CommandLineInterface;
import com.rubberjam.protobuf.compiler.GeneratorContext;
import com.rubberjam.protobuf.compiler.CodeGenerator.GenerationException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CommandLineInterfaceTest {
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

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
  public void testHelp() {
    CommandLineInterface cli = new CommandLineInterface();
    cli.run(new String[] {"--help"});
    Assert.assertTrue(outContent.toString().startsWith("Usage: protoc"));
  }

  @Test
  public void testVersion() {
    CommandLineInterface cli = new CommandLineInterface();
    cli.run(new String[] {"--version"});
    Assert.assertTrue(outContent.toString().startsWith("libprotoc"));
  }

  @Test
  public void testMissingOutput() {
    CommandLineInterface cli = new CommandLineInterface();
    cli.run(new String[] {"test.proto"});
    // Note: The CLI implementation prints to stderr.
    // Asserting exact string might be brittle due to line endings.
    Assert.assertTrue(errContent.toString().trim().contains("Missing output directive"));
  }

  @Test
  public void testMissingInput() {
    CommandLineInterface cli = new CommandLineInterface();
    cli.run(new String[] {"--java_out=."});
    Assert.assertTrue(errContent.toString().trim().contains("Missing input file"));
  }

  @Test
  public void testMockGenerator() throws Exception {
    File protoFile = tempFolder.newFile("test.proto");
    java.io.FileWriter writer = new java.io.FileWriter(protoFile);
    writer.write("syntax = \"proto2\";\nmessage Test {}");
    writer.close();

    File outDir = tempFolder.newFolder("out");

    CommandLineInterface cli = new CommandLineInterface();
    MockCodeGenerator mockGen = new MockCodeGenerator();
    cli.registerGenerator("test", mockGen, "Test generator");

    int exitCode = cli.run(new String[] {
        "--test_out=" + outDir.getAbsolutePath(),
        "--proto_path=" + tempFolder.getRoot().getAbsolutePath(),
        "test.proto"
    });

    if (exitCode != 0) {
        System.err.println("CLI failed. Stderr: " + errContent.toString());
    }

    Assert.assertEquals(0, exitCode);
    Assert.assertTrue("Generator should be called", mockGen.wasCalled);
    Assert.assertNotNull(mockGen.generatedFile);
    Assert.assertEquals("test.proto", mockGen.generatedFile.getName());

    File generated = new File(outDir, "test.proto.txt");
    Assert.assertTrue("Generated file should exist", generated.exists());
  }

  private static class MockCodeGenerator extends CodeGenerator {
    boolean wasCalled = false;
    FileDescriptor generatedFile;

    @Override
    public void generate(FileDescriptor file, String parameter, GeneratorContext generatorContext)
        throws GenerationException {
       wasCalled = true;
       generatedFile = file;
       try {
           java.io.OutputStream out = generatorContext.open(file.getName() + ".txt");
           out.write("generated".getBytes());
           out.close();
       } catch (IOException e) {
           throw new GenerationException(e);
       }
    }
  }
}
