// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.protobuf.compiler;

import com.google.protobuf.Descriptors.FileDescriptor;

/**
 * An interface for generating Java code for a .proto file.
 */
public interface JavaCodeGenerator {
  /**
   * Generates Java code for the given file.
   *
   * @param file The file to generate code for.
   * @param parameter The generator parameter.
   * @param generatorContext The generator context.
   * @throws GenerationException If an error occurred during code generation.
   */
  void generate(FileDescriptor file, String parameter, GeneratorContext generatorContext)
      throws CodeGenerator.GenerationException;
}
