package com.rubberjam.protobuf.compiler.java.proto2.speed;

import java.io.PrintWriter;

import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.rubberjam.protobuf.compiler.java.JavaContext;
import com.rubberjam.protobuf.compiler.java.ServiceGenerator;
import com.rubberjam.protobuf.compiler.java.StringUtils;

public class ImmutableServiceGenerator extends ServiceGenerator
{
	private final JavaContext context;

	public ImmutableServiceGenerator(ServiceDescriptor descriptor, JavaContext context)
	{
		super(descriptor);
		this.context = context;
	}

	@Override
	public void generate(PrintWriter printer)
	{
		String classname = descriptor.getName();
		com.rubberjam.protobuf.compiler.java.DocComment.writeServiceDocComment(printer, descriptor, context, "  ");
		printer.println("  public static abstract class " + classname);
		printer.println("      implements com.google.protobuf.Service {");
		printer.println("    protected " + classname + "() {}");
		printer.println();

		generateInterface(printer);
		printer.println();
		generateNewReflectiveServiceMethod(printer);
		printer.println();
		generateNewReflectiveBlockingServiceMethod(printer);
		printer.println();
		generateAbstractMethods(printer, "    ");
		printer.println();

		printer.println("    public static final");
		printer.println("        com.google.protobuf.Descriptors.ServiceDescriptor");
		printer.println("        getDescriptor() {");
		printer.println("      return " + context.getNameResolver().getImmutableClassName(descriptor.getFile())
				+ ".getDescriptor().getService(" + descriptor.getIndex() + ");");
		printer.println("    }");

		printer.println("    public final com.google.protobuf.Descriptors.ServiceDescriptor");
		printer.println("        getDescriptorForType() {");
		printer.println("      return getDescriptor();");
		printer.println("    }");
		printer.println();

		generateCallMethod(printer);
		printer.println();
		generateGetPrototype(true, printer); // Request
		printer.println();
		generateGetPrototype(false, printer); // Response
		printer.println();
		generateStub(printer);
		printer.println();
		generateBlockingStub(printer);
		printer.println();

		printer.println("    // @@protoc_insertion_point(class_scope:" + descriptor.getFullName() + ")");
		printer.println("  }");
		printer.println();
	}

	private void generateInterface(PrintWriter printer)
	{
		printer.println("    public interface Interface {");
		generateAbstractMethods(printer, "      ");
		printer.println();
		printer.println("    }");
	}

	private void generateNewReflectiveServiceMethod(PrintWriter printer)
	{
		printer.println("    public static com.google.protobuf.Service newReflectiveService(");
		printer.println("        final Interface impl) {");
		printer.println("      return new " + descriptor.getName() + "() {");

		boolean first = true;
		for (MethodDescriptor method : descriptor.getMethods())
		{
			if (!first)
			{
				printer.println();
			}
			first = false;
			printer.println("        @java.lang.Override");
			generateMethodSignature(printer, method, false, "        ");
			printer.println(" {");
			printer.println("          impl." + StringUtils.underscoresToCamelCase(method.getName(), false)
					+ "(controller, request, done);");
			printer.println("        }");
		}
		printer.println();
		printer.println("      };");
		printer.println("    }");
	}

	private void generateNewReflectiveBlockingServiceMethod(PrintWriter printer)
	{
		printer.println("    public static com.google.protobuf.BlockingService");
		printer.println("        newReflectiveBlockingService(final BlockingInterface impl) {");
		printer.println("      return new com.google.protobuf.BlockingService() {");

		printer.println("        public final com.google.protobuf.Descriptors.ServiceDescriptor");
		printer.println("            getDescriptorForType() {");
		printer.println("          return getDescriptor();");
		printer.println("        }");
		printer.println();

		generateCallBlockingMethod(printer, "        ");
		printer.println();
		generateGetPrototype(true, printer, "        ");
		printer.println();
		generateGetPrototype(false, printer, "        ");
		printer.println();

		printer.println("      };");
		printer.println("    }");
	}

	private void generateAbstractMethods(PrintWriter printer, String indent)
	{
		boolean first = true;
		for (MethodDescriptor method : descriptor.getMethods())
		{
			if (!first)
			{
				printer.println();
			}
			first = false;
			com.rubberjam.protobuf.compiler.java.DocComment.writeMethodDocComment(printer, method, context, indent);
			generateMethodSignature(printer, method, true, indent);
			printer.println(";");
		}
	}

