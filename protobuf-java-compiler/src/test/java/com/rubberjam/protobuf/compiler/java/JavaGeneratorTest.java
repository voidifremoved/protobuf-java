package com.rubberjam.protobuf.compiler.java;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.rubberjam.protobuf.compiler.GeneratorContext;
import com.rubberjam.protobuf.compiler.java.JavaGenerator;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class JavaGeneratorTest {

    @Test
    public void testGenerateSimple() throws Exception {
        DescriptorProtos.FileDescriptorProto proto = DescriptorProtos.FileDescriptorProto.newBuilder()
                .setName("test.proto")
                .setPackage("com.example")
                .addMessageType(DescriptorProtos.DescriptorProto.newBuilder()
                        .setName("TestMessage")
                        .addField(DescriptorProtos.FieldDescriptorProto.newBuilder()
                                .setName("field1")
                                .setNumber(1)
                                .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32)
                                .build())
                        .build())
                .setOptions(DescriptorProtos.FileOptions.newBuilder()
                        .setJavaPackage("com.example.gen")
                        .setJavaOuterClassname("TestProto")
                        .build())
                .build();

        Descriptors.FileDescriptor file = Descriptors.FileDescriptor.buildFrom(proto, new Descriptors.FileDescriptor[]{});

        JavaGenerator generator = new JavaGenerator();
        TestGeneratorContext context = new TestGeneratorContext();
        generator.generate(file, "", context);

        Assert.assertTrue("File com/example/gen/TestProto.java should exist", context.files.containsKey("com/example/gen/TestProto.java"));
        String content = context.files.get("com/example/gen/TestProto.java");
        Assert.assertTrue(content.contains("public final class TestProto"));
        Assert.assertTrue(content.contains("package com.example.gen;"));
        // Check for descriptor initialization code (SharedCodeGenerator)
        Assert.assertTrue(content.contains("getDescriptor()"));
    }

    @Test
    public void testGenerateMultipleFiles() throws Exception {
        DescriptorProtos.FileDescriptorProto proto = DescriptorProtos.FileDescriptorProto.newBuilder()
                .setName("test_multi.proto")
                .setPackage("com.example")
                .addMessageType(DescriptorProtos.DescriptorProto.newBuilder()
                        .setName("TestMessage")
                        .build())
                .setOptions(DescriptorProtos.FileOptions.newBuilder()
                        .setJavaPackage("com.example.gen.multi")
                        .setJavaMultipleFiles(true)
                        .build())
                .build();

        Descriptors.FileDescriptor file = Descriptors.FileDescriptor.buildFrom(proto, new Descriptors.FileDescriptor[]{});

        JavaGenerator generator = new JavaGenerator();
        TestGeneratorContext context = new TestGeneratorContext();
        generator.generate(file, "", context);

        // Check outer class
        String outerFile = "com/example/gen/multi/TestMultiProto.java";
        Assert.assertTrue("Outer class file " + outerFile + " should exist", context.files.containsKey(outerFile));
        String outerContent = context.files.get(outerFile);
        Assert.assertTrue(outerContent.contains("public final class TestMultiProto"));

        // Check separate message file
        String messageFile = "com/example/gen/multi/TestMessage.java";
        Assert.assertTrue("Message file " + messageFile + " should exist", context.files.containsKey(messageFile));
        String messageContent = context.files.get(messageFile);
        Assert.assertTrue(messageContent.contains("public final class TestMessage"));
        Assert.assertTrue(messageContent.contains("package com.example.gen.multi;"));
        Assert.assertTrue(messageContent.contains("// source: test_multi.proto"));
    }

    private static class TestGeneratorContext implements GeneratorContext {
        final Map<String, String> files = new HashMap<>();

        @Override
        public OutputStream open(String filename) throws IOException {
            return new ByteArrayOutputStream() {
                @Override
                public void close() throws IOException {
                    super.close();
                    files.put(filename, this.toString("UTF-8"));
                }
            };
        }
    }
}
