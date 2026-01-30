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

public class MapFieldGeneratorTest {
  private Context context;
  private Printer printer;

  @Before
  public void setUp() {
    printer = mock(Printer.class);
  }

  @Test
  public void testMapField() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("foo")
        .addMessageType(DescriptorProto.newBuilder()
            .setName("MyMessage")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("my_map")
                .setNumber(1)
                .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)
                .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
                .setTypeName(".foo.MyMessage.MyMapEntry"))
            .addNestedType(DescriptorProto.newBuilder()
                .setName("MyMapEntry")
                .setOptions(com.google.protobuf.DescriptorProtos.MessageOptions.newBuilder().setMapEntry(true).build())
                .addField(FieldDescriptorProto.newBuilder().setName("key").setNumber(1).setType(FieldDescriptorProto.Type.TYPE_STRING))
                .addField(FieldDescriptorProto.newBuilder().setName("value").setNumber(2).setType(FieldDescriptorProto.Type.TYPE_INT32))))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    Descriptor message = file.findMessageTypeByName("MyMessage");
    FieldDescriptor field = message.findFieldByName("my_map");

    MapFieldGenerator generator =
        new MapFieldGenerator(field, 0, 0, new Context(file, new Options()));

    generator.generateMembers(printer);

    ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
    verify(printer, atLeastOnce()).emit(variablesCaptor.capture(), contains("public java.util.Map<$key_type$, $value_type$> get$capitalized_name$Map() {"));
    Map<String, Object> vars = variablesCaptor.getValue();
    assertEquals("java.lang.String", vars.get("key_type"));
    assertEquals("java.lang.Integer", vars.get("value_type"));

    // Verify DefaultEntryHolder generation
    verify(printer, atLeastOnce()).emit(anyMap(), contains("private static final class $capitalized_name$DefaultEntryHolder {"));
  }
}
