// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.protobuf.compiler;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FileDescriptor;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** A command-line interface for the protocol buffer compiler. */
public final class CommandLineInterface {
  public static void main(String[] args) {
    System.exit(run(args));
  }

  /**
   * Runs the command-line interface.
   *
   * @param args The command-line arguments.
   * @return The exit code.
   */
  public static int run(String[] args) {
    if (args.length == 0) {
      printHelp();
      return 0;
    }

    String javaOut = null;
    List<String> protoFiles = new ArrayList<>();
    String parameter = "";

    for (String arg : args) {
      if (arg.startsWith("--java_out=")) {
        javaOut = arg.substring("--java_out=".length());
      } else if (arg.startsWith("--")) {
        parameter += arg.substring(2) + ",";
      } else {
        protoFiles.add(arg);
      }
    }

    if (javaOut == null) {
      System.err.println("Missing output directive.");
      return 1;
    }

    if (protoFiles.isEmpty()) {
      System.err.println("Missing input file.");
      return 1;
    }

    for (String protoFile : protoFiles) {
      try {
        if (!processProtoFile(protoFile, javaOut, parameter)) {
          return 1;
        }
      } catch (IOException e) {
        System.err.println("Error processing file " + protoFile + ": " + e.getMessage());
        return 1;
      }
    }

    return 0;
  }

  private static boolean processProtoFile(String protoFile, String javaOut, String parameter)
      throws IOException {
    File file = new File(protoFile);
    if (!file.exists()) {
      System.err.println(protoFile + ": No such file or directory.");
      return false;
    }

    FileDescriptorProto.Builder fileBuilder = FileDescriptorProto.newBuilder();
    fileBuilder.setName(protoFile);

    ErrorCollector errorCollector =
        (line, column, message) ->
            System.err.println(protoFile + ":" + line + ":" + column + ": " + message);

    try (Reader reader = new FileReader(file)) {
      Tokenizer tokenizer = new Tokenizer(reader, errorCollector);
      Parser parser = new Parser(errorCollector, new SourceLocationTable());
      if (!parser.parse(tokenizer, fileBuilder)) {
        System.err.println("Error parsing " + protoFile);
        return false;
      }
    }

    try {
      FileDescriptor fileDescriptor =
          FileDescriptor.buildFrom(fileBuilder.build(), new FileDescriptor[0]);
      JavaCodeGenerator codeGenerator = GeneratorFactory.create();
      GeneratorContextImpl generatorContext = new GeneratorContextImpl(javaOut);
      codeGenerator.generate(fileDescriptor, parameter, generatorContext);
    } catch (Descriptors.DescriptorValidationException | CodeGenerator.GenerationException e) {
      System.err.println("Error processing " + protoFile + ": " + e.getMessage());
      return false;
    }

    return true;
  }

  private static void printHelp() {
    System.out.println("Usage: protoc [OPTION] PROTO_FILES");
    System.out.println("Parse PROTO_FILES and generate output based on the options given:");
    System.out.println("  --java_out=OUT_DIR      Generate Java source files.");
  }
}
