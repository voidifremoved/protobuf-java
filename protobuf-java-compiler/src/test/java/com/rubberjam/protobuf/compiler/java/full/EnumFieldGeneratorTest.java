package com.rubberjam.protobuf.compiler.java.full;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.InternalHelpers;
import com.rubberjam.protobuf.compiler.java.Options;
import com.rubberjam.protobuf.compiler.java.full.EnumFieldGenerator;
import com.rubberjam.protobuf.compiler.java.full.RepeatedEnumFieldGenerator;
import com.rubberjam.protobuf.io.Printer;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class EnumFieldGeneratorTest {
  private Context context;
  private FieldDescriptor fieldDescriptor;
  private Printer printer;
  private EnumFieldGenerator generator;

  @Before
  public void setUp() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("foo")
        .setSyntax("proto3")
        .addEnumType(EnumDescriptorProto.newBuilder()
            .setName("MyEnum")
            .addValue(EnumValueDescriptorProto.newBuilder().setName("VAL0").setNumber(0))
            .addValue(EnumValueDescriptorProto.newBuilder().setName("VAL1").setNumber(1)))
        .addMessageType(DescriptorProto.newBuilder()
            .setName("MyMessage")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("my_enum")
                .setNumber(1)
                .setTypeName(".foo.MyEnum")
                .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    Descriptor message = file.findMessageTypeByName("MyMessage");
    fieldDescriptor = message.findFieldByName("my_enum");

    context = new Context(file, new Options());
    printer = mock(Printer.class);

    generator = new EnumFieldGenerator(fieldDescriptor, 0, 0, context);
  }

  @Test
  public void testGenerateMembers_Proto3() {
    generator.generateMembers(printer);
    // Proto3 enum is stored as int
    verify(printer, atLeastOnce()).emit(anyMap(), contains("private int $name$_;\n"));
    verify(printer, atLeastOnce()).emit(anyMap(), contains("public int get$capitalized_name$Value() {\n"));
  }

  @Test
  public void testGenerateMembers_Proto2() throws Exception {
      FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("bar.proto")
        .setPackage("bar")
        .setSyntax("proto2")
        .addEnumType(EnumDescriptorProto.newBuilder()
            .setName("MyEnum2")
            .addValue(EnumValueDescriptorProto.newBuilder().setName("VAL0").setNumber(0)))
        .addMessageType(DescriptorProto.newBuilder()
            .setName("MyMessage2")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("my_enum2")
                .setNumber(1)
                .setTypeName(".bar.MyEnum2")
                .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    Descriptor message = file.findMessageTypeByName("MyMessage2");
    FieldDescriptor field = message.findFieldByName("my_enum2");

    // We need to ensure InternalHelpers.supportUnknownEnumValue returns false for this test
    // to simulate Proto2 closed enum behavior.
    try (org.mockito.MockedStatic<InternalHelpers> mockedHelpers = mockStatic(InternalHelpers.class)) {
        mockedHelpers.when(() -> InternalHelpers.supportUnknownEnumValue(any())).thenReturn(false);

        EnumFieldGenerator gen2 = new EnumFieldGenerator(field, 0, 0, new Context(file, new Options()));

        gen2.generateMembers(printer);
        // Proto2 closed enum is stored as Enum type
        verify(printer, atLeastOnce()).emit(anyMap(), contains("private $type$ $name$_;\n"));
        verify(printer, atLeastOnce()).emit(anyMap(), contains("public $type$ get$capitalized_name$() {\n"));
    }
  }

  @Test
  public void testGenerateBuilderMembers_Proto3() {
    generator.generateBuilderMembers(printer);
    // Proto3 enum builder member
    verify(printer, atLeastOnce()).emit(anyMap(), contains("private int $name$_ = $default_number$;\n"));
    verify(printer, atLeastOnce()).emit(anyMap(), contains("public int get$capitalized_name$Value() {\n"));
  }

  @Test
  public void testRepeatedEnum_Proto3() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("repeated.proto")
        .setPackage("repeated")
        .setSyntax("proto3")
        .addEnumType(EnumDescriptorProto.newBuilder()
            .setName("MyEnum3")
            .addValue(EnumValueDescriptorProto.newBuilder().setName("VAL0").setNumber(0)))
        .addMessageType(DescriptorProto.newBuilder()
            .setName("MyMessage3")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("my_enums")
                .setNumber(1)
                .setTypeName(".repeated.MyEnum3")
                .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    Descriptor message = file.findMessageTypeByName("MyMessage3");
    FieldDescriptor field = message.findFieldByName("my_enums");

    RepeatedEnumFieldGenerator repeatedGen =
        new RepeatedEnumFieldGenerator(field, 0, 0, new Context(file, new Options()));

    repeatedGen.generateMembers(printer);
    // Proto3 repeated enum: List<Integer> and conversion wrapper
    verify(printer, atLeastOnce()).emit(anyMap(), contains("private java.util.List<java.lang.Integer> $name$_;\n"));
    verify(printer, atLeastOnce()).emit(anyMap(), contains("get$capitalized_name$ValueList()"));
  }
}
