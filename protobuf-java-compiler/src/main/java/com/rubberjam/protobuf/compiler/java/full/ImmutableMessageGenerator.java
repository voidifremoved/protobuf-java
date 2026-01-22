package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.Descriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.FieldGeneratorMap;
import com.rubberjam.protobuf.compiler.java.MessageGenerator;
import com.rubberjam.protobuf.compiler.java.StringUtils;

import java.io.PrintWriter;

public class ImmutableMessageGenerator extends MessageGenerator
{
	private final Context context;
	private final FieldGeneratorMap<ImmutableFieldGenerator> fieldGenerators;
	private final MessageBuilderGenerator messageBuilderGenerator;

	public ImmutableMessageGenerator(Descriptor descriptor, Context context)
	{
		super(descriptor);
		this.context = context;
		this.fieldGenerators = MakeFieldGens.makeImmutableFieldGenerators(descriptor, context);
		this.messageBuilderGenerator = new MessageBuilderGenerator(descriptor, context, fieldGenerators);
	}

	@Override
	public void generateStaticVariables(PrintWriter printer, int[] bytecodeEstimate)
	{
		String uniqueName = getUniqueClassName(descriptor);
		printer.println("  private static final com.google.protobuf.Descriptors.Descriptor");
		printer.println("      " + uniqueName + "_descriptor;");
		printer.println("  private static final com.google.protobuf.GeneratedMessage.FieldAccessorTable");
		printer.println("      " + uniqueName + "_fieldAccessorTable;");

		for (Descriptor nestedType : descriptor.getNestedTypes())
		{
			new ImmutableMessageGenerator(nestedType, context).generateStaticVariables(printer, bytecodeEstimate);
		}
	}

	@Override
	public int generateStaticVariableInitializers(PrintWriter printer)
	{
		String uniqueName = getUniqueClassName(descriptor);
		if (descriptor.getContainingType() == null)
		{
			printer.println("    " + uniqueName + "_descriptor =");
			printer.println("        getDescriptor().getMessageTypes().get(" + descriptor.getIndex() + ");");
		}
		else
		{
			String parentUniqueName = getUniqueClassName(descriptor.getContainingType());
			printer.println("    " + uniqueName + "_descriptor =");
			printer.println("        " + parentUniqueName + "_descriptor.getNestedTypes().get(" + descriptor.getIndex() + ");");
		}
		printer.println("    " + uniqueName + "_fieldAccessorTable =");
		printer.println("        new com.google.protobuf.GeneratedMessage.FieldAccessorTable(");
		printer.print("            " + uniqueName + "_descriptor,");
		printer.print("            new java.lang.String[] {");
		for (int i = 0; i < descriptor.getFields().size(); i++)
		{
			if (i > 0)
			{
				printer.print(", ");
			}
			printer.print("\"" + StringUtils.capitalizedFieldName(descriptor.getFields().get(i)) + "\"");
		}
		printer.println("});");

		for (Descriptor nestedType : descriptor.getNestedTypes())
		{
			new ImmutableMessageGenerator(nestedType, context).generateStaticVariableInitializers(printer);
		}
		return 0;
	}

	@Override
	public void generate(PrintWriter printer)
	{
		String className = descriptor.getName();
		String outerClassName = context.getNameResolver().getFileClassName(descriptor.getFile(), true);
		printer.println("public static final class " + className + " extends");
		printer.println("    com.google.protobuf.GeneratedMessage implements");
		printer.println("    // @@protoc_insertion_point(message_implements:" + descriptor.getFullName() + ")");
		printer.println("    " + className + "OrBuilder {");
		printer.println("private static final long serialVersionUID = 0L;");
		printer.println("    static {");
		printer.println("      com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(");
		printer.println("        com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,");
		printer.println("        /* major= */ 4,");
		printer.println("        /* minor= */ 31,");
		printer.println("        /* patch= */ 1,");
		printer.println("        /* suffix= */ \"\",");
		printer.println("        " + className + ".class.getName());");
		printer.println("    }");

		// Fields
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateMembers(printer);
		}

		printer.println("  private " + className + "() {");
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateInitializationCode(printer);
		}
		printer.println("  }");

		printer.println("  @java.lang.Override");
		printer.println("  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {");
		printer.println("    return this.unknownFields;");
		printer.println("  }");

		printer.println("  public static final com.google.protobuf.Descriptors.Descriptor");
		printer.println("      getDescriptor() {");
		printer.println("    return " + outerClassName + "." + getUniqueClassName(descriptor) + "_descriptor;");
		printer.println("  }");

		printer.println("  @java.lang.Override");
		printer.println("  protected com.google.protobuf.GeneratedMessage.FieldAccessorTable");
		printer.println("      internalGetFieldAccessorTable() {");
		printer.println("    return " + outerClassName + "." + getUniqueClassName(descriptor) + "_fieldAccessorTable");
		printer.println("        .ensureFieldAccessorsInitialized(" + className + ".class, " + className + ".Builder.class);");
		printer.println("  }");

		// Builder
		printer.println("  public static Builder newBuilder() {");
		printer.println("    return DEFAULT_INSTANCE.toBuilder();");
		printer.println("  }");

		printer.println("  public static Builder newBuilder(" + className + " prototype) {");
		printer.println("    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);");
		printer.println("  }");

		printer.println("  @java.lang.Override");
		printer.println("  public Builder toBuilder() {");
		printer.println("    return this == DEFAULT_INSTANCE");
		printer.println("        ? new Builder() : new Builder().mergeFrom(this);");
		printer.println("  }");

		printer.println("  @java.lang.Override");
		printer.println("  protected Builder newBuilderForType(com.google.protobuf.GeneratedMessage.BuilderParent parent) {");
		printer.println("    Builder builder = new Builder(parent);");
		printer.println("    return builder;");
		printer.println("  }");

		messageBuilderGenerator.generate(printer);

		// Constructor taking builder
		printer.println("  private " + className + "(Builder builder) {");
		printer.println("    super(builder);");
		printer.println("  }");

		// Static block
		printer.println("  static {");
		printer.println("  }");

		// Default instance
		printer.println("    // @@protoc_insertion_point(class_scope:" + descriptor.getFullName() + ")");
		printer.println("  private static final " + className + " DEFAULT_INSTANCE;");
		printer.println("  static {");
		printer.println("    DEFAULT_INSTANCE = new " + className + "();");
		printer.println("  }");

		printer.println("  public static " + className + " getDefaultInstance() {");
		printer.println("    return DEFAULT_INSTANCE;");
		printer.println("  }");

		printer.println("  @java.lang.Override");
		printer.println("  public " + className + " getDefaultInstanceForType() {");
		printer.println("    return DEFAULT_INSTANCE;");
		printer.println("  }");

		printer.println("}");
	}

	@Override
	public void generateInterface(PrintWriter printer)
	{
		String className = descriptor.getName();
		printer.println("public interface " + className + "OrBuilder extends");
		printer.println("    // @@protoc_insertion_point(interface_extends:" + descriptor.getFullName() + ")");
		printer.println("    com.google.protobuf.MessageOrBuilder {");
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateInterfaceMembers(printer);
		}
		printer.println("}");
	}

	@Override
	public void generateExtensionRegistrationCode(PrintWriter printer)
	{
	}

	private String getUniqueClassName(Descriptor descriptor)
	{
		return "internal_static_" + descriptor.getFullName().replace('.', '_');
	}
}
