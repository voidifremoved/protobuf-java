package com.rubberjam.protobuf.another.compiler.java;

import org.junit.Test;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.rubberjam.protobuf.io.Printer;

public class GeneratorFactoryTest {

  @Test
  public void testInterfaceDef() {
    // Just verify the interface exists and methods are callable
    GeneratorFactory factory = new GeneratorFactory() {
        @Override public MessageGenerator newMessageGenerator(Descriptor descriptor) { return null; }
        @Override public EnumGenerator newEnumGenerator(EnumDescriptor descriptor) { return null; }
        @Override public ExtensionGenerator newExtensionGenerator(FieldDescriptor descriptor) { return null; }
        @Override public ServiceGenerator newServiceGenerator(ServiceDescriptor descriptor) { return null; }
    };
  }
}
