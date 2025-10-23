// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.protobuf.compiler.java;

import java.io.PrintWriter;

/**
 * A placeholder for the MessageGenerator class.
 */
public final class MessageGenerator {
    public void generate(PrintWriter out) {}
    public void generateInterface(PrintWriter out) {}
    public void generateExtensionRegistrationCode(PrintWriter out) {}
    public void generateStaticVariables(PrintWriter out, int[] bytecodeEstimate) {}
    public int generateStaticVariableInitializers(PrintWriter out) { return 0; }
}
