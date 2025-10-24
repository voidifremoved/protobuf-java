// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.protobuf.compiler;

/**
 * A factory for creating {@link JavaCodeGenerator} instances.
 */
public final class GeneratorFactory {
  /**
   * Creates a new {@link JavaCodeGenerator} instance.
   *
   * @return A new {@link JavaCodeGenerator} instance.
   */
  public static JavaCodeGenerator create() {
    return new JavaCodeGeneratorImpl();
  }
}
