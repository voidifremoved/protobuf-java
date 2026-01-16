package com.google.protobuf.compiler.java;

import com.google.protobuf.Descriptors.FieldDescriptor;

/**
 * Utilities for generating names.
 */
public final class Names {

  private Names() {}

  public static String newBuilder(FieldDescriptor field) {
    if (field.isRepeated()) {
      return "add" + StringUtils.capitalizedFieldName(field) + "Builder";
    } else {
      return "get" + StringUtils.capitalizedFieldName(field) + "Builder";
    }
  }

  // Add other methods as needed from names.cc
}
