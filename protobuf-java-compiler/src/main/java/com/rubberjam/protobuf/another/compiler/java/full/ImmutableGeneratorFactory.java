package com.rubberjam.protobuf.another.compiler.java.full;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.rubberjam.protobuf.another.compiler.java.Context;
import com.rubberjam.protobuf.another.compiler.java.GeneratorFactory;
import com.rubberjam.protobuf.another.compiler.java.Helpers;

/**
 * Immutable generator factory.
 * Ported from java/full/generator_factory.cc.
 */
public class ImmutableGeneratorFactory implements GeneratorFactory {

  private final Context context;

  public ImmutableGeneratorFactory(Context context) {
    this.context = context;
  }

  @Override
  public MessageGenerator newMessageGenerator(Descriptor descriptor) {
    if (context.hasGeneratedMethods(descriptor)) {
      return new ImmutableMessageGenerator(descriptor, context);
    } else {
      // TODO: Implement ImmutableMessageLiteGenerator (Phase 10)
      throw new UnsupportedOperationException("Lite generator not implemented yet.");
      // return new ImmutableMessageLiteGenerator(descriptor, context);
    }
  }

  @Override
  public EnumGenerator newEnumGenerator(EnumDescriptor descriptor) {
      // EnumGenerator handles both? Or we need EnumLiteGenerator?
      // C++ uses EnumGenerator for both, passing immutable_api=true.
      return new com.rubberjam.protobuf.another.compiler.java.full.EnumGenerator(descriptor, true, context);
  }

  @Override
  public ExtensionGenerator newExtensionGenerator(FieldDescriptor descriptor) {
    if (context.hasGeneratedMethods(descriptor.getFile())) { // ExtensionGenerator checks file
      return new ImmutableExtensionGenerator(descriptor, context);
    } else {
      // TODO: Implement ImmutableExtensionLiteGenerator (Phase 10)
      throw new UnsupportedOperationException("Lite extension generator not implemented yet.");
      // return new ImmutableExtensionLiteGenerator(descriptor, context);
    }
  }

  @Override
  public ServiceGenerator newServiceGenerator(ServiceDescriptor descriptor) {
    return new ImmutableServiceGenerator(descriptor, context);
  }
}
