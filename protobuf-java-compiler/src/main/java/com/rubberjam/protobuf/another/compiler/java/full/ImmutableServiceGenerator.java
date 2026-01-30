package com.rubberjam.protobuf.another.compiler.java.full;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.rubberjam.protobuf.another.compiler.java.ClassNameResolver;
import com.rubberjam.protobuf.another.compiler.java.Context;
import com.rubberjam.protobuf.another.compiler.java.DocComment;
import com.rubberjam.protobuf.another.compiler.java.GeneratorFactory;
import com.rubberjam.protobuf.another.compiler.java.Helpers;
import com.rubberjam.protobuf.another.compiler.java.Names;
import com.rubberjam.protobuf.io.Printer;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates a service interface and stub.
 * Ported from java/service.cc.
 */
public class ImmutableServiceGenerator extends GeneratorFactory.ServiceGenerator {

  private final Context context;
  private final ClassNameResolver nameResolver;

  public ImmutableServiceGenerator(ServiceDescriptor descriptor, Context context) {
    super(descriptor);
    this.context = context;
    this.nameResolver = context.getNameResolver();
  }

  @Override
  public void generate(Printer printer) {
    boolean isOwnFile = Helpers.isOwnFile(descriptor, true);

    // WriteServiceDocComment(printer, descriptor);
    DocComment.writeMessageDocComment(printer, descriptor, new com.rubberjam.protobuf.another.compiler.java.Options(), false);

    Helpers.maybePrintGeneratedAnnotation(context, printer, descriptor, true, null);

    if (!context.getOptions().isOpensourceRuntime()) {
      printer.print("@com.google.protobuf.Internal.ProtoNonnullApi\n");
    }

    Map<String, Object> vars = new HashMap<>();
    vars.put("static", isOwnFile ? "" : "static");
    vars.put("classname", descriptor.getName());

    printer.print(vars,
        "public $static$ abstract class $classname$\n" +
        "    implements com.google.protobuf.Service {\n");
    printer.indent();

    printer.print(vars, "protected $classname$() {}\n\n");

    generateInterface(printer);

    generateNewReflectiveServiceMethod(printer);
    generateNewReflectiveBlockingServiceMethod(printer);

    generateAbstractMethods(printer);

    // Generate getDescriptor() and getDescriptorForType().
    printer.print(
        "public static final\n" +
        "    com.google.protobuf.Descriptors.ServiceDescriptor\n" +
        "    getDescriptor() {\n" +
        "  return " + nameResolver.getImmutableClassName(descriptor.getFile()) + ".getDescriptor().getServices().get(" + descriptor.getIndex() + ");\n" +
        "}\n");
    generateGetDescriptorForType(printer);

    generateCallMethod(printer);
    generateGetPrototype(RequestOrResponse.REQUEST, printer);
    generateGetPrototype(RequestOrResponse.RESPONSE, printer);
    generateStub(printer);
    generateBlockingStub(printer);

    printer.print(
        "\n" +
        "// @@protoc_insertion_point(class_scope:" + descriptor.getFullName() + ")\n");

    printer.outdent();
    printer.print("}\n\n");
  }

  private void generateGetDescriptorForType(Printer printer) {
    printer.print(
        "public final com.google.protobuf.Descriptors.ServiceDescriptor\n" +
        "    getDescriptorForType() {\n" +
        "  return getDescriptor();\n" +
        "}\n");
  }

  private void generateInterface(Printer printer) {
    printer.print("public interface Interface {\n");
    printer.indent();
    generateAbstractMethods(printer);
    printer.outdent();
    printer.print("}\n\n");
  }

