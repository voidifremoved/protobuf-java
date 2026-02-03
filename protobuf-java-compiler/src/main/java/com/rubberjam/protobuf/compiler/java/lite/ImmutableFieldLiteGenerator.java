package com.rubberjam.protobuf.compiler.java.lite;

import com.rubberjam.protobuf.compiler.java.GeneratorCommon;
import com.rubberjam.protobuf.io.Printer;
import java.util.List;

public interface ImmutableFieldLiteGenerator extends GeneratorCommon.FieldGenerator {
  int getNumBitsForMessage();
  void generateInterfaceMembers(Printer printer);
  void generateMembers(Printer printer);
  void generateBuilderMembers(Printer printer);
  void generateInitializationCode(Printer printer);
  void generateFieldInfo(Printer printer, List<Integer> output);
  String getBoxedType();

  @Override
  default void generateSerializationCode(Printer printer) {
    // No-op for Lite as it uses table-driven serialization via MessageInfo.
  }
}
