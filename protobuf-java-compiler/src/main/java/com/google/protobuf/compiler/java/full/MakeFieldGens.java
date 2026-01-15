package com.google.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.compiler.java.Context;
import com.google.protobuf.compiler.java.FieldGeneratorMap;
import com.google.protobuf.compiler.java.JavaType;
import com.google.protobuf.compiler.java.StringUtils;

public final class MakeFieldGens {

  public static FieldGeneratorMap<ImmutableFieldGenerator> makeImmutableFieldGenerators(
      Descriptor descriptor, Context context) {
    FieldGeneratorMap<ImmutableFieldGenerator> ret = new FieldGeneratorMap<>(descriptor);
    int messageBitIndex = 0;
    int builderBitIndex = 0;

    for (FieldDescriptor field : descriptor.getFields()) {
      ImmutableFieldGenerator generator = makeImmutableGenerator(field, messageBitIndex, builderBitIndex, context);
      messageBitIndex += generator.getNumBitsForMessage();
      builderBitIndex += generator.getNumBitsForBuilder();
      ret.add(field, generator);
    }
    return ret;
  }

  private static ImmutableFieldGenerator makeImmutableGenerator(
      FieldDescriptor field, int messageBitIndex, int builderBitIndex, Context context) {
    if (field.isRepeated()) {
      // TODO: Implement Repeated Field Generators
      // For now, return a PrimitiveFieldGenerator as a fallback to avoid crash, but this is WRONG for repeated
       JavaType javaType = StringUtils.getJavaType(field);
       if (javaType == JavaType.STRING) {
           return new StringFieldGenerator(field, messageBitIndex, builderBitIndex, context); // Should be RepeatedStringFieldGenerator
       }
       return new PrimitiveFieldGenerator(field, messageBitIndex, builderBitIndex, context); // Should be RepeatedPrimitiveFieldGenerator
    } else {
      if (field.getContainingOneof() != null) {
          // TODO: Oneof support
          JavaType javaType = StringUtils.getJavaType(field);
          if (javaType == JavaType.STRING) {
              return new StringFieldGenerator(field, messageBitIndex, builderBitIndex, context); // Should be StringOneof
          }
           return new PrimitiveFieldGenerator(field, messageBitIndex, builderBitIndex, context); // Should be PrimitiveOneof
      } else {
        JavaType javaType = StringUtils.getJavaType(field);
        switch (javaType) {
          case MESSAGE:
             // TODO: MessageFieldGenerator
             throw new UnsupportedOperationException("Message field generation not implemented yet");
          case ENUM:
             // TODO: EnumFieldGenerator
             return new PrimitiveFieldGenerator(field, messageBitIndex, builderBitIndex, context); // Stub
          case STRING:
             return new StringFieldGenerator(field, messageBitIndex, builderBitIndex, context);
          default:
             return new PrimitiveFieldGenerator(field, messageBitIndex, builderBitIndex, context);
        }
      }
    }
  }
}
