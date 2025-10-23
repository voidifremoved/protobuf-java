// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.protobuf.compiler;

import com.google.protobuf.Descriptors.FileDescriptor;
import java.util.List;

/**
 * The abstract interface to a class which generates code implementing a
 * particular proto file in a particular language.  A number of these may
 * be registered with CommandLineInterface to support various languages.
 */
public abstract class CodeGenerator {
  /**
   * Generates code for the given proto file, generating one or more files in
   * the given output directory.
   *
   * @param file The file to generate code for.
   * @param parameter A parameter to be passed to the generator.
   * @param generatorContext The context in which to generate the code.
   * @throws GenerationException if an error occurred during generation.
   */
  public abstract void generate(
      FileDescriptor file, String parameter, GeneratorContext generatorContext)
      throws GenerationException;

  /**
   * Generates code for all given proto files.
   *
   * @param files The files to generate code for.
   * @param parameter A parameter to be passed to the generator.
   * @param generatorContext The context in which to generate the code.
   * @throws GenerationException if an error occurred during generation.
   */
  public void generateAll(
      List<FileDescriptor> files, String parameter, GeneratorContext generatorContext)
      throws GenerationException {
    for (FileDescriptor file : files) {
      generate(file, parameter, generatorContext);
    }
  }

  /**
   * An exception that occurred during code generation.
   */
  public static class GenerationException extends Exception {
    public GenerationException(String message) {
      super(message);
    }
  }
}