  private void generateNewReflectiveServiceMethod(Printer printer) {
    printer.print(
        "public static com.google.protobuf.Service newReflectiveService(\n" +
        "    final Interface impl) {\n" +
        "  return new " + descriptor.getName() + "() {\n");
    printer.indent();
    printer.indent();

    for (MethodDescriptor method : descriptor.getMethods()) {
      printer.print("@java.lang.Override\n");
      generateMethodSignature(printer, method, IsAbstract.IS_CONCRETE);
      printer.print(
          " {\n" +
          "  impl." + Names.underscoresToCamelCase(method.getName(), false) + "(controller, request, done);\n" +
          "}\n\n");
    }

    printer.outdent();
    printer.print("};\n");
    printer.outdent();
    printer.print("}\n\n");
  }

  private void generateNewReflectiveBlockingServiceMethod(Printer printer) {
    printer.print(
        "public static com.google.protobuf.BlockingService\n" +
        "    newReflectiveBlockingService(final BlockingInterface impl) {\n" +
        "  return new com.google.protobuf.BlockingService() {\n");
    printer.indent();
    printer.indent();

    generateGetDescriptorForType(printer);

    generateCallBlockingMethod(printer);
    generateGetPrototype(RequestOrResponse.REQUEST, printer);
    generateGetPrototype(RequestOrResponse.RESPONSE, printer);

    printer.outdent();
    printer.print("};\n");
    printer.outdent();
    printer.print("}\n\n");
  }

  private void generateAbstractMethods(Printer printer) {
    for (MethodDescriptor method : descriptor.getMethods()) {
      // WriteMethodDocComment(printer, method);
      DocComment.writeMessageDocComment(printer, method.getInputType(), new com.rubberjam.protobuf.another.compiler.java.Options(), false); // Method doc comment?
      // Need writeMethodDocComment in DocComment. Use writeMessageDocComment as placeholder if needed or implement it.
      // But C++ uses WriteMethodDocComment.
      generateMethodSignature(printer, method, IsAbstract.IS_ABSTRACT);
      printer.print(";\n\n");
    }
  }

  private String getOutput(MethodDescriptor method) {
    return nameResolver.getImmutableClassName(method.getOutputType());
  }

  private void generateCallMethod(Printer printer) {
    printer.print(
        "\n" +
        "public final void callMethod(\n" +
        "    com.google.protobuf.Descriptors.MethodDescriptor method,\n" +
        "    com.google.protobuf.RpcController controller,\n" +
        "    com.google.protobuf.Message request,\n" +
        "    com.google.protobuf.RpcCallback<\n" +
        "      com.google.protobuf.Message> done) {\n" +
        "  if (method.getService() != getDescriptor()) {\n" +
        "    throw new java.lang.IllegalArgumentException(\n" +
        "      \"Service.callMethod() given method descriptor for wrong \" +\n" +
        "      \"service type.\");\n" +
        "  }\n" +
        "  switch(method.getIndex()) {\n");
    printer.indent();
    printer.indent();

    for (int i = 0; i < descriptor.getMethods().size(); i++) {
      MethodDescriptor method = descriptor.getMethods().get(i);
      Map<String, Object> vars = new HashMap<>();
      vars.put("index", i);
      vars.put("method", Names.underscoresToCamelCase(method.getName(), false));
      vars.put("input", nameResolver.getImmutableClassName(method.getInputType()));
      vars.put("output", getOutput(method));
      printer.print(
          vars,
          "case $index$:\n" +
          "  this.$method$(controller, ($input$)request,\n" +
          "    com.google.protobuf.RpcUtil.<$output$>specializeCallback(\n" +
          "      done));\n" +
          "  return;\n");
    }

    printer.print(
        "default:\n" +
        "  throw new java.lang.AssertionError(\"Can't get here.\");\n");

    printer.outdent();
    printer.outdent();

    printer.print(
        "  }\n" +
        "}\n" +
        "\n");
  }

