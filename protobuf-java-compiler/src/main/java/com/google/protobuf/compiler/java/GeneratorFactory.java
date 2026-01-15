package com.google.protobuf.compiler.java;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;

public interface GeneratorFactory {
  MessageGenerator newMessageGenerator(Descriptor descriptor);
  EnumGenerator newEnumGenerator(EnumDescriptor descriptor);
  ExtensionGenerator newExtensionGenerator(FieldDescriptor descriptor);
  ServiceGenerator newServiceGenerator(ServiceDescriptor descriptor);
}
