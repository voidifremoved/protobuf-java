// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.rubberjam.protobuf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.rubberjam.protobuf.compiler.CommandLineInterface;

/** Unit tests for {@link CommandLineInterface}. */
public class CommandLineInterfaceTest {
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
    CommandLineInterface.run(new String[] {});
    Assert.assertTrue(outContent.toString().startsWith("Usage: protoc"));
  }

  @Test
  public void testMissingOutput() {
    CommandLineInterface.run(new String[] {"test.proto"});
    Assert.assertEquals("Missing output directive.\n", errContent.toString());
  }

  @Test
  public void testMissingInput() {
    CommandLineInterface.run(new String[] {"--java_out=."});
    Assert.assertEquals("Missing input file.\n", errContent.toString());
  }

  @Test
  public void testSimpleProto() throws Exception {
    File protoFile = new File("test.proto");
    try {
      java.io.FileWriter writer = new java.io.FileWriter(protoFile);
      writer.write("message Test {}");
      writer.close();

      String outDir = "build/test/gen";
      new File(outDir).mkdirs();

      CommandLineInterface.run(new String[] {"--java_out=" + outDir, "test.proto"});

      File outputFile = new File(outDir, "Test.java");
      Assert.assertTrue(outputFile.exists());
    } finally {
      protoFile.delete();
    }
  }
}
