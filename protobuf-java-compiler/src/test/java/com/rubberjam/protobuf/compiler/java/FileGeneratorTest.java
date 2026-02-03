package com.rubberjam.protobuf.compiler.java;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.compiler.java.FileGenerator;
import com.rubberjam.protobuf.compiler.java.Options;
import com.rubberjam.protobuf.io.Printer;

public class FileGeneratorTest {

  @Test
  public void testGenerateSingleFile() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("test.proto")
        .setPackage("test")
        .addMessageType(com.google.protobuf.DescriptorProtos.DescriptorProto.newBuilder()
            .setName("TestMessage")
            .addField(com.google.protobuf.DescriptorProtos.FieldDescriptorProto.newBuilder()
                .setName("test_field")
                .setNumber(1)
                .setType(com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32)
                .build())
            .build())
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[0]);
    Options options = new Options();
    // Default options: multipleFiles = false

    FileGenerator generator = new FileGenerator(file, options);
    Printer.Options printerOptions = new Printer.Options();
    printerOptions.variableDelimiter = '$';
    Printer printer = new Printer(printerOptions);

    generator.generate(printer);

    String generated = printer.toString();
    assertTrue(generated.contains("package test;"));
    assertTrue(generated.contains("public final class TestProto {"));
    assertTrue(generated.contains("public static final class TestMessage"));
    assertTrue(generated.contains("descriptor = com.google.protobuf.Descriptors.FileDescriptor"));
    assertTrue(generated.contains("public static void registerAllExtensions("));
  }

  @Test
  public void testGenerateMultipleFiles() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("test_multi.proto")
        .setPackage("test.multi")
        .setOptions(com.google.protobuf.DescriptorProtos.FileOptions.newBuilder()
            .setJavaMultipleFiles(true)
            .build())
        .addMessageType(com.google.protobuf.DescriptorProtos.DescriptorProto.newBuilder()
            .setName("TestMessageMulti")
            .build())
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[0]);
    Options options = new Options();

    FileGenerator generator = new FileGenerator(file, options);
    Printer.Options printerOptions = new Printer.Options();
    printerOptions.variableDelimiter = '$';
    Printer printer = new Printer(printerOptions);

    generator.generate(printer);

    String generated = printer.toString();
    assertTrue(generated.contains("package test.multi;"));
    assertTrue(generated.contains("public final class TestMultiProto {"));
    // Should NOT contain message definition in outer class
    // But check that it doesn't contain "public static final class TestMessageMulti"
    // Wait, TestMessageMulti would be top level class in its own file.
    // So it should not be present in output of FileGenerator which generates outer class.
    assertTrue(!generated.contains("public static final class TestMessageMulti"));

    assertTrue(generated.contains("descriptor = com.google.protobuf.Descriptors.FileDescriptor"));
  }
}
