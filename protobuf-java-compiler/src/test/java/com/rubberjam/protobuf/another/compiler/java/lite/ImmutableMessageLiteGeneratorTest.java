package com.rubberjam.protobuf.another.compiler.java.lite;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.rubberjam.protobuf.another.compiler.java.Context;
import com.rubberjam.protobuf.another.compiler.java.Options;
import com.rubberjam.protobuf.io.Printer;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ImmutableMessageLiteGeneratorTest {
  private Printer printer;

  @Before
  public void setUp() {
    printer = mock(Printer.class);
  }

  @Test
  public void testMessageGenerator() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("foo")
        .addMessageType(DescriptorProto.newBuilder()
            .setName("MyMessage")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("my_field")
                .setNumber(1)
                .setType(FieldDescriptorProto.Type.TYPE_INT32)))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    Descriptor message = file.findMessageTypeByName("MyMessage");

    ImmutableMessageLiteGenerator generator =
        new ImmutableMessageLiteGenerator(message, new Context(file, new Options()));

    generator.generate(printer);

    ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
    // Check for Lite base class inheritance
    verify(printer, atLeastOnce()).emit(variablesCaptor.capture(), contains("com.google.protobuf.GeneratedMessageLite<"));

    Map<String, Object> vars = variablesCaptor.getValue();
    assertEquals("foo.FooProto.MyMessage", vars.get("classname"));

    // Check dynamicMethod override
    verify(printer, atLeastOnce()).emit(contains("protected final java.lang.Object dynamicMethod("));

    // Check default instance registration
    verify(printer, atLeastOnce()).emit(anyMap(), contains("com.google.protobuf.GeneratedMessageLite.registerDefaultInstance("));
  }
}
