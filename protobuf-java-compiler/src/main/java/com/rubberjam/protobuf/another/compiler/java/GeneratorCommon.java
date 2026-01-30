package com.rubberjam.protobuf.another.compiler.java;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.io.Printer;

/**
 * Common generator infrastructure. Ported from generator_common.h.
 */
public final class GeneratorCommon {

  private GeneratorCommon() {}

  public static final int kMaxStaticSize = 1 << 15; // aka 32k

  public interface FieldGenerator {
    void generateSerializationCode(Printer printer);
  }

  // Convenience class which constructs FieldGenerators for a Descriptor.
  public static class FieldGeneratorMap<T extends FieldGenerator> {
    private final Descriptor descriptor;
    private final List<T> fieldGenerators;

    public FieldGeneratorMap(Descriptor descriptor) {
      this.descriptor = descriptor;
      this.fieldGenerators = new ArrayList<>(descriptor.getFields().size());
    }

    public void add(FieldDescriptor field, T fieldGenerator) {
      if (field.getContainingType() != descriptor) {
        throw new IllegalArgumentException("Field does not belong to the descriptor.");
      }
      fieldGenerators.add(fieldGenerator);
    }

    public T get(FieldDescriptor field) {
      if (field.getContainingType() != descriptor) {
        throw new IllegalArgumentException("Field does not belong to the descriptor.");
      }
      return fieldGenerators.get(field.getIndex());
    }

    public List<T> getFieldGenerators() {
      return new ArrayList<>(fieldGenerators);
    }
  }

  public static void reportUnexpectedPackedFieldsCall() {
    // Reaching here indicates a bug.
    throw new RuntimeException("GenerateBuilderParsingCodeFromPacked() called on field generator that does not support packing.");
  }
}
