package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.ServiceGenerator;
import com.rubberjam.protobuf.compiler.java.StringUtils;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class ImmutableServiceGenerator extends ServiceGenerator
{
	private final Context context;

	public ImmutableServiceGenerator(ServiceDescriptor descriptor, Context context)
	{
		super(descriptor);
		this.context = context;
	}

	@Override
	public void generate(PrintWriter printer)
	{
		String classname = descriptor.getName();
		printer.println("public static abstract class " + classname);
		printer.println("    implements com.google.protobuf.Service {");
		printer.println("  protected " + classname + "() {}");

		generateInterface(printer);
		generateNewReflectiveServiceMethod(printer);
		generateNewReflectiveBlockingServiceMethod(printer);
		generateAbstractMethods(printer);

		printer.println("  public static final com.google.protobuf.Descriptors.ServiceDescriptor");
		printer.println("      getDescriptor() {");
		printer.println("    return " + context.getNameResolver().getImmutableClassName(descriptor.getFile())
				+ ".getDescriptor().getServices().get(" + descriptor.getIndex() + ");");
		printer.println("  }");

		printer.println("  public final com.google.protobuf.Descriptors.ServiceDescriptor");
		printer.println("      getDescriptorForType() {");
		printer.println("    return getDescriptor();");
		printer.println("  }");

		generateCallMethod(printer);
		generateGetPrototype(true, printer); // Request
		generateGetPrototype(false, printer); // Response
		generateStub(printer);
		generateBlockingStub(printer);

		printer.println("}");
	}

	private void generateInterface(PrintWriter printer)
	{
		printer.println("  public interface Interface {");
		generateAbstractMethods(printer);
		printer.println("  }");
	}

	private void generateNewReflectiveServiceMethod(PrintWriter printer)
	{
		printer.println("  public static com.google.protobuf.Service newReflectiveService(");
		printer.println("      final Interface impl) {");
		printer.println("    return new " + descriptor.getName() + "() {");

		for (MethodDescriptor method : descriptor.getMethods())
		{
			printer.println("      @java.lang.Override");
			generateMethodSignature(printer, method, false);
			printer.println(" {");
			printer.println("        impl." + StringUtils.underscoresToCamelCase(method.getName(), false)
					+ "(controller, request, done);");
			printer.println("      }");
		}

		printer.println("    };");
		printer.println("  }");
	}

	private void generateNewReflectiveBlockingServiceMethod(PrintWriter printer)
	{
		printer.println("  public static com.google.protobuf.BlockingService");
		printer.println("      newReflectiveBlockingService(final BlockingInterface impl) {");
		printer.println("    return new com.google.protobuf.BlockingService() {");

		printer.println("      public final com.google.protobuf.Descriptors.ServiceDescriptor");
		printer.println("          getDescriptorForType() {");
		printer.println("        return getDescriptor();");
		printer.println("      }");

		generateCallBlockingMethod(printer);
		generateGetPrototype(true, printer);
		generateGetPrototype(false, printer);

		printer.println("    };");
		printer.println("  }");
	}

	private void generateAbstractMethods(PrintWriter printer)
	{
		for (MethodDescriptor method : descriptor.getMethods())
		{
			generateMethodSignature(printer, method, true);
			printer.println(";");
		}
	}

	private void generateMethodSignature(PrintWriter printer, MethodDescriptor method, boolean isAbstract)
	{
		String abstractKeyword = isAbstract ? "abstract" : "";
		printer.println(
				"      public " + abstractKeyword + " void " + StringUtils.underscoresToCamelCase(method.getName(), false) + "(");
		printer.println("          com.google.protobuf.RpcController controller,");
		printer.println("          " + context.getNameResolver().getImmutableClassName(method.getInputType()) + " request,");
		printer.println("          com.google.protobuf.RpcCallback<"
				+ context.getNameResolver().getImmutableClassName(method.getOutputType()) + "> done)");
	}

	private void generateCallMethod(PrintWriter printer)
	{
		printer.println("  public final void callMethod(");
		printer.println("      com.google.protobuf.Descriptors.MethodDescriptor method,");
		printer.println("      com.google.protobuf.RpcController controller,");
		printer.println("      com.google.protobuf.Message request,");
		printer.println("      com.google.protobuf.RpcCallback<");
		printer.println("        com.google.protobuf.Message> done) {");
		printer.println("    if (method.getService() != getDescriptor()) {");
		printer.println("      throw new java.lang.IllegalArgumentException(");
		printer.println("        \"Service.callMethod() given method descriptor for wrong \" +");
		printer.println("        \"service type.\");");
		printer.println("    }");
		printer.println("    switch(method.getIndex()) {");

		for (MethodDescriptor method : descriptor.getMethods())
		{
			printer.println("      case " + method.getIndex() + ":");
			printer.println("        this." + StringUtils.underscoresToCamelCase(method.getName(), false) + "(controller, ("
					+ context.getNameResolver().getImmutableClassName(method.getInputType()) + ")request,");
			printer.println("          com.google.protobuf.RpcUtil.<"
					+ context.getNameResolver().getImmutableClassName(method.getOutputType()) + ">specializeCallback(");
			printer.println("            done));");
			printer.println("        return;");
		}

		printer.println("      default:");
		printer.println("        throw new java.lang.AssertionError(\"Can't get here.\");");
		printer.println("    }");
		printer.println("  }");
	}

	private void generateCallBlockingMethod(PrintWriter printer)
	{
		printer.println("  public final com.google.protobuf.Message callBlockingMethod(");
		printer.println("      com.google.protobuf.Descriptors.MethodDescriptor method,");
		printer.println("      com.google.protobuf.RpcController controller,");
		printer.println("      com.google.protobuf.Message request)");
		printer.println("      throws com.google.protobuf.ServiceException {");
		printer.println("    if (method.getService() != getDescriptor()) {");
		printer.println("      throw new java.lang.IllegalArgumentException(");
		printer.println("        \"Service.callBlockingMethod() given method descriptor for \" +");
		printer.println("        \"wrong service type.\");");
		printer.println("    }");
		printer.println("    switch(method.getIndex()) {");

		for (MethodDescriptor method : descriptor.getMethods())
		{
			printer.println("      case " + method.getIndex() + ":");
			printer.println("        return impl." + StringUtils.underscoresToCamelCase(method.getName(), false)
					+ "(controller, (" + context.getNameResolver().getImmutableClassName(method.getInputType()) + ")request);");
		}

		printer.println("      default:");
		printer.println("        throw new java.lang.AssertionError(\"Can't get here.\");");
		printer.println("    }");
		printer.println("  }");
	}

	private void generateGetPrototype(boolean isRequest, PrintWriter printer)
	{
		String typeName = isRequest ? "Request" : "Response";
		printer.println("  public final com.google.protobuf.Message");
		printer.println("      get" + typeName + "Prototype(");
		printer.println("      com.google.protobuf.Descriptors.MethodDescriptor method) {");
		printer.println("    if (method.getService() != getDescriptor()) {");
		printer.println("      throw new java.lang.IllegalArgumentException(");
		printer.println("        \"Service.get" + typeName + "Prototype() given method \" +");
		printer.println("        \"descriptor for wrong service type.\");");
		printer.println("    }");
		printer.println("    switch(method.getIndex()) {");

		for (MethodDescriptor method : descriptor.getMethods())
		{
			String type = isRequest
					? context.getNameResolver().getImmutableClassName(method.getInputType())
					: context.getNameResolver().getImmutableClassName(method.getOutputType());
			printer.println("      case " + method.getIndex() + ":");
			printer.println("        return " + type + ".getDefaultInstance();");
		}

		printer.println("      default:");
		printer.println("        throw new java.lang.AssertionError(\"Can't get here.\");");
		printer.println("    }");
		printer.println("  }");
	}

	private void generateStub(PrintWriter printer)
	{
		printer.println("  public static Stub newStub(");
		printer.println("      com.google.protobuf.RpcChannel channel) {");
		printer.println("    return new Stub(channel);");
		printer.println("  }");

		printer.println("  public static final class Stub extends " + descriptor.getName() + " implements Interface {");
		printer.println("    private Stub(com.google.protobuf.RpcChannel channel) {");
		printer.println("      this.channel = channel;");
		printer.println("    }");
		printer.println("    private final com.google.protobuf.RpcChannel channel;");
		printer.println("    public com.google.protobuf.RpcChannel getChannel() {");
		printer.println("      return channel;");
		printer.println("    }");

		for (MethodDescriptor method : descriptor.getMethods())
		{
			generateMethodSignature(printer, method, false);
			printer.println(" {");
			String outputType = context.getNameResolver().getImmutableClassName(method.getOutputType());
			printer.println("      channel.callMethod(");
			printer.println("        getDescriptor().getMethods().get(" + method.getIndex() + "),");
			printer.println("        controller,");
			printer.println("        request,");
			printer.println("        " + outputType + ".getDefaultInstance(),");
			printer.println("        com.google.protobuf.RpcUtil.generalizeCallback(");
			printer.println("          done,");
			printer.println("          " + outputType + ".class,");
			printer.println("          " + outputType + ".getDefaultInstance()));");
			printer.println("    }");
		}

		printer.println("  }");
	}

	private void generateBlockingStub(PrintWriter printer)
	{
		printer.println("  public static BlockingInterface newBlockingStub(");
		printer.println("      com.google.protobuf.BlockingRpcChannel channel) {");
		printer.println("    return new BlockingStub(channel);");
		printer.println("  }");

		printer.println("  public interface BlockingInterface {");
		for (MethodDescriptor method : descriptor.getMethods())
		{
			generateBlockingMethodSignature(printer, method);
			printer.println(";");
		}
		printer.println("  }");

		printer.println("  private static final class BlockingStub implements BlockingInterface {");
		printer.println("    private BlockingStub(com.google.protobuf.BlockingRpcChannel channel) {");
		printer.println("      this.channel = channel;");
		printer.println("    }");
		printer.println("    private final com.google.protobuf.BlockingRpcChannel channel;");

		for (MethodDescriptor method : descriptor.getMethods())
		{
			generateBlockingMethodSignature(printer, method);
			printer.println(" {");
			String outputType = context.getNameResolver().getImmutableClassName(method.getOutputType());
			printer.println("      return (" + outputType + ") channel.callBlockingMethod(");
			printer.println("        getDescriptor().getMethods().get(" + method.getIndex() + "),");
			printer.println("        controller,");
			printer.println("        request,");
			printer.println("        " + outputType + ".getDefaultInstance());");
			printer.println("    }");
		}

		printer.println("  }");
	}

	private void generateBlockingMethodSignature(PrintWriter printer, MethodDescriptor method)
	{
		printer.println("    public " + context.getNameResolver().getImmutableClassName(method.getOutputType()) + " "
				+ StringUtils.underscoresToCamelCase(method.getName(), false) + "(");
		printer.println("        com.google.protobuf.RpcController controller,");
		printer.println("        " + context.getNameResolver().getImmutableClassName(method.getInputType()) + " request)");
		printer.println("        throws com.google.protobuf.ServiceException");
	}
}
