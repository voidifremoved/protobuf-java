// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.protobuf.compiler.java;

import com.google.protobuf.Descriptors.FieldDescriptor;

/**
 * A utility class for string manipulations.
 */
public final class StringUtils {

  private StringUtils() {}

  public static String underscoresToCamelCase(String input, boolean capNextLetter) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if ('a' <= c && c <= 'z') {
        if (capNextLetter) {
          result.append((char) (c + ('A' - 'a')));
        } else {
          result.append(c);
        }
        capNextLetter = false;
      } else if ('A' <= c && c <= 'Z') {
        if (i == 0 && !capNextLetter) {
          result.append((char) (c + ('a' - 'A')));
        } else {
          result.append(c);
        }
        capNextLetter = false;
      } else if ('0' <= c && c <= '9') {
        result.append(c);
        capNextLetter = true;
      } else {
        capNextLetter = true;
      }
    }
    if (input.endsWith("#")) {
      result.append('_');
    }
    return result.toString();
  }

  public static String capitalizedFieldName(FieldDescriptor field) {
    return underscoresToCamelCase(field.getName(), true);
  }

  public static String camelCaseFieldName(FieldDescriptor field) {
    String fieldName = underscoresToCamelCase(field.getName(), false);
    if (fieldName.matches("^[0-9].*")) {
      return "_" + fieldName;
    }
    return fieldName;
  }

  public static String fieldConstantName(FieldDescriptor field) {
    return field.getName().toUpperCase() + "_FIELD_NUMBER";
  }

  public static JavaType getJavaType(FieldDescriptor field) {
    switch (field.getType()) {
      case INT32:
      case UINT32:
      case SINT32:
      case FIXED32:
      case SFIXED32:
        return JavaType.INT;
      case INT64:
      case UINT64:
      case SINT64:
      case FIXED64:
      case SFIXED64:
        return JavaType.LONG;
      case FLOAT:
        return JavaType.FLOAT;
      case DOUBLE:
        return JavaType.DOUBLE;
      case BOOL:
        return JavaType.BOOLEAN;
      case STRING:
        return JavaType.STRING;
      case BYTES:
        return JavaType.BYTES;
      case ENUM:
        return JavaType.ENUM;
      case GROUP:
      case MESSAGE:
        return JavaType.MESSAGE;
    }
    throw new IllegalArgumentException("Unknown field type: " + field.getType());
  }

  public static String getFieldTypeName(FieldDescriptor.Type fieldType) {
    switch (fieldType) {
      case INT32:
        return "INT32";
      case UINT32:
        return "UINT32";
      case SINT32:
        return "SINT32";
      case FIXED32:
        return "FIXED32";
      case SFIXED32:
        return "SFIXED32";
      case INT64:
        return "INT64";
      case UINT64:
        return "UINT64";
      case SINT64:
        return "SINT64";
      case FIXED64:
        return "FIXED64";
      case SFIXED64:
        return "SFIXED64";
      case FLOAT:
        return "FLOAT";
      case DOUBLE:
        return "DOUBLE";
      case BOOL:
        return "BOOL";
      case STRING:
        return "STRING";
      case BYTES:
        return "BYTES";
      case ENUM:
        return "ENUM";
      case GROUP:
        return "GROUP";
      case MESSAGE:
        return "MESSAGE";
    }
    throw new IllegalArgumentException("Unknown field type: " + fieldType);
  }

  public static String boxedPrimitiveTypeName(JavaType type) {
    switch (type) {
      case INT:
        return "java.lang.Integer";
      case LONG:
        return "java.lang.Long";
      case FLOAT:
        return "java.lang.Float";
      case DOUBLE:
        return "java.lang.Double";
      case BOOLEAN:
        return "java.lang.Boolean";
      case STRING:
        return "java.lang.String";
      case BYTES:
        return "com.google.protobuf.ByteString";
      default:
        return null;
    }
  }

  public static String getPrimitiveTypeName(JavaType type) {
    switch (type) {
      case INT:
        return "int";
      case LONG:
        return "long";
      case FLOAT:
        return "float";
      case DOUBLE:
        return "double";
      case BOOLEAN:
        return "boolean";
      case STRING:
        return "java.lang.String";
      case BYTES:
        return "com.google.protobuf.ByteString";
      default:
        return null;
    }
  }

  public static String defaultValue(FieldDescriptor field) {
    if (field.isRepeated()) {
      return "java.util.Collections.emptyList()";
    }
    JavaType javaType = getJavaType(field);
    switch (javaType) {
      case INT:
        return "0";
      case LONG:
        return "0L";
      case FLOAT:
        return "0F";
      case DOUBLE:
        return "0D";
      case BOOLEAN:
        return "false";
      case STRING:
        return "\"\"";
      case BYTES:
        return "com.google.protobuf.ByteString.EMPTY";
      case ENUM:
        return field.getEnumType().getName() + "." + field.getEnumType().getValues().get(0).getName();
      case MESSAGE:
        return "null";
      default:
        return "null";
    }
  }

  public static boolean isDefaultValueJavaDefault(FieldDescriptor field) {
      if (field.isRepeated()) return false;
      JavaType javaType = getJavaType(field);
      // For now assume all defaults are java defaults for primitives if not explicitly set in proto
      // Real logic is more complex checking descriptor.hasDefaultValue()
      return !field.hasDefaultValue();
  }

  public static String toProperCase(String s) {
    if (s == null || s.isEmpty()) {
      return "";
    }
    if (s.length() == 1) {
      return s.toUpperCase();
    }
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }
}
