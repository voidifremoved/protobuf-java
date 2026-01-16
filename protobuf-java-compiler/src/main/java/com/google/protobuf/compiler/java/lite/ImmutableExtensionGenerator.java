package com.google.protobuf.compiler.java.lite;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.compiler.java.Context;
import com.google.protobuf.compiler.java.ExtensionGenerator;
import java.io.PrintWriter;

public class ImmutableExtensionGenerator extends ExtensionGenerator {
  public ImmutableExtensionGenerator(FieldDescriptor descriptor, Context context) {
  }

  @Override
  public void generate(PrintWriter printer) {
      // Stub
  }

  @Override
  public int generateNonNestedInitializationCode(PrintWriter printer) {
      return 0;
  }

  @Override
  public int generateRegistrationCode(PrintWriter printer) {
      return 0;
  }
}
