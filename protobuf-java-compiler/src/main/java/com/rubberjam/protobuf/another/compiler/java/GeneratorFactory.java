package com.rubberjam.protobuf.another.compiler.java;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.rubberjam.protobuf.io.Printer;

/**
 * Factory for creating generators. Ported from generator_factory.h.
 */
public interface GeneratorFactory {

  MessageGenerator newMessageGenerator(Descriptor descriptor);

  EnumGenerator newEnumGenerator(EnumDescriptor descriptor);

  ExtensionGenerator newExtensionGenerator(FieldDescriptor descriptor);

  ServiceGenerator newServiceGenerator(ServiceDescriptor descriptor);

  abstract class MessageGenerator {
    protected final Descriptor descriptor;

    public MessageGenerator(Descriptor descriptor) {
      this.descriptor = descriptor;
    }

    public abstract void generateStaticVariables(Printer printer, int[] bytecodeEstimate);

    public abstract int generateStaticVariableInitializers(Printer printer);

    public abstract void generate(Printer printer);

    public abstract void generateInterface(Printer printer);

    public abstract void generateExtensionRegistrationCode(Printer printer);
  }

  abstract class EnumGenerator {
    protected final EnumDescriptor descriptor;

    public EnumGenerator(EnumDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public abstract void generate(Printer printer);
  }

  abstract class ExtensionGenerator {
    protected final FieldDescriptor descriptor;

    public ExtensionGenerator(FieldDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public abstract void generate(Printer printer);

    public abstract int generateNonNestedInitializationCode(Printer printer);

    public abstract int generateRegistrationCode(Printer printer);
  }

  abstract class ServiceGenerator {
    protected final ServiceDescriptor descriptor;

    public ServiceGenerator(ServiceDescriptor descriptor) {
      this.descriptor = descriptor;
    }

    public abstract void generate(Printer printer);
  }
}
