package com.rubberjam.protobuf.another.compiler.java.lite;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.another.compiler.java.Context;
import com.rubberjam.protobuf.another.compiler.java.DocComment;
import com.rubberjam.protobuf.another.compiler.java.FieldCommon;
import com.rubberjam.protobuf.another.compiler.java.Helpers;
import com.rubberjam.protobuf.another.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.io.Printer;
import java.util.List;

public class ImmutableStringOneofFieldLiteGenerator extends ImmutableStringFieldLiteGenerator {
  public ImmutableStringOneofFieldLiteGenerator(
      FieldDescriptor descriptor, int messageBitIndex, Context context) {
    super(descriptor, messageBitIndex, context);
    FieldCommon.OneofGeneratorInfo info = context.getOneofGeneratorInfo(descriptor.getContainingOneof());
    FieldCommon.setCommonOneofVariables(descriptor, info, variables);
  }

  @Override
  public void generateMembers(Printer printer) {
    if (descriptor.hasPresence()) {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER, context.getOptions());
      printer.emit(variables,
          "@java.lang.Override\n" +
          "$deprecation$public boolean ${$has$capitalized_name$$}$() {\n" +
          "  return $has_oneof_case_message$;\n" +
          "}\n");
      printer.annotate("{", "}", descriptor);
    }

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public java.lang.String ${$get$capitalized_name$$}$() {\n" +
        "  java.lang.String ref = $default$;\n" +
        "  if ($has_oneof_case_message$) {\n" +
        "    ref = (java.lang.String) $oneof_name$_;\n" +
        "  }\n" +
        "  return ref;\n" +
        "}\n");
    printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public com.google.protobuf.ByteString\n" +
        "    ${$get$capitalized_name$Bytes$}$() {\n" +
        "  java.lang.String ref = $default$;\n" +
        "  if ($has_oneof_case_message$) {\n" +
        "    ref = (java.lang.String) $oneof_name$_;\n" +
        "  }\n" +
        "  return com.google.protobuf.ByteString.copyFromUtf8(ref);\n" +
        "}\n");
    printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void set$capitalized_name$(\n" +
        "    java.lang.String value) {\n" +
        "$null_check$" +
        "  $set_oneof_case_message$;\n" +
        "  $oneof_name$_ = value;\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void clear$capitalized_name$() {\n" +
        "  if ($has_oneof_case_message$) {\n" +
        "    $clear_oneof_case_message$;\n" +
        "    $oneof_name$_ = null;\n" +
        "  }\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void set$capitalized_name$Bytes(\n" +
        "    com.google.protobuf.ByteString value) {\n" +
        "  checkByteStringIsUtf8(value);\n" +
        "  $oneof_name$_ = value.toStringUtf8();\n" +
        "  $set_oneof_case_message$;\n" +
        "}\n");
  }

  @Override
  public void generateBuilderMembers(Printer printer) {
    if (descriptor.hasPresence()) {
      DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.HAZZER, context.getOptions());
      printer.emit(variables,
          "@java.lang.Override\n" +
          "$deprecation$public boolean ${$has$capitalized_name$$}$() {\n" +
          "  return instance.has$capitalized_name$();\n" +
          "}\n");
      printer.annotate("{", "}", descriptor);
    }

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public java.lang.String ${$get$capitalized_name$$}$() {\n" +
        "  return instance.get$capitalized_name$();\n" +
        "}\n");
    printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public com.google.protobuf.ByteString\n" +
        "    ${$get$capitalized_name$Bytes$}$() {\n" +
        "  return instance.get$capitalized_name$Bytes();\n" +
        "}\n");
    printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$set$capitalized_name$$}$(\n" +
        "    java.lang.String value) {\n" +
        "  copyOnWrite();\n" +
        "  instance.set$capitalized_name$(value);\n" +
        "  return this;\n" +
        "}\n");
    printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$clear$capitalized_name$$}$() {\n" +
        "  copyOnWrite();\n" +
        "  instance.clear$capitalized_name$();\n" +
        "  return this;\n" +
        "}\n");
    printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$set$capitalized_name$Bytes$}$(\n" +
        "    com.google.protobuf.ByteString value) {\n" +
        "  copyOnWrite();\n" +
        "  instance.set$capitalized_name$Bytes(value);\n" +
        "  return this;\n" +
        "}\n");
    printer.annotate("{", "}", descriptor);
  }

  @Override
  public void generateFieldInfo(Printer printer, List<Integer> output) {
    Helpers.writeIntToUtf16CharSequence(descriptor.getNumber(), output);
    Helpers.writeIntToUtf16CharSequence(InternalHelpers.getExperimentalJavaFieldType(descriptor), output);
    Helpers.writeIntToUtf16CharSequence(descriptor.getContainingOneof().getIndex(), output);
  }
}