	private void generateMethodSignature(PrintWriter printer, MethodDescriptor method, boolean isAbstract, String indent)
	{
		String abstractKeyword = isAbstract ? "abstract" : "";
		printer.println(indent + "public " + abstractKeyword + " void " + StringUtils.underscoresToCamelCase(method.getName(), false) + "(");
		printer.println(indent + "    com.google.protobuf.RpcController controller,");
		printer.println(indent + "    " + context.getNameResolver().getImmutableClassName(method.getInputType()) + " request,");
		printer.print(indent + "    com.google.protobuf.RpcCallback<"
				+ context.getNameResolver().getImmutableClassName(method.getOutputType()) + "> done)");
	}

	private void generateCallMethod(PrintWriter printer)
	{
		printer.println("    public final void callMethod(");
		printer.println("        com.google.protobuf.Descriptors.MethodDescriptor method,");
		printer.println("        com.google.protobuf.RpcController controller,");
		printer.println("        com.google.protobuf.Message request,");
		printer.println("        com.google.protobuf.RpcCallback<");
		printer.println("          com.google.protobuf.Message> done) {");
		printer.println("      if (method.getService() != getDescriptor()) {");
		printer.println("        throw new java.lang.IllegalArgumentException(");
		printer.println("          \"Service.callMethod() given method descriptor for wrong \" +");
		printer.println("          \"service type.\");");
		printer.println("      }");
		printer.println("      switch(method.getIndex()) {");

		for (MethodDescriptor method : descriptor.getMethods())
		{
			printer.println("        case " + method.getIndex() + ":");
			printer.println("          this." + StringUtils.underscoresToCamelCase(method.getName(), false) + "(controller, ("
					+ context.getNameResolver().getImmutableClassName(method.getInputType()) + ")request,");
			printer.println("            com.google.protobuf.RpcUtil.<"
					+ context.getNameResolver().getImmutableClassName(method.getOutputType()) + ">specializeCallback(");
			printer.println("              done));");
			printer.println("          return;");
		}

		printer.println("        default:");
		printer.println("          throw new java.lang.AssertionError(\"Can't get here.\");");
		printer.println("      }");
		printer.println("    }");
	}

	private void generateCallBlockingMethod(PrintWriter printer, String indent)
	{
		printer.println(indent + "public final com.google.protobuf.Message callBlockingMethod(");
		printer.println(indent + "    com.google.protobuf.Descriptors.MethodDescriptor method,");
		printer.println(indent + "    com.google.protobuf.RpcController controller,");
		printer.println(indent + "    com.google.protobuf.Message request)");
		printer.println(indent + "    throws com.google.protobuf.ServiceException {");
		printer.println(indent + "  if (method.getService() != getDescriptor()) {");
		printer.println(indent + "    throw new java.lang.IllegalArgumentException(");
		printer.println(indent + "      \"Service.callBlockingMethod() given method descriptor for \" +");
		printer.println(indent + "      \"wrong service type.\");");
		printer.println(indent + "  }");
		printer.println(indent + "  switch(method.getIndex()) {");

		for (MethodDescriptor method : descriptor.getMethods())
		{
			printer.println(indent + "    case " + method.getIndex() + ":");
			printer.println(indent + "      return impl." + StringUtils.underscoresToCamelCase(method.getName(), false)
					+ "(controller, (" + context.getNameResolver().getImmutableClassName(method.getInputType()) + ")request);");
		}

		printer.println(indent + "    default:");
		printer.println(indent + "      throw new java.lang.AssertionError(\"Can't get here.\");");
		printer.println(indent + "  }");
		printer.println(indent + "}");
	}

	private void generateCallBlockingMethod(PrintWriter printer) {
		generateCallBlockingMethod(printer, "    ");
	}

	private void generateGetPrototype(boolean isRequest, PrintWriter printer, String indent)
	{
		String typeName = isRequest ? "Request" : "Response";
		printer.println(indent + "public final com.google.protobuf.Message");
		printer.println(indent + "    get" + typeName + "Prototype(");
		printer.println(indent + "    com.google.protobuf.Descriptors.MethodDescriptor method) {");
		printer.println(indent + "  if (method.getService() != getDescriptor()) {");
		printer.println(indent + "    throw new java.lang.IllegalArgumentException(");
		printer.println(indent + "      \"Service.get" + typeName + "Prototype() given method \" +");
		printer.println(indent + "      \"descriptor for wrong service type.\");");
		printer.println(indent + "  }");
		printer.println(indent + "  switch(method.getIndex()) {");

		for (MethodDescriptor method : descriptor.getMethods())
		{
			String type = isRequest
					? context.getNameResolver().getImmutableClassName(method.getInputType())
					: context.getNameResolver().getImmutableClassName(method.getOutputType());
			printer.println(indent + "    case " + method.getIndex() + ":");
			printer.println(indent + "      return " + type + ".getDefaultInstance();");
		}

		printer.println(indent + "    default:");
		printer.println(indent + "      throw new java.lang.AssertionError(\"Can't get here.\");");
		printer.println(indent + "  }");
		printer.println(indent + "}");
	}

