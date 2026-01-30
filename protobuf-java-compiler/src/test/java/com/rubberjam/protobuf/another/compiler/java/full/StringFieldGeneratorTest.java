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

public class StringFieldGeneratorTest {
  private Context context;
  private Printer printer;

  @Before
  public void setUp() {
    printer = mock(Printer.class);
  }

  @Test
  public void testStringField() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("foo")
        .addMessageType(DescriptorProto.newBuilder()
            .setName("MyMessage")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("my_string")
                .setNumber(1)
                .setType(FieldDescriptorProto.Type.TYPE_STRING)))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    Descriptor message = file.findMessageTypeByName("MyMessage");
    FieldDescriptor field = message.findFieldByName("my_string");

    StringFieldGenerator generator =
        new StringFieldGenerator(field, 0, 0, new Context(file, new Options()));

    generator.generateMembers(printer);
    verify(printer, atLeastOnce()).emit(anyMap(), contains("private volatile java.lang.Object $name$_;\n"));
    verify(printer, atLeastOnce()).emit(anyMap(), contains("public java.lang.String get$capitalized_name$() {\n"));
    verify(printer, atLeastOnce()).emit(anyMap(), contains("public com.google.protobuf.ByteString\n    get$capitalized_name$Bytes() {\n"));
  }

  @Test
  public void testBytesField() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("bar.proto")
        .setPackage("bar")
        .addMessageType(DescriptorProto.newBuilder()
            .setName("MyMessage")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("my_bytes")
                .setNumber(1)
                .setType(FieldDescriptorProto.Type.TYPE_BYTES)))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    Descriptor message = file.findMessageTypeByName("MyMessage");
    FieldDescriptor field = message.findFieldByName("my_bytes");

    StringFieldGenerator generator =
        new StringFieldGenerator(field, 0, 0, new Context(file, new Options()));

    generator.generateMembers(printer);
    verify(printer, atLeastOnce()).emit(anyMap(), contains("private volatile java.lang.Object $name$_;\n"));
    verify(printer, atLeastOnce()).emit(anyMap(), contains("public com.google.protobuf.ByteString get$capitalized_name$() {\n"));
  }

  @Test
  public void testRepeatedString() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("repeated_string.proto")
        .setPackage("foo")
        .addMessageType(DescriptorProto.newBuilder()
            .setName("MyMessage")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("my_strings")
                .setNumber(1)
                .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)
                .setType(FieldDescriptorProto.Type.TYPE_STRING)))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    Descriptor message = file.findMessageTypeByName("MyMessage");
    FieldDescriptor field = message.findFieldByName("my_strings");

    RepeatedStringFieldGenerator generator =
        new RepeatedStringFieldGenerator(field, 0, 0, new Context(file, new Options()));

    generator.generateMembers(printer);
    verify(printer, atLeastOnce()).emit(anyMap(), contains("com.google.protobuf.LazyStringArrayList $name$_"));

    // Test SerializedSizeCode
    generator.generateSerializedSizeCode(printer);
    verify(printer, atLeastOnce()).emit(anyMap(), contains("com.google.protobuf.CodedOutputStream.computeStringSizeNoTag"));
  }

  @Test
  public void testRepeatedBytes() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("repeated_bytes.proto")
        .setPackage("foo")
        .addMessageType(DescriptorProto.newBuilder()
            .setName("MyMessage")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("my_bytes_list")
                .setNumber(1)
                .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)
                .setType(FieldDescriptorProto.Type.TYPE_BYTES)))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    Descriptor message = file.findMessageTypeByName("MyMessage");
    FieldDescriptor field = message.findFieldByName("my_bytes_list");

    RepeatedStringFieldGenerator generator =
        new RepeatedStringFieldGenerator(field, 0, 0, new Context(file, new Options()));

    generator.generateMembers(printer);
    verify(printer, atLeastOnce()).emit(anyMap(), contains("java.util.List<com.google.protobuf.ByteString> $name$_"));
  }
}
