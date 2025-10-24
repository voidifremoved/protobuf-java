// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.protobuf.compiler.java;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import java.util.HashSet;
import java.util.Set;

/** A utility class for string manipulations. */
public final class Helpers {

  private Helpers() {}

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

  public static String getOneofStoredType(FieldDescriptor field) {
    JavaType javaType = getJavaType(field);
    switch (javaType) {
      case ENUM:
        return "java.lang.Integer";
      case MESSAGE:
        return new ClassNameResolver().getClassName(field.getMessageType(), true);
      default:
        return boxedPrimitiveTypeName(javaType);
    }
  }

  public static String defaultValue(FieldDescriptor field) {
    switch (field.getType()) {
      case INT32:
        return Integer.toString(field.getDefaultValue().hashCode());
      case INT64:
        return Long.toString(field.getDefaultValue().hashCode()) + "L";
      case FLOAT:
        return Float.toString((Float) field.getDefaultValue()) + "F";
      case DOUBLE:
        return Double.toString((Double) field.getDefaultValue()) + "D";
      case BOOL:
        return Boolean.toString((Boolean) field.getDefaultValue());
      case STRING:
        return "\"" + (String) field.getDefaultValue() + "\"";
      case BYTES:
        return "com.google.protobuf.ByteString.EMPTY";
      case ENUM:
        return new ClassNameResolver().getClassName(field.getEnumType(), true)
            + "."
            + field.getDefaultValue().toString();
      case MESSAGE:
        return new ClassNameResolver().getClassName(field.getMessageType(), true)
            + ".getDefaultInstance()";
    }
    throw new IllegalArgumentException("Unknown field type: " + field.getType());
  }

  public static boolean isDefaultValueJavaDefault(FieldDescriptor field) {
    switch (field.getType()) {
      case INT32:
        return (Integer) field.getDefaultValue() == 0;
      case INT64:
        return (Long) field.getDefaultValue() == 0L;
      case FLOAT:
        return (Float) field.getDefaultValue() == 0F;
      case DOUBLE:
        return (Double) field.getDefaultValue() == 0D;
      case BOOL:
        return !(Boolean) field.getDefaultValue();
      case STRING:
      case BYTES:
      case ENUM:
      case MESSAGE:
        return false;
    }
    throw new IllegalArgumentException("Unknown field type: " + field.getType());
  }

  public static boolean isByteStringWithCustomDefaultValue(FieldDescriptor field) {
    return getJavaType(field) == JavaType.BYTES && !field.getDefaultValue().toString().isEmpty();
  }

  public static String javaPackageToDir(String packageName) {
    return packageName.replace('.', '/');
  }

  public static boolean hasRequiredFields(Descriptor descriptor) {
    Set<Descriptor> alreadySeen = new HashSet<>();
    return hasRequiredFields(descriptor, alreadySeen);
  }

  private static boolean hasRequiredFields(Descriptor descriptor, Set<Descriptor> alreadySeen) {
    if (alreadySeen.contains(descriptor)) {
      return false;
    }
    alreadySeen.add(descriptor);
    if (descriptor.toProto().getExtensionRangeCount() > 0) {
      return true;
    }
    for (FieldDescriptor field : descriptor.getFields()) {
      if (field.isRequired()) {
        return true;
      }
      if (getJavaType(field) == JavaType.MESSAGE) {
        if (hasRequiredFields(field.getMessageType(), alreadySeen)) {
          return true;
        }
      }
    }
    return false;
  }
}
