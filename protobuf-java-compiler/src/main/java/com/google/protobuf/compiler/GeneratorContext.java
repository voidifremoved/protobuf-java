// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.protobuf.compiler;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.FileDescriptor;
import java.io.OutputStream;
import java.util.List;

/**
 * The abstract interface to a class which generates code implementing a
 * particular proto file in a particular language.  A number of these may
 * be registered with CommandLineInterface to support various languages.
 */
public interface GeneratorContext {
  /**
   * Opens the given file, truncating it if it exists, and returns a
   * OutputStream that writes to the file.  The caller takes ownership
   * of the returned object.
   */
  OutputStream open(String filename) throws java.io.IOException;
}
