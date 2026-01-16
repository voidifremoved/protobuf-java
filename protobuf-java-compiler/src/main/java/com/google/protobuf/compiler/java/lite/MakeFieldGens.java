package com.google.protobuf.compiler.java.lite;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.compiler.java.Context;
import com.google.protobuf.compiler.java.FieldGeneratorMap;
import com.google.protobuf.compiler.java.JavaType;

public final class MakeFieldGens {
  private MakeFieldGens() {}

  public static FieldGeneratorMap<ImmutableFieldGenerator> makeImmutableFieldGenerators(
      Descriptor descriptor, Context context) {
    FieldGeneratorMap<ImmutableFieldGenerator> result =
        new FieldGeneratorMap<>(descriptor);

    for (FieldDescriptor field : descriptor.getFields()) {
      result.add(field, createFieldGenerator(field, context));
    }
    return result;
  }

  private static ImmutableFieldGenerator createFieldGenerator(FieldDescriptor field, Context context) {
      // Stub implementation
      return new ImmutableFieldGenerator() {
          @Override
          public void generateSerializationCode(java.io.PrintWriter printer) {
          }

          public void generateMembers(java.io.PrintWriter printer) {
          }
          public void generateBuilderMembers(java.io.PrintWriter printer) {
          }
          public void generateInitializationCode(java.io.PrintWriter printer) {
          }
          public void generateBuilderClearCode(java.io.PrintWriter printer) {
          }
          public void generateMergingCode(java.io.PrintWriter printer) {
          }
          public void generateBuildingCode(java.io.PrintWriter printer) {
          }
          public void generateParsingCode(java.io.PrintWriter printer) {
          }
          public void generateParsingDoneCode(java.io.PrintWriter printer) {
          }
          public void generateSerializedSizeCode(java.io.PrintWriter printer) {
          }
          public void generateFieldAccessor(java.io.PrintWriter printer) {
          }
          public void generateEqualsCode(java.io.PrintWriter printer) {
          }
          public void generateHashCode(java.io.PrintWriter printer) {
          }
          public void generateInterfaceMembers(java.io.PrintWriter printer) {
          }
      };
  }
}