	private void generateGetPrototype(boolean isRequest, PrintWriter printer) {
		generateGetPrototype(isRequest, printer, "    ");
	}

	private void generateStub(PrintWriter printer)
	{
		printer.println("    public static Stub newStub(");
		printer.println("        com.google.protobuf.RpcChannel channel) {");
		printer.println("      return new Stub(channel);");
		printer.println("    }");
		printer.println();

		printer.println("    public static final class Stub extends " + context.getNameResolver().getImmutableClassName(descriptor.getFile()) + "." + descriptor.getName() + " implements Interface {");
		printer.println("      private Stub(com.google.protobuf.RpcChannel channel) {");
		printer.println("        this.channel = channel;");
		printer.println("      }");
		printer.println();
		printer.println("      private final com.google.protobuf.RpcChannel channel;");
		printer.println();
		printer.println("      public com.google.protobuf.RpcChannel getChannel() {");
		printer.println("        return channel;");
		printer.println("      }");

		for (MethodDescriptor method : descriptor.getMethods())
		{
			printer.println();
			generateMethodSignature(printer, method, false, "      ");
			printer.println(" {");
			String outputType = context.getNameResolver().getImmutableClassName(method.getOutputType());
			printer.println("        channel.callMethod(");
			printer.println("          getDescriptor().getMethod(" + method.getIndex() + "),");
			printer.println("          controller,");
			printer.println("          request,");
			printer.println("          " + outputType + ".getDefaultInstance(),");
			printer.println("          com.google.protobuf.RpcUtil.generalizeCallback(");
			printer.println("            done,");
			printer.println("            " + outputType + ".class,");
			printer.println("            " + outputType + ".getDefaultInstance()));");
			printer.println("      }");
		}
		printer.println("    }");
	}

	private void generateBlockingStub(PrintWriter printer)
	{
		printer.println("    public static BlockingInterface newBlockingStub(");
		printer.println("        com.google.protobuf.BlockingRpcChannel channel) {");
		printer.println("      return new BlockingStub(channel);");
		printer.println("    }");
		printer.println();

		printer.println("    public interface BlockingInterface {");
		boolean first = true;
		for (MethodDescriptor method : descriptor.getMethods())
		{
			if (!first)
			{
				printer.println();
			}
			first = false;
			generateBlockingMethodSignature(printer, method, "      ");
			printer.println(";");
		}
		printer.println("    }");
		printer.println();

		printer.println("    private static final class BlockingStub implements BlockingInterface {");
		printer.println("      private BlockingStub(com.google.protobuf.BlockingRpcChannel channel) {");
		printer.println("        this.channel = channel;");
		printer.println("      }");
		printer.println();
		printer.println("      private final com.google.protobuf.BlockingRpcChannel channel;");

		first = true;
		for (MethodDescriptor method : descriptor.getMethods())
		{
			printer.println();
			if (!first)
			{
				printer.println();
			}
			first = false;
			generateBlockingMethodSignature(printer, method, "      ");
			printer.println(" {");
			String outputType = context.getNameResolver().getImmutableClassName(method.getOutputType());
			printer.println("        return (" + outputType + ") channel.callBlockingMethod(");
			printer.println("          getDescriptor().getMethod(" + method.getIndex() + "),");
			printer.println("          controller,");
			printer.println("          request,");
			printer.println("          " + outputType + ".getDefaultInstance());");
			printer.println("      }");
		}
		printer.println();
		printer.println("    }");
	}

	private void generateBlockingMethodSignature(PrintWriter printer, MethodDescriptor method, String indent)
	{
		printer.println(indent + "public " + context.getNameResolver().getImmutableClassName(method.getOutputType()) + " "
				+ StringUtils.underscoresToCamelCase(method.getName(), false) + "(");
		printer.println(indent + "    com.google.protobuf.RpcController controller,");
		printer.println(indent + "    " + context.getNameResolver().getImmutableClassName(method.getInputType()) + " request)");
		printer.print(indent + "    throws com.google.protobuf.ServiceException");
	}
}
