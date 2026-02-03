package com.rubberjam.protobuf.compiler.java;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.junit.Test;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.compiler.java.GeneratorCommon.FieldGenerator;
import com.rubberjam.protobuf.compiler.java.GeneratorCommon.FieldGeneratorMap;

public class GeneratorCommonTest {

  @Test
  public void testFieldGeneratorMap() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("test.proto")
        .addMessageType(DescriptorProto.newBuilder()
            .setName("TestMessage")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("test_field")
                .setNumber(1)
                .setType(FieldDescriptorProto.Type.TYPE_INT32)
                .build())
            .build())
        .build();
    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[0]);
    Descriptor descriptor = file.getMessageTypes().get(0);
    FieldDescriptor field = descriptor.getFields().get(0);

    FieldGeneratorMap<FieldGenerator> map = new FieldGeneratorMap<>(descriptor);
    FieldGenerator generator = mock(FieldGenerator.class);

    map.add(field, generator);

    assertEquals(generator, map.get(field));
    assertEquals(1, map.getFieldGenerators().size());
  }
}
