package com.rubberjam.protobuf.compiler.java.full;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.compiler.java.ClassNameResolver;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.Options;
import com.rubberjam.protobuf.compiler.java.full.ImmutableExtensionGenerator;
import com.rubberjam.protobuf.io.Printer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ImmutableExtensionGeneratorTest {

  @Mock private Context context;
  @Mock private ClassNameResolver nameResolver;
  @Mock private Options options;

  private Printer printer;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    when(context.getNameResolver()).thenReturn(nameResolver);
    when(context.getOptions()).thenReturn(options);
    printer = new Printer(new Printer.Options());
  }

  private FieldDescriptor createExtensionField(String name, DescriptorProtos.FieldDescriptorProto.Type type) throws Exception {
      return createExtensionField(name, type, DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL);
  }

  private FieldDescriptor createExtensionField(String name, DescriptorProtos.FieldDescriptorProto.Type type, DescriptorProtos.FieldDescriptorProto.Label label) throws Exception {
    DescriptorProtos.FileDescriptorProto fileProto = DescriptorProtos.FileDescriptorProto.newBuilder()
        .setName("foo.proto")
        .setPackage("foo")
        .addMessageType(DescriptorProtos.DescriptorProto.newBuilder()
            .setName("BaseMessage")
            .addExtensionRange(DescriptorProtos.DescriptorProto.ExtensionRange.newBuilder()
                .setStart(1)
                .setEnd(10)))
        .addExtension(DescriptorProtos.FieldDescriptorProto.newBuilder()
            .setName(name)
            .setNumber(1)
            .setType(type)
            .setLabel(label)
            .setExtendee(".foo.BaseMessage"))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(fileProto, new FileDescriptor[]{}, true);
    return file.getExtensions().get(0);
  }

  private FieldDescriptor createMessageExtensionField(String name, DescriptorProtos.FieldDescriptorProto.Label label) throws Exception {
      DescriptorProtos.FileDescriptorProto fileProto = DescriptorProtos.FileDescriptorProto.newBuilder()
          .setName("foo.proto")
          .setPackage("foo")
          .addMessageType(DescriptorProtos.DescriptorProto.newBuilder()
              .setName("BaseMessage")
              .addExtensionRange(DescriptorProtos.DescriptorProto.ExtensionRange.newBuilder()
                  .setStart(1)
                  .setEnd(10)))
          .addMessageType(DescriptorProtos.DescriptorProto.newBuilder().setName("SomeMessage"))
          .addExtension(DescriptorProtos.FieldDescriptorProto.newBuilder()
              .setName(name)
              .setNumber(1)
              .setType(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE)
              .setLabel(label)
              .setTypeName(".foo.SomeMessage")
              .setExtendee(".foo.BaseMessage"))
          .build();

      FileDescriptor file = FileDescriptor.buildFrom(fileProto, new FileDescriptor[]{}, true);
      return file.getExtensions().get(0);
  }

  private FieldDescriptor createNestedExtensionField(String name, DescriptorProtos.FieldDescriptorProto.Type type) throws Exception {
      DescriptorProtos.FileDescriptorProto fileProto = DescriptorProtos.FileDescriptorProto.newBuilder()
          .setName("foo.proto")
          .setPackage("foo")
          .addMessageType(DescriptorProtos.DescriptorProto.newBuilder()
              .setName("BaseMessage")
              .addExtensionRange(DescriptorProtos.DescriptorProto.ExtensionRange.newBuilder()
                  .setStart(1)
                  .setEnd(10)))
          .addMessageType(DescriptorProtos.DescriptorProto.newBuilder()
              .setName("Container")
              .addExtension(DescriptorProtos.FieldDescriptorProto.newBuilder()
                  .setName(name)
                  .setNumber(1)
                  .setType(type)
                  .setExtendee(".foo.BaseMessage")))
          .build();

      FileDescriptor file = FileDescriptor.buildFrom(fileProto, new FileDescriptor[]{}, true);
      return file.getMessageTypes().get(1).getExtensions().get(0);
    }

  @Test
  public void testGeneratePrimitiveExtension() throws Exception {
    FieldDescriptor field = createExtensionField("bar", DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32);
    when(nameResolver.getClassName(any(Descriptor.class), eq(true))).thenAnswer(invocation -> {
        Descriptor d = invocation.getArgument(0);
        if ("BaseMessage".equals(d.getName())) return "com.foo.BaseMessage";
        return "Unknown";
    });

    ImmutableExtensionGenerator generator = new ImmutableExtensionGenerator(field, context);
    generator.generate(printer);

    String output = printer.toString();
    assertTrue(output.contains("public static final"));
    assertTrue(output.contains("com.google.protobuf.GeneratedMessage.GeneratedExtension<"));
    assertTrue(output.contains("com.foo.BaseMessage,"));
    assertTrue(output.contains("java.lang.Integer> bar;"));
  }

  @Test
  public void testGenerateRepeatedExtension() throws Exception {
      FieldDescriptor field = createExtensionField("bar", DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32, DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED);
      when(nameResolver.getClassName(any(Descriptor.class), eq(true))).thenAnswer(invocation -> {
          Descriptor d = invocation.getArgument(0);
          if ("BaseMessage".equals(d.getName())) return "com.foo.BaseMessage";
          return "Unknown";
      });

      ImmutableExtensionGenerator generator = new ImmutableExtensionGenerator(field, context);
      generator.generate(printer);

      String output = printer.toString();
      assertTrue(output.contains("java.util.List<java.lang.Integer>> bar;"));
  }

  @Test
  public void testGenerateMessageExtension() throws Exception {
      FieldDescriptor field = createMessageExtensionField("bar", DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL);
      when(nameResolver.getClassName(any(Descriptor.class), eq(true))).thenAnswer(invocation -> {
          Descriptor d = invocation.getArgument(0);
          if ("BaseMessage".equals(d.getName())) return "com.foo.BaseMessage";
          if ("SomeMessage".equals(d.getName())) return "com.foo.SomeMessage";
          return "Unknown";
      });

      ImmutableExtensionGenerator generator = new ImmutableExtensionGenerator(field, context);
      generator.generate(printer);

      String output = printer.toString();
      assertTrue(output.contains("com.foo.SomeMessage> bar;"));
  }

  @Test
  public void testGenerateRepeatedMessageExtension() throws Exception {
      FieldDescriptor field = createMessageExtensionField("bar", DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED);
      when(nameResolver.getClassName(any(Descriptor.class), eq(true))).thenAnswer(invocation -> {
          Descriptor d = invocation.getArgument(0);
          if ("BaseMessage".equals(d.getName())) return "com.foo.BaseMessage";
          if ("SomeMessage".equals(d.getName())) return "com.foo.SomeMessage";
          return "Unknown";
      });

      ImmutableExtensionGenerator generator = new ImmutableExtensionGenerator(field, context);
      generator.generate(printer);

      String output = printer.toString();
      assertTrue(output.contains("java.util.List<com.foo.SomeMessage>> bar;"));
  }

  @Test
  public void testGenerateInitCodePrimitive() throws Exception {
    FieldDescriptor field = createExtensionField("bar", DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32);
    when(nameResolver.getClassName(any(Descriptor.class), eq(true))).thenAnswer(invocation -> {
        Descriptor d = invocation.getArgument(0);
        if ("BaseMessage".equals(d.getName())) return "com.foo.BaseMessage";
        return "Unknown";
    });

    ImmutableExtensionGenerator generator = new ImmutableExtensionGenerator(field, context);
    generator.generateNonNestedInitializationCode(printer);

    String output = printer.toString();
    assertTrue(output.contains("bar = com.google.protobuf.GeneratedMessage.newFileScopedGeneratedExtension("));
    assertTrue(output.contains("java.lang.Integer.class,"));
    assertTrue(output.contains("null);"));
  }

  @Test
  public void testGenerateNestedExtensionInitCode() throws Exception {
      FieldDescriptor field = createNestedExtensionField("bar", DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32);
      when(nameResolver.getClassName(any(Descriptor.class), eq(true))).thenAnswer(invocation -> {
          Descriptor d = invocation.getArgument(0);
          if ("Container".equals(d.getName())) return "com.foo.Container";
          if ("BaseMessage".equals(d.getName())) return "com.foo.BaseMessage";
          return "Unknown";
      });

      ImmutableExtensionGenerator generator = new ImmutableExtensionGenerator(field, context);
      generator.generateNonNestedInitializationCode(printer);

      String output = printer.toString();
      assertTrue(output.contains("com.foo.Container.bar = com.google.protobuf.GeneratedMessage.newMessageScopedGeneratedExtension("));
      assertTrue(output.contains("com.foo.Container.getDefaultInstance(),"));
      assertTrue(output.contains("0,"));
      assertTrue(output.contains("java.lang.Integer.class,"));
      assertTrue(output.contains("null);"));
  }

  @Test
  public void testGenerateRegistrationCode() throws Exception {
      FieldDescriptor field = createExtensionField("bar", DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32);

      ImmutableExtensionGenerator generator = new ImmutableExtensionGenerator(field, context);
      generator.generateRegistrationCode(printer);

      String output = printer.toString();
      assertTrue(output.contains("registry.add(bar);"));
  }

  @Test
  public void testGenerateNestedRegistrationCode() throws Exception {
      FieldDescriptor field = createNestedExtensionField("bar", DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32);
      when(nameResolver.getClassName(any(Descriptor.class), eq(true))).thenReturn("com.foo.Container");

      ImmutableExtensionGenerator generator = new ImmutableExtensionGenerator(field, context);
      generator.generateRegistrationCode(printer);

      String output = printer.toString();
      assertTrue(output.contains("registry.add(com.foo.Container.bar);"));
  }
}