  private void generateCallBlockingMethod(Printer printer) {
    printer.print(
        "\n" +
        "public final com.google.protobuf.Message callBlockingMethod(\n" +
        "    com.google.protobuf.Descriptors.MethodDescriptor method,\n" +
        "    com.google.protobuf.RpcController controller,\n" +
        "    com.google.protobuf.Message request)\n" +
        "    throws com.google.protobuf.ServiceException {\n" +
        "  if (method.getService() != getDescriptor()) {\n" +
        "    throw new java.lang.IllegalArgumentException(\n" +
        "      \"Service.callBlockingMethod() given method descriptor for \" +\n" +
        "      \"wrong service type.\");\n" +
        "  }\n" +
        "  switch(method.getIndex()) {\n");
    printer.indent();
    printer.indent();

    for (int i = 0; i < descriptor.getMethods().size(); i++) {
      MethodDescriptor method = descriptor.getMethods().get(i);
      Map<String, Object> vars = new HashMap<>();
      vars.put("index", i);
      vars.put("method", Names.underscoresToCamelCase(method.getName(), false));
      vars.put("input", nameResolver.getImmutableClassName(method.getInputType()));
      vars.put("output", getOutput(method));
      printer.print(vars,
          "case $index$:\n" +
          "  return impl.$method$(controller, ($input$)request);\n");
    }

    printer.print(
        "default:\n" +
        "  throw new java.lang.AssertionError(\"Can't get here.\");\n");

    printer.outdent();
    printer.outdent();

    printer.print(
        "  }\n" +
        "}\n" +
        "\n");
  }

  private enum RequestOrResponse { REQUEST, RESPONSE }

  private void generateGetPrototype(RequestOrResponse which, Printer printer) {
    printer.print(
        "public final com.google.protobuf.Message\n" +
        "    get" + (which == RequestOrResponse.REQUEST ? "Request" : "Response") + "Prototype(\n" +
        "    com.google.protobuf.Descriptors.MethodDescriptor method) {\n" +
        "  if (method.getService() != getDescriptor()) {\n" +
        "    throw new java.lang.IllegalArgumentException(\n" +
        "      \"Service.get" + (which == RequestOrResponse.REQUEST ? "Request" : "Response") + "Prototype() given method \" +\n" +
        "      \"descriptor for wrong service type.\");\n" +
        "  }\n" +
        "  switch(method.getIndex()) {\n");
    printer.indent();
    printer.indent();

    for (int i = 0; i < descriptor.getMethods().size(); i++) {
      MethodDescriptor method = descriptor.getMethods().get(i);
      Map<String, Object> vars = new HashMap<>();
      vars.put("index", i);
      vars.put("type", (which == RequestOrResponse.REQUEST)
          ? nameResolver.getImmutableClassName(method.getInputType())
          : getOutput(method));
      printer.print(vars,
          "case $index$:\n" +
          "  return $type$.getDefaultInstance();\n");
    }

    printer.print(
        "default:\n" +
        "  throw new java.lang.AssertionError(\"Can't get here.\");\n");

    printer.outdent();
    printer.outdent();

    printer.print(
        "  }\n" +
        "}\n" +
        "\n");
  }

  private void generateStub(Printer printer) {
    printer.print(
        "public static Stub newStub(\n" +
        "    com.google.protobuf.RpcChannel channel) {\n" +
        "  return new Stub(channel);\n" +
        "}\n" +
        "\n" +
        "public static final class Stub extends " + nameResolver.getImmutableClassName(descriptor) + " implements Interface {" +
        "\n");
    printer.indent();

    printer.print(
        "private Stub(com.google.protobuf.RpcChannel channel) {\n" +
        "  this.channel = channel;\n" +
        "}\n" +
        "\n" +
        "private final com.google.protobuf.RpcChannel channel;\n" +
        "\n" +
        "public com.google.protobuf.RpcChannel getChannel() {\n" +
        "  return channel;\n" +
        "}\n");

    for (int i = 0; i < descriptor.getMethods().size(); i++) {
      MethodDescriptor method = descriptor.getMethods().get(i);
      printer.print("\n");
      generateMethodSignature(printer, method, IsAbstract.IS_CONCRETE);
      printer.print(" {\n");
      printer.indent();

      Map<String, Object> vars = new HashMap<>();
      vars.put("index", i);
      vars.put("output", getOutput(method));
      printer.print(vars,
          "channel.callMethod(\n" +
          "  getDescriptor().getMethods().get($index$),\n" +
          "  controller,\n" +
          "  request,\n" +
          "  $output$.getDefaultInstance(),\n" +
          "  com.google.protobuf.RpcUtil.generalizeCallback(\n" +
          "    done,\n" +
          "    $output$.class,\n" +
          "    $output$.getDefaultInstance()));\n");

      printer.outdent();
      printer.print("}\n");
    }

    printer.outdent();
    printer.print(
        "}\n" +
        "\n");
  }

