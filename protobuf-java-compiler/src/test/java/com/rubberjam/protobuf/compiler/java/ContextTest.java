package com.rubberjam.protobuf.compiler.java;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.Options;

public class ContextTest {

  @Test
  public void testContextInitialization() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("test.proto")
        .setPackage("test")
        .addMessageType(com.google.protobuf.DescriptorProtos.DescriptorProto.newBuilder()
            .setName("TestMessage")
            .addField(com.google.protobuf.DescriptorProtos.FieldDescriptorProto.newBuilder()
                .setName("test_field")
                .setNumber(1)
                .setType(com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32)
                .build())
            .build())
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[0]);
    Options options = new Options();

    Context context = new Context(file, options);

    assertNotNull(context.getNameResolver());
    assertNotNull(context.getFieldGeneratorInfo(file.getMessageTypes().get(0).getFields().get(0)));
  }
}
