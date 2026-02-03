package com.rubberjam.protobuf.compiler.java.lite;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.compiler.java.GeneratorCommon.FieldGeneratorMap;

public final class ImmutableFieldLiteGeneratorFactory {

  private ImmutableFieldLiteGeneratorFactory() {}

  public static FieldGeneratorMap<ImmutableFieldLiteGenerator> createFieldGenerators(
      Descriptor descriptor, Context context) {
    FieldGeneratorMap<ImmutableFieldLiteGenerator> fieldGenerators =
        new FieldGeneratorMap<>(descriptor);

    int messageBitIndex = 0;

    for (FieldDescriptor field : descriptor.getFields()) {
      ImmutableFieldLiteGenerator generator;
      if (field.isRepeated()) {
        if (field.isMapField()) {
          generator = new ImmutableMapFieldLiteGenerator(field, messageBitIndex, context);
        } else {
          if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
            generator =
                new RepeatedImmutableMessageFieldLiteGenerator(field, messageBitIndex, context);
          } else if (field.getJavaType() == FieldDescriptor.JavaType.ENUM) {
            generator =
                new RepeatedImmutableEnumFieldLiteGenerator(field, messageBitIndex, context);
          } else if (field.getJavaType() == FieldDescriptor.JavaType.STRING ||
                     field.getJavaType() == FieldDescriptor.JavaType.BYTE_STRING) {
            generator =
                new RepeatedImmutableStringFieldLiteGenerator(field, messageBitIndex, context);
          } else {
            generator =
                new RepeatedImmutablePrimitiveFieldLiteGenerator(
                    field, messageBitIndex, context);
          }
        }
      } else {
        if (field.getContainingOneof() != null) {
          if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
            generator =
                new ImmutableMessageOneofFieldLiteGenerator(field, messageBitIndex, context);
          } else if (field.getJavaType() == FieldDescriptor.JavaType.ENUM) {
            generator = new ImmutableEnumOneofFieldLiteGenerator(field, messageBitIndex, context);
          } else if (field.getJavaType() == FieldDescriptor.JavaType.STRING ||
                     field.getJavaType() == FieldDescriptor.JavaType.BYTE_STRING) {
            generator = new ImmutableStringOneofFieldLiteGenerator(field, messageBitIndex, context);
          } else {
             generator =
                new ImmutablePrimitiveOneofFieldLiteGenerator(field, messageBitIndex, context);
          }
        } else {
          if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
            generator =
                new ImmutableMessageFieldLiteGenerator(field, messageBitIndex, context);
          } else if (field.getJavaType() == FieldDescriptor.JavaType.ENUM) {
            generator = new ImmutableEnumFieldLiteGenerator(field, messageBitIndex, context);
          } else if (field.getJavaType() == FieldDescriptor.JavaType.STRING ||
                     field.getJavaType() == FieldDescriptor.JavaType.BYTE_STRING) {
            generator = new ImmutableStringFieldLiteGenerator(field, messageBitIndex, context);
          } else {
            generator =
                new ImmutablePrimitiveFieldLiteGenerator(field, messageBitIndex, context);
          }
        }
      }

      fieldGenerators.add(field, generator);

      // Increment bit indices
      messageBitIndex += generator.getNumBitsForMessage();
    }

    return fieldGenerators;
  }
}
