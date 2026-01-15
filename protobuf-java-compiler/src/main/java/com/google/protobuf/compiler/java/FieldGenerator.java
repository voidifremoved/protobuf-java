package com.google.protobuf.compiler.java;

import java.io.PrintWriter;

public abstract class FieldGenerator {
  public abstract void generateSerializationCode(PrintWriter printer);
}
