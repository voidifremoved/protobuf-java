package com.google.protobuf.compiler.java;

import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;

public class InternalHelpers {

  public static boolean supportUnknownEnumValue(FieldDescriptor field) {
    // For now simplified logic assuming proto3 semantics or similar
    return field.getEnumType() != null && field.getFile().getSyntax() == FileDescriptor.Syntax.PROTO3;
  }

  public static boolean checkUtf8(FieldDescriptor descriptor) {
    // Simplified logic
    return descriptor.getType() == FieldDescriptor.Type.STRING;
  }
}
