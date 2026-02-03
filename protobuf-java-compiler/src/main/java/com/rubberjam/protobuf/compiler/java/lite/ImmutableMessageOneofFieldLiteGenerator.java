package com.rubberjam.protobuf.compiler.java.lite;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.DocComment;
import com.rubberjam.protobuf.compiler.java.FieldCommon;
import com.rubberjam.protobuf.compiler.java.Helpers;
import com.rubberjam.protobuf.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.io.Printer;
import java.util.List;

public class ImmutableMessageOneofFieldLiteGenerator extends ImmutableMessageFieldLiteGenerator {
  public ImmutableMessageOneofFieldLiteGenerator(
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
      // printer.annotate("{", "}", descriptor);
    }

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public $type$ ${$get$capitalized_name$$}$() {\n" +
        "  if ($has_oneof_case_message$) {\n" +
        "     return ($type$) $oneof_name$_;\n" +
        "  }\n" +
        "  return $type$.getDefaultInstance();\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void set$capitalized_name$($type$ value) {\n" +
        "$null_check$" +
        "  $oneof_name$_ = value;\n" +
        "  $set_oneof_case_message$;\n" +
        "}\n");

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), false, false, true);
    printer.emit(variables,
        "private void merge$capitalized_name$($type$ value) {\n" +
        "$null_check$" +
        "  if ($has_oneof_case_message$ &&\n" +
        "      $oneof_name$_ != $type$.getDefaultInstance()) {\n" +
        "    $oneof_name$_ = $type$.newBuilder(($type$) $oneof_name$_)\n" +
        "        .mergeFrom(value).buildPartial();\n" +
        "  } else {\n" +
        "    $oneof_name$_ = value;\n" +
        "  }\n" +
        "  $set_oneof_case_message$;\n" +
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
      // printer.annotate("{", "}", descriptor);
    }

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.GETTER, context.getOptions());
    printer.emit(variables,
        "@java.lang.Override\n" +
        "$deprecation$public $type$ ${$get$capitalized_name$$}$() {\n" +
        "  return instance.get$capitalized_name$();\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$set$capitalized_name$$}$($type$ value) {\n" +
        "  copyOnWrite();\n" +
        "  instance.set$capitalized_name$(value);\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$set$capitalized_name$$}$(\n" +
        "    $type$.Builder builderForValue) {\n" +
        "  copyOnWrite();\n" +
        "  instance.set$capitalized_name$(builderForValue.build());\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.SETTER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$merge$capitalized_name$$}$($type$ value) {\n" +
        "  copyOnWrite();\n" +
        "  instance.merge$capitalized_name$(value);\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);

    DocComment.writeFieldAccessorDocComment(printer, descriptor, DocComment.AccessorType.CLEARER,
        context.getOptions(), true);
    printer.emit(variables,
        "$deprecation$public Builder ${$clear$capitalized_name$$}$() {\n" +
        "  copyOnWrite();\n" +
        "  instance.clear$capitalized_name$();\n" +
        "  return this;\n" +
        "}\n");
    // printer.annotate("{", "}", descriptor);
  }

  @Override
  public void generateFieldInfo(Printer printer, List<Integer> output) {
    Helpers.writeIntToUtf16CharSequence(descriptor.getNumber(), output);
    Helpers.writeIntToUtf16CharSequence(InternalHelpers.getExperimentalJavaFieldType(descriptor), output);
    Helpers.writeIntToUtf16CharSequence(descriptor.getContainingOneof().getIndex(), output);
  }
}
