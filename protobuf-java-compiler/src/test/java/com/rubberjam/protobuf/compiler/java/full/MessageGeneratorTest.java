package com.rubberjam.protobuf.compiler.java.full;

import static org.junit.Assert.*;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.Options;
import com.rubberjam.protobuf.compiler.java.full.ImmutableMessageGenerator;
import com.rubberjam.protobuf.io.Printer;
import org.junit.Before;
import org.junit.Test;

public class MessageGeneratorTest {
  private Context context;
  private Descriptor descriptor;
  private Printer printer;
  private ImmutableMessageGenerator generator;

  @Before
  public void setUp() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("foo")
        .addMessageType(DescriptorProto.newBuilder()
            .setName("TestMessage")
            .addField(FieldDescriptorProto.newBuilder()
                .setName("optional_int32")
                .setNumber(1)
                .setType(FieldDescriptorProto.Type.TYPE_INT32)
                .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL))
            .addField(FieldDescriptorProto.newBuilder()
                .setName("repeated_string")
                .setNumber(2)
                .setType(FieldDescriptorProto.Type.TYPE_STRING)
                .setLabel(FieldDescriptorProto.Label.LABEL_REPEATED)))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    descriptor = file.findMessageTypeByName("TestMessage");
    assertNotNull("Descriptor should not be null", descriptor);

    context = new Context(file, new Options());
    printer = new Printer(new Printer.Options());

    generator = new ImmutableMessageGenerator(descriptor, context);
  }

  @Test
  public void testGenerate() {
    generator.generate(printer);
    String output = printer.toString();

    assertTrue("Output missing class def: " + output, output.contains("public final class TestMessage extends"));
    assertTrue("Output missing field constant: " + output, output.contains("public static final int OPTIONAL_INT32_FIELD_NUMBER = 1;"));
    assertTrue("Output missing private field: " + output, output.contains("private int optionalInt32_;"));
    assertTrue("Output missing repeated field constant: " + output, output.contains("public static final int REPEATED_STRING_FIELD_NUMBER = 2;"));
    assertTrue("Output missing getter: " + output, output.contains("public int getOptionalInt32() {"));
    assertTrue("Output missing repeated getter: " + output, output.contains("public java.util.List<java.lang.String> getRepeatedStringList() {"));
    assertTrue("Output missing newBuilder: " + output, output.contains("public static Builder newBuilder() {"));
  }

  @Test
  public void testGenerateInterface() {
    generator.generateInterface(printer);
    String output = printer.toString();

    assertTrue("Output missing interface def: " + output, output.contains("public interface TestMessageOrBuilder extends"));
    assertTrue("Output missing has: " + output, output.contains("boolean hasOptionalInt32();"));
    assertTrue("Output missing get: " + output, output.contains("int getOptionalInt32();"));
    assertTrue("Output missing repeated list get: " + output, output.contains("getRepeatedStringList();"));
  }
}
