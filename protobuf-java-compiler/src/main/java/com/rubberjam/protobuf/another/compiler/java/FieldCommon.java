package com.rubberjam.protobuf.another.compiler.java;

import java.util.Map;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.io.Printer;

/**
 * Common logic for field and oneof generators.
 * Ported from field_common.cc/field_common.h.
 */
public final class FieldCommon {

  private FieldCommon() {}

  // Field information used in FieldGenerators.
  //
  public static class FieldGeneratorInfo {
    public String name;
    public String capitalizedName;
    public String disambiguatedReason;
    public Options options;

    public FieldGeneratorInfo(String name, String capitalizedName, String disambiguatedReason, Options options) {
      this.name = name;
      this.capitalizedName = capitalizedName;
      this.disambiguatedReason = disambiguatedReason;
      this.options = options;
    }
  }

  // Oneof information used in OneofFieldGenerators.
  //
  public static class OneofGeneratorInfo {
    public String name;
    public String capitalizedName;

    public OneofGeneratorInfo(String name, String capitalizedName) {
      this.name = name;
      this.capitalizedName = capitalizedName;
    }
  }

  // Set some common variables used in variable FieldGenerators.
  //
  public static void setCommonFieldVariables(
      FieldDescriptor descriptor, FieldGeneratorInfo info, Map<String, Object> variables) {
    variables.put("field_name", descriptor.getName());
    variables.put("name", info.name);
    variables.put("classname", descriptor.getContainingType().getName());
    variables.put("capitalized_name", info.capitalizedName);
    variables.put("disambiguated_reason", info.disambiguatedReason != null ? info.disambiguatedReason : "");
    variables.put("constant_name", Helpers.getFieldConstantName(descriptor));
    variables.put("number", String.valueOf(descriptor.getNumber()));
    variables.put("kt_dsl_builder", "_builder");
    
    // Placeholders for annotation identifiers
    variables.put("{", "");
    variables.put("}", "");

    // Kotlin specific variables
    variables.put("kt_name", Helpers.isForbiddenKotlin(info.name) ? info.name + "_" : info.name);
    
    String ktPropertyName = getKotlinPropertyName(info.capitalizedName);
    variables.put("kt_property_name", ktPropertyName);
    variables.put("kt_safe_name", Helpers.isForbiddenKotlin(ktPropertyName) ? "`" + ktPropertyName + "`" : ktPropertyName);
    variables.put("kt_capitalized_name", Helpers.isForbiddenKotlin(info.name) ? info.capitalizedName + "_" : info.capitalizedName);
    
    // Synthetic annotation for JVM DSL
    variables.put("jvm_synthetic", info.options.isJvmDsl() ? "@kotlin.jvm.JvmSynthetic\n" : "");

    // Annotation field type logic
    if (!descriptor.isRepeated()) {
      variables.put("annotation_field_type", Helpers.getFieldTypeName(descriptor.getType()));
    } else if (Helpers.getJavaType(descriptor) == Helpers.JavaType.MESSAGE && Helpers.isMapEntry(descriptor.getMessageType())) {
      variables.put("annotation_field_type", Helpers.getFieldTypeName(descriptor.getType()) + "MAP");
    } else {
      String listType = Helpers.getFieldTypeName(descriptor.getType()) + "_LIST";
      if (descriptor.isPacked()) {
        listType += "_PACKED";
      }
      variables.put("annotation_field_type", listType);
    }
  }

  // Locale-independent ASCII upper and lower case munging.
  private static boolean isUpper(char c) {
    return c >= 'A' && c <= 'Z';
  }

  private static char toLower(char c) {
    return isUpper(c) ? (char) (c - 'A' + 'a') : c;
  }

  // Returns the name by which the generated Java getters and setters should be
  // referenced from Kotlin as properties.
  //
  public static String getKotlinPropertyName(String capitalizedName) {
    // Find the first non-capital. If it is the second character, then we just
    // need to lowercase the first one. Otherwise we need to lowercase everything
    // up to but not including the last capital, except that if everything is
    // capitals then everything must be lowercased.
    
    StringBuilder ktPropertyName = new StringBuilder(capitalizedName);
    int firstNonCapital;
    for (firstNonCapital = 0; firstNonCapital < capitalizedName.length() && isUpper(capitalizedName.charAt(firstNonCapital)); firstNonCapital++) {
      // Loop continues until a non-capital is found or end of string
    }
    
    int stop = firstNonCapital;
    if (stop > 1 && stop < capitalizedName.length()) {
      stop--;
    }
    
    for (int i = 0; i < stop; i++) {
      ktPropertyName.setCharAt(i, toLower(ktPropertyName.charAt(i)));
    }
    
    return ktPropertyName.toString();
  }

  // Set some common oneof variables used in OneofFieldGenerators.
  //
  public static void setCommonOneofVariables(
      FieldDescriptor descriptor, OneofGeneratorInfo info, Map<String, Object> variables) {
    variables.put("oneof_name", info.name);
    variables.put("oneof_capitalized_name", info.capitalizedName);
    variables.put("oneof_index", String.valueOf(descriptor.getRealContainingOneof().getIndex()));
    variables.put("oneof_stored_type", Helpers.getOneofStoredType(descriptor));
    
    variables.put("set_oneof_case_message", info.name + "Case_ = " + descriptor.getNumber());
    variables.put("clear_oneof_case_message", info.name + "Case_ = 0");
    variables.put("has_oneof_case_message", info.name + "Case_ == " + descriptor.getNumber());
  }

  // Print useful comments before a field's accessors.
  //
  public static void printExtraFieldInfo(Map<String, Object> variables, Printer printer) {
    String disambiguatedReason = (String) variables.get("disambiguated_reason");
    if (disambiguatedReason != null && !disambiguatedReason.isEmpty()) {
      printer.emit(variables,
          "// An alternative name is used for field \"$field_name$\" because:\n" +
          "//     $disambiguated_reason$\n");
    }
  }
}