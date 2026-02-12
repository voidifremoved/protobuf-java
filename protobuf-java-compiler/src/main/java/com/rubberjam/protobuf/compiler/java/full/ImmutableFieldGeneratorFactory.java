package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.compiler.java.GeneratorCommon.FieldGeneratorMap;

/**
 * Factory for creating immutable field generators. Ported from java/full/make_field_gens.cc.
 */
public final class ImmutableFieldGeneratorFactory {

  private ImmutableFieldGeneratorFactory() {}

  public static FieldGeneratorMap<ImmutableFieldGenerator> createFieldGenerators(
      Descriptor descriptor, Context context) {
    FieldGeneratorMap<ImmutableFieldGenerator> fieldGenerators =
        new FieldGeneratorMap<>(descriptor);

    int messageBitIndex = 0;
    int builderBitIndex = 0;

    for (FieldDescriptor field : descriptor.getFields()) {
      ImmutableFieldGenerator generator;
      if (field.isRepeated()) {
        if (field.isMapField()) {
          generator = new MapFieldGenerator(field, messageBitIndex, builderBitIndex, context);
        } else {
          if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
            generator =
                new RepeatedMessageFieldGenerator(field, messageBitIndex, builderBitIndex, context);
          } else if (field.getJavaType() == FieldDescriptor.JavaType.ENUM) {
            generator =
                new RepeatedEnumFieldGenerator(field, messageBitIndex, builderBitIndex, context);
          } else if (field.getJavaType() == FieldDescriptor.JavaType.STRING ||
                     field.getJavaType() == FieldDescriptor.JavaType.BYTE_STRING) {
            generator =
                new RepeatedStringFieldGenerator(field, messageBitIndex, builderBitIndex, context);
          } else {
            generator =
                new RepeatedPrimitiveFieldGenerator(
                    field, messageBitIndex, builderBitIndex, context);
          }
        }
      } else {
        if (Helpers.isRealOneof(field)) {
          if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
            generator =
                new MessageFieldGenerator(field, messageBitIndex, builderBitIndex, context);
          } else if (field.getJavaType() == FieldDescriptor.JavaType.ENUM) {
            generator = new EnumFieldGenerator(field, messageBitIndex, builderBitIndex, context);
          } else if (field.getJavaType() == FieldDescriptor.JavaType.STRING ||
                     field.getJavaType() == FieldDescriptor.JavaType.BYTE_STRING) {
            generator = new StringFieldGenerator(field, messageBitIndex, builderBitIndex, context);
          } else {
            generator =
                new PrimitiveFieldGenerator(field, messageBitIndex, builderBitIndex, context);
          }
        } else {
          if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
            generator =
                new MessageFieldGenerator(field, messageBitIndex, builderBitIndex, context);
          } else if (field.getJavaType() == FieldDescriptor.JavaType.ENUM) {
            generator = new EnumFieldGenerator(field, messageBitIndex, builderBitIndex, context);
          } else if (field.getJavaType() == FieldDescriptor.JavaType.STRING ||
                     field.getJavaType() == FieldDescriptor.JavaType.BYTE_STRING) {
            generator = new StringFieldGenerator(field, messageBitIndex, builderBitIndex, context);
          } else {
            generator =
                new PrimitiveFieldGenerator(field, messageBitIndex, builderBitIndex, context);
          }
        }
      }

      fieldGenerators.add(field, generator);

      // Increment bit indices
      messageBitIndex += generator.getNumBitsForMessage();
      builderBitIndex++;
    }

    return fieldGenerators;
  }
}
