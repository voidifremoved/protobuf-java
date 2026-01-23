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
		String identifier = getUniqueFileScopeIdentifier(descriptor);
		printer.println("  private static final com.google.protobuf.Descriptors.Descriptor");
		printer.println("      internal_" + identifier + "_descriptor;");
		printer.println("  private static final com.google.protobuf.GeneratedMessage.FieldAccessorTable");
		printer.println("      internal_" + identifier + "_fieldAccessorTable;");

		for (Descriptor nestedType : descriptor.getNestedTypes())
		{
			new ImmutableMessageGenerator(nestedType, context).generateStaticVariables(printer, bytecodeEstimate);
		}
	}

	@Override
	public int generateStaticVariableInitializers(PrintWriter printer)
	{
		String identifier = getUniqueFileScopeIdentifier(descriptor);
		if (descriptor.getContainingType() == null)
		{
			printer.println("    internal_" + identifier + "_descriptor =");
			printer.println("        getDescriptor().getMessageTypes().get(" + descriptor.getIndex() + ");");
		}
		else
		{
			String parentIdentifier = getUniqueFileScopeIdentifier(descriptor.getContainingType());
			printer.println("    internal_" + identifier + "_descriptor =");
			printer.println("        internal_" + parentIdentifier + "_descriptor.getNestedTypes().get(" + descriptor.getIndex() + ");");
		}
		printer.println("    internal_" + identifier + "_fieldAccessorTable =");
		printer.println("        new com.google.protobuf.GeneratedMessage.FieldAccessorTable(");
		printer.print("            internal_" + identifier + "_descriptor,");
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
		
		// Match C++ WriteMessageDocComment behavior - use DocComment utility
		com.rubberjam.protobuf.compiler.java.DocComment.writeMessageDocComment(printer, descriptor, context.getOptions(), false);
		
		printer.println("  public static final class " + className + " extends");
		printer.println("      com.google.protobuf.GeneratedMessage implements");
		printer.println("      // @@protoc_insertion_point(message_implements:" + descriptor.getFullName() + ")");
		printer.println("      " + className + "OrBuilder {");
		printer.println("  private static final long serialVersionUID = 0L;");
		printer.println("    static {");
		printer.println("      com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(");
		printer.println("        com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,");
		printer.println("        /* major= */ 4,");
		printer.println("        /* minor= */ 33,");
		printer.println("        /* patch= */ 4,");
		printer.println("        /* suffix= */ \"\",");
		printer.println("        \"" + className + "\");");
		printer.println("    }");
		printer.println("    // Use " + className + ".newBuilder() to construct.");
		printer.println("    private " + className + "(com.google.protobuf.GeneratedMessage.Builder<?> builder) {");
		printer.println("      super(builder);");
		printer.println("    }");
		printer.println("    private " + className + "() {");
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateInitializationCode(printer);
		}
		printer.println("    }");

		printer.println("    public static final com.google.protobuf.Descriptors.Descriptor");
		printer.println("        getDescriptor() {");
		String identifier = getUniqueFileScopeIdentifier(descriptor);
		printer.println("      return " + outerClassName + ".internal_" + identifier + "_descriptor;");
		printer.println("    }");
		printer.println();

		printer.println("    @java.lang.Override");
		printer.println("    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable");
		printer.println("        internalGetFieldAccessorTable() {");
		printer.println("      return " + outerClassName + ".internal_" + identifier + "_fieldAccessorTable");
		printer.println("          .ensureFieldAccessorsInitialized(");
		printer.println("              " + outerClassName + "." + className + ".class, " + outerClassName + "." + className + ".Builder.class);");
		printer.println("    }");
		printer.println();

		// Fields
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateMembers(printer);
		}

		// Builder methods
		printer.println("    public static Builder newBuilder() {");
		printer.println("      return DEFAULT_INSTANCE.toBuilder();");
		printer.println("    }");
		printer.println();

		printer.println("    public static Builder newBuilder(" + className + " prototype) {");
		printer.println("      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);");
		printer.println("    }");
		printer.println();

		printer.println("    @java.lang.Override");
		printer.println("    public Builder toBuilder() {");
		printer.println("      return this == DEFAULT_INSTANCE");
		printer.println("          ? new Builder() : new Builder().mergeFrom(this);");
		printer.println("    }");
		printer.println();

		printer.println("    @java.lang.Override");
		printer.println("    public Builder newBuilderForType() {");
		printer.println("      return newBuilder();");
		printer.println("    }");
		printer.println();

		printer.println("    @java.lang.Override");
		printer.println("    protected Builder newBuilderForType(com.google.protobuf.GeneratedMessage.BuilderParent parent) {");
		printer.println("      Builder builder = new Builder(parent);");
		printer.println("      return builder;");
		printer.println("    }");
		printer.println();

		messageBuilderGenerator.generate(printer);

		// Default instance
		printer.println("    // @@protoc_insertion_point(class_scope:" + descriptor.getFullName() + ")");
		printer.println("    private static final " + className + " DEFAULT_INSTANCE;");
		printer.println("    static {");
		printer.println("      DEFAULT_INSTANCE = new " + className + "();");
		printer.println("    }");
		printer.println();

		printer.println("    public static " + className + " getDefaultInstance() {");
		printer.println("      return DEFAULT_INSTANCE;");
		printer.println("    }");
		printer.println();

		printer.println("    @java.lang.Override");
		printer.println("    public " + className + " getDefaultInstanceForType() {");
		printer.println("      return DEFAULT_INSTANCE;");
		printer.println("    }");

		printer.println("  }");
	}

	@Override
	public void generateInterface(PrintWriter printer)
	{
		String className = descriptor.getName();
		printer.println("  public interface " + className + "OrBuilder extends");
		printer.println("      // @@protoc_insertion_point(interface_extends:" + descriptor.getFullName() + ")");
		printer.println("      com.google.protobuf.MessageOrBuilder {");
		printer.println();
		for (ImmutableFieldGenerator fieldGenerator : fieldGenerators.getFieldGenerators())
		{
			fieldGenerator.generateInterfaceMembers(printer);
		}
		printer.println("  }");
	}

	@Override
	public void generateExtensionRegistrationCode(PrintWriter printer)
	{
	}

	private String getUniqueFileScopeIdentifier(Descriptor descriptor)
	{
		return "static_" + descriptor.getFullName().replace('.', '_');
	}
}
