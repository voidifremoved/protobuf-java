package com.google.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.compiler.java.Context;
import com.google.protobuf.compiler.java.EnumGenerator;
import com.google.protobuf.compiler.java.ExtensionGenerator;
import com.google.protobuf.compiler.java.MessageGenerator;
import com.google.protobuf.compiler.java.ServiceGenerator;
import java.io.PrintWriter;

public class GeneratorFactory implements com.google.protobuf.compiler.java.GeneratorFactory {
  private final Context context;

  public GeneratorFactory(Context context) {
    this.context = context;
  }

  @Override
  public MessageGenerator newMessageGenerator(Descriptor descriptor) {
    return new ImmutableMessageGenerator(descriptor, context);
  }

  @Override
  public EnumGenerator newEnumGenerator(EnumDescriptor descriptor) {
    return new EnumGenerator() {
         @Override
         public void generate(PrintWriter printer) {
             printer.println("// TODO: EnumGenerator");
         }
    };
  }

  @Override
  public ExtensionGenerator newExtensionGenerator(FieldDescriptor descriptor) {
    return new ExtensionGenerator() {
         @Override
         public void generate(PrintWriter printer) {
             printer.println("// TODO: ExtensionGenerator");
         }
         @Override
         public int generateNonNestedInitializationCode(PrintWriter printer) { return 0; }
         @Override
         public int generateRegistrationCode(PrintWriter printer) { return 0; }
    };
  }

  @Override
  public ServiceGenerator newServiceGenerator(ServiceDescriptor descriptor) {
    return new ServiceGenerator(descriptor) {
         @Override
         public void generate(PrintWriter printer) {
             printer.println("// TODO: ServiceGenerator");
         }
    };
  }
}
