package com.rubberjam.protobuf.another.compiler.java;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.JavaFeaturesProto;
import org.junit.BeforeClass;
import org.junit.Test;

public class InternalHelpersTest {

  @BeforeClass
  public static void init() {
    try {
      Class.forName("com.google.protobuf.DescriptorProtos");
      Class.forName("com.google.protobuf.JavaFeaturesProto");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testSupportUnknownEnumValue() {
    FieldDescriptor field = mock(FieldDescriptor.class);
    DescriptorProtos.FieldOptions options = mock(DescriptorProtos.FieldOptions.class);
    DescriptorProtos.FeatureSet features = mock(DescriptorProtos.FeatureSet.class);
    JavaFeaturesProto.JavaFeatures javaFeatures = mock(JavaFeaturesProto.JavaFeatures.class);

    when(field.getOptions()).thenReturn(options);
    when(options.getFeatures()).thenReturn(features);
    when(features.getExtension(JavaFeaturesProto.java_)).thenReturn(javaFeatures);

    // Case 1: LegacyClosedEnum = true (Proto2 behavior)
    when(javaFeatures.getLegacyClosedEnum()).thenReturn(true);
    assertFalse(InternalHelpers.supportUnknownEnumValue(field));

    // Case 2: LegacyClosedEnum = false (Proto3 behavior)
    when(javaFeatures.getLegacyClosedEnum()).thenReturn(false);
    assertTrue(InternalHelpers.supportUnknownEnumValue(field));
  }

  @Test
  public void testCheckUtf8() {
    FieldDescriptor field = mock(FieldDescriptor.class);
    DescriptorProtos.FieldOptions options = mock(DescriptorProtos.FieldOptions.class);
    DescriptorProtos.FeatureSet features = mock(DescriptorProtos.FeatureSet.class);
    JavaFeaturesProto.JavaFeatures javaFeatures = mock(JavaFeaturesProto.JavaFeatures.class);

    when(field.getType()).thenReturn(FieldDescriptor.Type.STRING);
    when(field.getOptions()).thenReturn(options);
    when(options.getFeatures()).thenReturn(features);
    when(features.getExtension(JavaFeaturesProto.java_)).thenReturn(javaFeatures);

    // Case 1: Utf8Validation = VERIFY
    when(javaFeatures.getUtf8Validation()).thenReturn(JavaFeaturesProto.JavaFeatures.Utf8Validation.VERIFY);
    assertTrue(InternalHelpers.checkUtf8(field));

    // Case 2: Utf8Validation = DEFAULT (assuming logic treats verify as explicit VERIFY)
    when(javaFeatures.getUtf8Validation()).thenReturn(JavaFeaturesProto.JavaFeatures.Utf8Validation.DEFAULT);
    assertFalse(InternalHelpers.checkUtf8(field));

    // Case 3: Not STRING type
    when(field.getType()).thenReturn(FieldDescriptor.Type.INT32);
    assertFalse(InternalHelpers.checkUtf8(field));
  }

  @Test
  public void testCheckLargeEnum() {
    EnumDescriptor descriptor = mock(EnumDescriptor.class);
    DescriptorProtos.EnumOptions options = mock(DescriptorProtos.EnumOptions.class);
    DescriptorProtos.FeatureSet features = mock(DescriptorProtos.FeatureSet.class);
    JavaFeaturesProto.JavaFeatures javaFeatures = mock(JavaFeaturesProto.JavaFeatures.class);

    when(descriptor.getOptions()).thenReturn(options);
    when(options.getFeatures()).thenReturn(features);
    when(features.getExtension(JavaFeaturesProto.java_)).thenReturn(javaFeatures);

    // Case 1: LargeEnum = true
    when(javaFeatures.getLargeEnum()).thenReturn(true);
    assertTrue(InternalHelpers.checkLargeEnum(descriptor));

    // Case 2: LargeEnum = false
    when(javaFeatures.getLargeEnum()).thenReturn(false);
    assertFalse(InternalHelpers.checkLargeEnum(descriptor));
  }
}
