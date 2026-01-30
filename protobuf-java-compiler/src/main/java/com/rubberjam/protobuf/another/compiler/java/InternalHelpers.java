package com.rubberjam.protobuf.another.compiler.java;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.io.Printer;

/**
 * Internal helpers for Java code generation. Ported from internal_helpers.h.
 */
public final class InternalHelpers {

  private InternalHelpers() {}

  public static boolean supportUnknownEnumValue(FieldDescriptor field) {
    // TODO: Implement full Editions support using JavaFeatures
    // For now, assume Proto3 supports unknown enum values (open), Proto2 does not (closed).
    return "proto3".equals(field.getFile().toProto().getSyntax());
  }

  public static boolean checkUtf8(FieldDescriptor descriptor) {
    // TODO: Implement full Editions support using JavaFeatures
    if (descriptor.getType() != FieldDescriptor.Type.STRING) {
      return false;
    }
    if ("proto3".equals(descriptor.getFile().toProto().getSyntax())) {
      return true;
    }
    return descriptor.getFile().getOptions().getJavaStringCheckUtf8();
  }

  public static boolean checkLargeEnum(EnumDescriptor descriptor) {
    // TODO: Implement full Editions support using JavaFeatures
    return false;
  }

  public static void generateLarge(Printer printer, EnumDescriptor descriptor, boolean immutable, Context context, ClassNameResolver nameResolver) {
    // Logic for generating large enums would go here.
    // This often involves splitting the static initialization code.
    // Since this is a complex generation logic, and might not be used if checkLargeEnum is false,
    // we can stub it or implement a basic version if needed.
    // For now, we leave it empty or minimal as we defaulted checkLargeEnum to false.
  }

  // Only the lowest two bytes of the return value are used.
  public static int getExperimentalJavaFieldType(FieldDescriptor field) {
    int result = Helpers.getJavaType(field).ordinal();

    // bit 0: whether the field is required.
    if (field.isRequired()) {
      result |= 0x100;
    }

    // bit 1: whether the field requires UTF-8 validation.
    if (checkUtf8(field)) {
      result |= 0x200;
    }

    // bit 2: whether the field needs isInitialized check.
    if (Helpers.getJavaType(field) == Helpers.JavaType.MESSAGE && Helpers.hasRequiredFields(field.getMessageType())) {
        result |= 0x400;
    }

    // bit 3: whether the field is a closed enum.
    if (field.getType() == FieldDescriptor.Type.ENUM && !supportUnknownEnumValue(field)) {
      result |= 0x800;
    }

    return result;
  }
}
