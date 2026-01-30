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

public class RepeatedImmutableMessageFieldLiteGeneratorTest {
  private Printer printer;

  @Before
  public void setUp() {
    printer = mock(Printer.class);
  }

  @Test
  public void testRepeatedMessageField() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("foo")
        .addMessageType(DescriptorProto.newBuilder()
            .setName("SubMessage"))
        .addMessageType(DescriptorProto.newBuilder()
            .setName("MyMessage")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("sub_messages")
                .setNumber(1)
                .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)
                .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
                .setTypeName(".foo.SubMessage")))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    Descriptor message = file.findMessageTypeByName("MyMessage");
    FieldDescriptor field = message.findFieldByName("sub_messages");

    RepeatedImmutableMessageFieldLiteGenerator generator =
        new RepeatedImmutableMessageFieldLiteGenerator(field, 0, new Context(file, new Options()));

    generator.generateMembers(printer);

    ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
    // Verify list type is generated
    verify(printer, atLeastOnce()).emit(variablesCaptor.capture(), contains("private $field_list_type$ $name$_;"));

    Map<String, Object> vars = variablesCaptor.getValue();
    assertEquals("com.google.protobuf.Internal.ProtobufList<foo.FooProto.SubMessage>", vars.get("field_list_type"));
    assertEquals("foo.FooProto.SubMessage", vars.get("type"));

    // Verify ensureIsMutable
    verify(printer, atLeastOnce()).emit(anyMap(), contains("ensure$capitalized_name$IsMutable()"));

    // Verify addAll
    verify(printer, atLeastOnce()).emit(anyMap(), contains("addAll$capitalized_name$("));
  }
}
