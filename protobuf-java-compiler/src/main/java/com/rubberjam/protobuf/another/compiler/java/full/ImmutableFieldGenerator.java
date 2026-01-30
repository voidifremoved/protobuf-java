package com.rubberjam.protobuf.another.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.another.compiler.java.Context;
import com.rubberjam.protobuf.another.compiler.java.FieldCommon;
import com.rubberjam.protobuf.another.compiler.java.GeneratorCommon;
import com.rubberjam.protobuf.io.Printer;
import java.util.HashMap;
import java.util.Map;

/**
 * Interface for generating a field. Ported from java/full/field_generator.h.
 */
public abstract class ImmutableFieldGenerator implements GeneratorCommon.FieldGenerator {
  protected final FieldDescriptor descriptor;
  protected final int messageBitIndex;
  protected final int builderBitIndex;
  protected final Context context;
  protected final Map<String, Object> variables;

  public ImmutableFieldGenerator(
      FieldDescriptor descriptor, int messageBitIndex, int builderBitIndex, Context context) {
    this.descriptor = descriptor;
    this.messageBitIndex = messageBitIndex;
    this.builderBitIndex = builderBitIndex;
    this.context = context;
    this.variables = new HashMap<>();
    FieldCommon.setCommonFieldVariables(
        descriptor, context.getFieldGeneratorInfo(descriptor), variables);
  }

  public int getMessageBitIndex() {
    return messageBitIndex;
  }

  public abstract int getNumBitsForMessage();

  public abstract int getNumBitsForBuilder();

  public abstract void generateInterfaceMembers(Printer printer);

  public abstract void generateMembers(Printer printer);

  public abstract void generateBuilderMembers(Printer printer);

  public abstract void generateInitializationCode(Printer printer);

  public abstract void generateBuilderClearCode(Printer printer);

  public abstract void generateMergingCode(Printer printer);

  public abstract void generateBuildingCode(Printer printer);

  public abstract void generateParsingCode(Printer printer);

  public abstract void generateParsingCodeFromPacked(Printer printer);

  public abstract void generateParsingDoneCode(Printer printer);

  public abstract void generateSerializedSizeCode(Printer printer);

  public abstract void generateEqualsCode(Printer printer);

  public abstract void generateHashCodeCode(Printer printer);

  // Added missing abstract methods that were called
  public void generateFieldBuilderInitializationCode(Printer printer) {
    // Default implementation does nothing
  }

  public void generateBuilderParsingCode(Printer printer) {
    generateParsingCode(printer); // Default to standard parsing code
  }

  public void generateBuilderParsingCodeFromPacked(Printer printer) {
    generateParsingCodeFromPacked(printer); // Default to standard parsing code
  }
}
