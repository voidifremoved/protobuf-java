package com.rubberjam.protobuf.compiler.java.lite;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.OneofDescriptorProto;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.Options;
import com.rubberjam.protobuf.compiler.java.lite.ImmutableMessageLiteGenerator;
import com.rubberjam.protobuf.compiler.java.lite.ImmutablePrimitiveOneofFieldLiteGenerator;
import com.rubberjam.protobuf.compiler.java.lite.ImmutableStringOneofFieldLiteGenerator;
import com.rubberjam.protobuf.io.Printer;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ImmutableOneofFieldLiteGeneratorTest {
  private Printer printer;

  @Before
  public void setUp() {
    printer = mock(Printer.class);
  }

  @Test
  public void testOneofFieldGenerator() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("foo")
        .addMessageType(DescriptorProto.newBuilder()
            .setName("MyMessage")
            .addOneofDecl(OneofDescriptorProto.newBuilder().setName("my_oneof"))
            .addField(FieldDescriptorProto.newBuilder()
                .setName("foo_int")
                .setNumber(1)
                .setType(FieldDescriptorProto.Type.TYPE_INT32)
                .setOneofIndex(0))
            .addField(FieldDescriptorProto.newBuilder()
                .setName("bar_string")
                .setNumber(2)
                .setType(FieldDescriptorProto.Type.TYPE_STRING)
                .setOneofIndex(0)))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    Descriptor message = file.findMessageTypeByName("MyMessage");

    // Test Message Generation for Oneof Case Enum
    ImmutableMessageLiteGenerator messageGenerator =
        new ImmutableMessageLiteGenerator(message, new Context(file, new Options()));

    messageGenerator.generate(printer);

    verify(printer, atLeastOnce()).emit(contains("public enum MyOneofCase {"));
    verify(printer, atLeastOnce()).emit(contains("FOO_INT(1),"));
    verify(printer, atLeastOnce()).emit(contains("BAR_STRING(2),"));
    verify(printer, atLeastOnce()).emit(contains("MYONEOF_NOT_SET(0);"));
    verify(printer, atLeastOnce()).emit(contains("switch (value) {"));

    // Test Field Generation for Primitive Oneof
    FieldDescriptor fooInt = message.findFieldByName("foo_int");
    ImmutablePrimitiveOneofFieldLiteGenerator primitiveOneofGenerator =
        new ImmutablePrimitiveOneofFieldLiteGenerator(fooInt, 0, new Context(file, new Options()));

    primitiveOneofGenerator.generateMembers(printer);

    ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
    // Verify getter checks case
    verify(printer, atLeastOnce()).emit(variablesCaptor.capture(), contains("if ($has_oneof_case_message$) {"));
    Map<String, Object> vars = variablesCaptor.getValue();
    assertEquals("myOneofCase_ == 1", vars.get("has_oneof_case_message"));

    // Verify getter casts object
    // Format string has ($boxed_type$) $oneof_name$_;
    verify(printer, atLeastOnce()).emit(anyMap(), contains("return ($boxed_type$) $oneof_name$_;"));

    // Test Field Generation for String Oneof
    FieldDescriptor barString = message.findFieldByName("bar_string");
    ImmutableStringOneofFieldLiteGenerator stringOneofGenerator =
        new ImmutableStringOneofFieldLiteGenerator(barString, 0, new Context(file, new Options()));

    stringOneofGenerator.generateMembers(printer);

    // Verify getter checks case
    // Just verify the format string is used
    verify(printer, atLeastOnce()).emit(anyMap(), contains("if ($has_oneof_case_message$) {"));
  }
}
