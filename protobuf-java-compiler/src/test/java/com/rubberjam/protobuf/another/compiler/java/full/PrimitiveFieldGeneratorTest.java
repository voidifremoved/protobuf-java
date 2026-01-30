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

public class PrimitiveFieldGeneratorTest {
  private Context context;
  private FieldDescriptor fieldDescriptor;
  private Printer printer;
  private PrimitiveFieldGenerator generator;

  @Before
  public void setUp() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("foo")
        .addMessageType(DescriptorProto.newBuilder()
            .setName("MyMessage")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("my_field")
                .setNumber(1)
                .setType(FieldDescriptorProto.Type.TYPE_INT32)
                .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL)))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    Descriptor message = file.findMessageTypeByName("MyMessage");
    fieldDescriptor = message.findFieldByName("my_field");

    context = new Context(file, new Options());
    printer = mock(Printer.class);

    generator = new PrimitiveFieldGenerator(fieldDescriptor, 0, 0, context);
  }

  @Test
  public void testGenerateMembers() {
    generator.generateMembers(printer);
    // Since printer.emit takes variables map, we can't easily check the resolved string unless we capture variables.
    // However, verify(printer).emit(anyMap(), contains(...)) checks the template string.
    verify(printer, atLeastOnce()).emit(anyMap(), contains("private $type$ $name$_;\n"));
    verify(printer, atLeastOnce()).emit(anyMap(), contains("public $type$ get$capitalized_name$() {\n"));
  }

  @Test
  public void testRepeatedField() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("bar.proto")
        .setPackage("bar")
        .addMessageType(DescriptorProto.newBuilder()
            .setName("MyMessage")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("my_list")
                .setNumber(1)
                .setType(FieldDescriptorProto.Type.TYPE_INT32)
                .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    Descriptor message = file.findMessageTypeByName("MyMessage");
    FieldDescriptor listField = message.findFieldByName("my_list");

    RepeatedPrimitiveFieldGenerator listGenerator =
        new RepeatedPrimitiveFieldGenerator(listField, 0, 0, new Context(file, new Options()));

    listGenerator.generateMembers(printer);
    verify(printer, atLeastOnce()).emit(anyMap(), contains("private $list_type$ $name$_;\n"));
    verify(printer, atLeastOnce()).emit(anyMap(), contains("public java.util.List<" + "java.lang.Integer" + "> $repeated_get$() {\n"));
  }

  @Test
  public void testSInt32Parsing() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("baz.proto")
        .setPackage("baz")
        .addMessageType(DescriptorProto.newBuilder()
            .setName("MyMessage")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("my_sint")
                .setNumber(1)
                .setType(FieldDescriptorProto.Type.TYPE_SINT32)))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    Descriptor message = file.findMessageTypeByName("MyMessage");
    FieldDescriptor sintField = message.findFieldByName("my_sint");

    PrimitiveFieldGenerator sintGenerator =
        new PrimitiveFieldGenerator(sintField, 0, 0, new Context(file, new Options()));

    sintGenerator.generateParsingCode(printer);
    ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(printer, atLeastOnce()).emit(variablesCaptor.capture(), contains("input.read$capitalized_type$();"));

    // Check if any of the captured maps has capitalized_type = SInt32
    boolean found = false;
    for (Map<String, Object> vars : variablesCaptor.getAllValues()) {
        if ("SInt32".equals(vars.get("capitalized_type"))) {
            found = true;
            break;
        }
    }
    assertTrue("Should have generated code with capitalized_type=SInt32", found);
  }
}
