package com.google.protobuf.compiler.java.lite;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.compiler.java.Context;
import java.io.PrintWriter;

public class MessageFieldGenerator extends ImmutableFieldGenerator {
  private final FieldDescriptor descriptor;
  private final Context context;

  public MessageFieldGenerator(FieldDescriptor descriptor, Context context) {
    this.descriptor = descriptor;
    this.context = context;
  }

  @Override
  public void generateMembers(PrintWriter printer) {
     // Stub
  }

  @Override
  public void generateBuilderMembers(PrintWriter printer) {
     // Stub
  }

  @Override
  public void generateInitializationCode(PrintWriter printer) {
     // Stub
  }
}
