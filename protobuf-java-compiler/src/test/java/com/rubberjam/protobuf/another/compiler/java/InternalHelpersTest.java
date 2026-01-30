package com.rubberjam.protobuf.another.compiler.java;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;

public class InternalHelpersTest {

  @Test
  public void testSupportUnknownEnumValue() throws Exception {
    FileDescriptorProto proto3 = FileDescriptorProto.newBuilder()
        .setName("test3.proto")
        .setSyntax("proto3")
        .addEnumType(EnumDescriptorProto.newBuilder().setName("TestEnum").addValue(EnumValueDescriptorProto.newBuilder().setName("A").setNumber(0)))
        .addMessageType(DescriptorProtos.DescriptorProto.newBuilder()
            .setName("TestMessage")
            .addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
                .setName("test_field")
                .setNumber(1)
                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM)
                .setTypeName(".TestEnum")
                .build())
            .build())
        .build();
    FileDescriptor file3 = FileDescriptor.buildFrom(proto3, new FileDescriptor[0]);
    FieldDescriptor field3 = file3.getMessageTypes().get(0).getFields().get(0);

    assertTrue(InternalHelpers.supportUnknownEnumValue(field3));

    FileDescriptorProto proto2 = FileDescriptorProto.newBuilder()
        .setName("test2.proto")
        .setSyntax("proto2")
        .addEnumType(EnumDescriptorProto.newBuilder().setName("TestEnum").addValue(EnumValueDescriptorProto.newBuilder().setName("A").setNumber(0)))
        .addMessageType(DescriptorProtos.DescriptorProto.newBuilder()
            .setName("TestMessage")
            .addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
                .setName("test_field")
                .setNumber(1)
                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM)
                .setTypeName(".TestEnum")
                .build())
            .build())
        .build();
    FileDescriptor file2 = FileDescriptor.buildFrom(proto2, new FileDescriptor[0]);
    FieldDescriptor field2 = file2.getMessageTypes().get(0).getFields().get(0);

    assertFalse(InternalHelpers.supportUnknownEnumValue(field2));
  }
}