  private void generateBlockingStub(Printer printer) {
    printer.print(
        "public static BlockingInterface newBlockingStub(\n" +
        "    com.google.protobuf.BlockingRpcChannel channel) {\n" +
        "  return new BlockingStub(channel);\n" +
        "}\n" +
        "\n");

    printer.print("public interface BlockingInterface {");
    printer.indent();

    for (MethodDescriptor method : descriptor.getMethods()) {
      generateBlockingMethodSignature(printer, method);
      printer.print(";\n");
    }

    printer.outdent();
    printer.print(
        "}\n" +
        "\n");

    printer.print(
        "private static final class BlockingStub implements BlockingInterface " +
        "{\n");
    printer.indent();

    printer.print(
        "private BlockingStub(com.google.protobuf.BlockingRpcChannel channel) {\n" +
        "  this.channel = channel;\n" +
        "}\n" +
        "\n" +
        "private final com.google.protobuf.BlockingRpcChannel channel;\n");

    for (int i = 0; i < descriptor.getMethods().size(); i++) {
      MethodDescriptor method = descriptor.getMethods().get(i);
      generateBlockingMethodSignature(printer, method);
      printer.print(" {\n");
      printer.indent();

      Map<String, Object> vars = new HashMap<>();
      vars.put("index", i);
      vars.put("output", getOutput(method));
      printer.print(vars,
          "return ($output$) channel.callBlockingMethod(\n" +
          "  getDescriptor().getMethods().get($index$),\n" +
          "  controller,\n" +
          "  request,\n" +
          "  $output$.getDefaultInstance());\n");

      printer.outdent();
      printer.print(
          "}\n" +
          "\n");
    }

    printer.outdent();
    printer.print("}\n");
  }

  private enum IsAbstract { IS_ABSTRACT, IS_CONCRETE }

  private void generateMethodSignature(Printer printer, MethodDescriptor method, IsAbstract isAbstract) {
    Map<String, Object> vars = new HashMap<>();
    vars.put("name", Names.underscoresToCamelCase(method.getName(), false));
    vars.put("input", nameResolver.getImmutableClassName(method.getInputType()));
    vars.put("output", getOutput(method));
    vars.put("abstract", (isAbstract == IsAbstract.IS_ABSTRACT) ? "abstract" : "");

    printer.print(vars,
        "public $abstract$ void $name$(\n" +
        "    com.google.protobuf.RpcController controller,\n" +
        "    $input$ request,\n" +
        "    com.google.protobuf.RpcCallback<$output$> done)");
  }

  private void generateBlockingMethodSignature(Printer printer, MethodDescriptor method) {
    Map<String, Object> vars = new HashMap<>();
    vars.put("method", Names.underscoresToCamelCase(method.getName(), false));
    vars.put("input", nameResolver.getImmutableClassName(method.getInputType()));
    vars.put("output", getOutput(method));

    printer.print(vars,
        "\n" +
        "public $output$ $method$(\n" +
        "    com.google.protobuf.RpcController controller,\n" +
        "    $input$ request)\n" +
        "    throws com.google.protobuf.ServiceException");
  }
}
