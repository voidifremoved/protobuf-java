package com.rubberjam.protobuf.compiler.java.lite;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.Options;
import com.rubberjam.protobuf.compiler.java.lite.ImmutableExtensionLiteGenerator;
import com.rubberjam.protobuf.io.Printer;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ImmutableExtensionLiteGeneratorTest {
  private Printer printer;

  @Before
  public void setUp() {
    printer = mock(Printer.class);
  }

  @Test
  public void testExtensionGenerator() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("foo")
        .addMessageType(DescriptorProto.newBuilder().setName("BaseMessage").addExtensionRange(DescriptorProto.ExtensionRange.newBuilder().setStart(100).setEnd(200)))
        .addExtension(FieldDescriptorProto.newBuilder()
            .setName("my_extension")
            .setNumber(100)
            .setExtendee(".foo.BaseMessage")
            .setType(FieldDescriptorProto.Type.TYPE_INT32))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    FieldDescriptor extension = file.findExtensionByName("my_extension");

    ImmutableExtensionLiteGenerator generator =
        new ImmutableExtensionLiteGenerator(extension, new Context(file, new Options()));

    generator.generate(printer);

    ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
    // Check for GeneratedExtension definition using Lite API
    verify(printer, atLeastOnce()).emit(variablesCaptor.capture(), contains("newSingularGeneratedExtension("));

    Map<String, Object> vars = variablesCaptor.getValue();
    assertEquals("myExtension", vars.get("name"));
    assertEquals("java.lang.Integer", vars.get("type"));
  }
}
