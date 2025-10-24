// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.google.protobuf.compiler.java;

import com.google.protobuf.Descriptors.FieldDescriptor;

/** A utility class for internal helper functions. */
public final class InternalHelpers {

  private InternalHelpers() {}

  public static boolean supportUnknownEnumValue(FieldDescriptor field) {
    return field.getEnumType() != null && !field.getEnumType().toProto().getOptions().getDeprecated();
  }

  public static boolean checkUtf8(FieldDescriptor descriptor) {
    return descriptor.getFile().getOptions().getJavaStringCheckUtf8();
  }

  public static int getExperimentalJavaFieldType(FieldDescriptor field) {
    final int kMapFieldType = 50;
    final int kOneofFieldTypeOffset = 51;

    final int kRequiredBit = 0x100;
    final int kUtf8CheckBit = 0x200;
    final int kCheckInitialized = 0x400;
    final int kLegacyEnumIsClosedBit = 0x800;
    final int kHasHasBit = 0x1000;

    int extraBits = field.isRequired() ? kRequiredBit : 0;
    if (field.getType() == FieldDescriptor.Type.STRING && checkUtf8(field)) {
      extraBits |= kUtf8CheckBit;
    }
    if (field.isRequired()
        || (Helpers.getJavaType(field) == JavaType.MESSAGE
            && Helpers.hasRequiredFields(field.getMessageType()))) {
      extraBits |= kCheckInitialized;
    }
    if (field.hasOptionalKeyword()) {
      extraBits |= kHasHasBit;
    }
    if (Helpers.getJavaType(field) == JavaType.ENUM && !supportUnknownEnumValue(field)) {
      extraBits |= kLegacyEnumIsClosedBit;
    }

    if (field.isMapField()) {
      if (!supportUnknownEnumValue(field.getMessageType().getFields().get(1))) {
        FieldDescriptor value = field.getMessageType().getFields().get(1);
        if (Helpers.getJavaType(value) == JavaType.ENUM) {
          extraBits |= kLegacyEnumIsClosedBit;
        }
      }
      return kMapFieldType | extraBits;
    } else if (field.isPacked()) {
      return getExperimentalJavaFieldTypeForPacked(field) | extraBits;
    } else if (field.isRepeated()) {
      return getExperimentalJavaFieldTypeForRepeated(field) | extraBits;
    } else if (field.getContainingOneof() != null) {
      return (getExperimentalJavaFieldTypeForSingular(field) + kOneofFieldTypeOffset) | extraBits;
    } else {
      return getExperimentalJavaFieldTypeForSingular(field) | extraBits;
    }
  }

  private static int getExperimentalJavaFieldTypeForSingular(FieldDescriptor field) {
    int result = field.getType().ordinal();
    if (result == FieldDescriptor.Type.GROUP.ordinal()) {
      return 17;
    } else if (result < FieldDescriptor.Type.GROUP.ordinal()) {
      return result - 1;
    } else {
      return result - 2;
    }
  }

  private static int getExperimentalJavaFieldTypeForRepeated(FieldDescriptor field) {
    if (field.getType() == FieldDescriptor.Type.GROUP) {
      return 49;
    } else {
      return getExperimentalJavaFieldTypeForSingular(field) + 18;
    }
  }

  private static int getExperimentalJavaFieldTypeForPacked(FieldDescriptor field) {
    int result = field.getType().ordinal();
    if (result < FieldDescriptor.Type.STRING.ordinal()) {
      return result + 34;
    } else if (result > FieldDescriptor.Type.BYTES.ordinal()) {
      return result + 30;
    } else {
      throw new IllegalArgumentException(field.getFullName() + " can't be packed.");
    }
  }
}
