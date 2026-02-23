package com.rubberjam.protobuf.compiler.java.lite;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.GeneratorFactory;

/**
 * Immutable Lite generator factory.
 * Ported from java/lite/generator_factory.cc.
 */
public class ImmutableLiteGeneratorFactory implements GeneratorFactory {

  private final Context context;

  public ImmutableLiteGeneratorFactory(Context context) {
    this.context = context;
  }

  @Override
  public MessageGenerator newMessageGenerator(Descriptor descriptor) {
    return new ImmutableMessageLiteGenerator(descriptor, context);
  }

  @Override
  public EnumGenerator newEnumGenerator(EnumDescriptor descriptor) {
    return new com.rubberjam.protobuf.compiler.java.full.EnumGenerator(descriptor, true, context);
  }

  @Override
  public ExtensionGenerator newExtensionGenerator(FieldDescriptor descriptor) {
    return new ImmutableExtensionLiteGenerator(descriptor, context);
  }

  @Override
  public ServiceGenerator newServiceGenerator(ServiceDescriptor descriptor) {
    // Lite runtime doesn't support services.
    return null;
  }
}
