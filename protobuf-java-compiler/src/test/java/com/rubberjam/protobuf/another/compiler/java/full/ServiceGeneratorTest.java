package com.rubberjam.protobuf.another.compiler.java.full;

import static org.junit.Assert.*;

import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.rubberjam.protobuf.another.compiler.java.Context;
import com.rubberjam.protobuf.another.compiler.java.Options;
import com.rubberjam.protobuf.io.Printer;
import org.junit.Before;
import org.junit.Test;

public class ServiceGeneratorTest {
  private Context context;
  private ServiceDescriptor descriptor;
  private Printer printer;
  private ImmutableServiceGenerator generator;

  @Before
  public void setUp() throws Exception {
    FileDescriptorProto proto = FileDescriptorProto.newBuilder()
        .setName("service.proto")
        .setPackage("service")
        .addMessageType(DescriptorProto.newBuilder().setName("Request"))
        .addMessageType(DescriptorProto.newBuilder().setName("Response"))
        .addService(ServiceDescriptorProto.newBuilder()
            .setName("TestService")
            .addMethod(MethodDescriptorProto.newBuilder()
                .setName("DoSomething")
                .setInputType(".service.Request")
                .setOutputType(".service.Response")))
        .build();

    FileDescriptor file = FileDescriptor.buildFrom(proto, new FileDescriptor[] {});
    descriptor = file.findServiceByName("TestService");
    assertNotNull("Descriptor should not be null", descriptor);

    context = new Context(file, new Options());
    printer = new Printer(new Printer.Options());

    generator = new ImmutableServiceGenerator(descriptor, context);
  }

  @Test
  public void testGenerate() {
    generator.generate(printer);
    String output = printer.toString();

    assertTrue("MISSING_CLASS_DEF", output.contains("public static abstract class TestService"));

    assertTrue("MISSING_INTERFACE", output.contains("public interface Interface {"));
    assertTrue("MISSING_DO_SOMETHING", output.contains("public abstract void doSomething("));
    assertTrue("MISSING_STUB_DEF", output.contains("class Stub"));
    assertTrue("Output missing Stub extends", output.contains("extends service.ServiceProto.TestService"));
    assertTrue("Output missing Stub implements", output.contains("implements Interface"));

    // Splitting this check because newline characters might be inconsistent or depending on Printer implementation
    assertTrue("Output missing getDescriptor signature", output.contains("getDescriptor()"));
    assertTrue("Output missing getDescriptor body", output.contains("return service.ServiceProto.getDescriptor().getServices().get(0);"));
  }
}
