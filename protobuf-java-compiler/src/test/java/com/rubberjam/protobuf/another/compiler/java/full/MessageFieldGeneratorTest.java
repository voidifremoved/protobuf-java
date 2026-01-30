package com.rubberjam.protobuf.another.compiler.java.full;

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

public class MessageFieldGeneratorTest {
  private Context context;
  private Printer printer;

  @Before
  public void setUp() {
    printer = mock(Printer.class);
  }

  @Test
  public void testMessageField() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("foo")
        .addMessageType(DescriptorProto.newBuilder()
            .setName("Inner")
            .addField(FieldDescriptorProto.newBuilder().setName("val").setNumber(1).setType(FieldDescriptorProto.Type.TYPE_INT32)))
        .addMessageType(DescriptorProto.newBuilder()
            .setName("Outer")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("inner_field")
                .setNumber(1)
                .setTypeName(".foo.Inner")
                .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
                .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    Descriptor outer = file.findMessageTypeByName("Outer");
    FieldDescriptor field = outer.findFieldByName("inner_field");

    MessageFieldGenerator generator =
        new MessageFieldGenerator(field, 0, 0, new Context(file, new Options()));

    generator.generateMembers(printer);

    // Check variable resolution
    ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(printer, atLeastOnce()).emit(variablesCaptor.capture(), contains("public $type$ get$capitalized_name$() {"));
    Map<String, Object> vars = variablesCaptor.getValue();
    assertEquals("foo.FooProto.Inner", vars.get("type"));

    verify(printer, atLeastOnce()).emit(anyMap(), contains("public $type$OrBuilder get$capitalized_name$OrBuilder() {"));
  }

  @Test
  public void testRepeatedMessageField() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("bar.proto")
        .setPackage("bar")
        .addMessageType(DescriptorProto.newBuilder()
            .setName("Inner")
            .addField(FieldDescriptorProto.newBuilder().setName("val").setNumber(1).setType(FieldDescriptorProto.Type.TYPE_INT32)))
        .addMessageType(DescriptorProto.newBuilder()
            .setName("Outer")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("inner_list")
                .setNumber(1)
                .setTypeName(".bar.Inner")
                .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)
                .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    Descriptor outer = file.findMessageTypeByName("Outer");
    FieldDescriptor field = outer.findFieldByName("inner_list");

    RepeatedMessageFieldGenerator generator =
        new RepeatedMessageFieldGenerator(field, 0, 0, new Context(file, new Options()));

    generator.generateMembers(printer);

    ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(printer, atLeastOnce()).emit(variablesCaptor.capture(), contains("public java.util.List<$type$> get$capitalized_name$List() {"));
    Map<String, Object> vars = variablesCaptor.getValue();
    assertEquals("bar.BarProto.Inner", vars.get("type"));
  }
}
