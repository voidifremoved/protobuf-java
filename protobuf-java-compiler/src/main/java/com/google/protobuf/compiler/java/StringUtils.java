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
}
