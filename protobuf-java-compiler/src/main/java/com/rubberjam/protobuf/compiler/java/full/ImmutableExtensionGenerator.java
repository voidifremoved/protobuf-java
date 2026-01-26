package com.rubberjam.protobuf.compiler.java.full;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.ExtensionGenerator;
import com.rubberjam.protobuf.compiler.java.JavaType;
import com.rubberjam.protobuf.compiler.java.StringUtils;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class ImmutableExtensionGenerator extends ExtensionGenerator
{
	private final FieldDescriptor descriptor;
	private final Context context;
	private final String scope;

	public ImmutableExtensionGenerator(FieldDescriptor descriptor, Context context)
	{
		this.descriptor = descriptor;
		this.context = context;
		if (descriptor.getExtensionScope() != null)
		{
			this.scope = context.getNameResolver().getImmutableClassName(descriptor.getExtensionScope());
		}
		else
		{
			// Match C++ behavior: use GetImmutableClassName(FileDescriptor*) for file-level extensions
			this.scope = context.getNameResolver().getImmutableClassName(descriptor.getFile());
		}
	}

	@Override
	public void generate(PrintWriter printer)
	{
		Map<String, String> vars = new HashMap<>();
		initTemplateVars(descriptor, scope, true, vars, context);
		printer.println("  public static final int " + vars.get("constant_name") + " = " + vars.get("number") + ";");

		if (descriptor.getExtensionScope() == null)
		{
			// Non-nested
			printer.println("  public static final");
			printer.println("    com.google.protobuf.GeneratedMessage.GeneratedExtension<");
			printer.println("      " + vars.get("containing_type") + ",");
			printer.println("      " + vars.get("type") + "> " + vars.get("name") + " = com.google.protobuf.GeneratedMessage");
			printer.println("          .newFileScopedGeneratedExtension(");
			printer.println("        " + vars.get("singular_type") + ".class,");
			printer.println("        " + vars.get("prototype") + ");");
		}
		else
		{
			// Nested
			printer.println("  public static final");
			printer.println("    com.google.protobuf.GeneratedMessage.GeneratedExtension<");
			printer.println("      " + vars.get("containing_type") + ",");
			printer.println("      " + vars.get("type") + "> " + vars.get("name") + " = com.google.protobuf.GeneratedMessage");
			printer.println("          .newMessageScopedGeneratedExtension(");
			printer.println("        " + vars.get("scope") + ".getDefaultInstance(),");
			printer.println("        " + vars.get("index") + ",");
			printer.println("        " + vars.get("singular_type") + ".class,");
			printer.println("        " + vars.get("prototype") + ");");
		}
	}

	@Override
	public int generateNonNestedInitializationCode(PrintWriter printer)
	{
		int bytecodeEstimate = 0;
		if (descriptor.getExtensionScope() == null)
		{
			printer.println("    " + StringUtils.underscoresToCamelCase(descriptor.getName(), false)
					+ ".internalInit(descriptor.getExtension(" + descriptor.getIndex() + "));");
			bytecodeEstimate += 21;
		}
		return bytecodeEstimate;
	}

	@Override
	public int generateRegistrationCode(PrintWriter printer)
	{
		printer.println(
				"    registry.add(" + scope + "." + StringUtils.underscoresToCamelCase(descriptor.getName(), false) + ");");
		return 7;
	}

	private void initTemplateVars(FieldDescriptor descriptor, String scope, boolean immutable, Map<String, String> vars,
			Context context)
	{
		vars.put("scope", scope);
		vars.put("name", StringUtils.underscoresToCamelCase(descriptor.getName(), false));
		vars.put("containing_type", context.getNameResolver().getClassName(descriptor.getContainingType(), immutable));
		vars.put("number", String.valueOf(descriptor.getNumber()));
		vars.put("constant_name", StringUtils.fieldConstantName(descriptor));
		vars.put("index", String.valueOf(descriptor.getIndex()));
		vars.put("prototype", "null");

		JavaType javaType = StringUtils.getJavaType(descriptor);
		String singularType;
		switch (javaType)
		{
		case MESSAGE:
			singularType = context.getNameResolver().getClassName(descriptor.getMessageType(), immutable);
			vars.put("prototype", singularType + ".getDefaultInstance()");
			break;
		case ENUM:
			singularType = context.getNameResolver().getImmutableClassName(descriptor.getEnumType());
			break;
		case STRING:
			singularType = "java.lang.String";
			break;
		case BYTES:
			singularType = "com.google.protobuf.ByteString";
			break;
		default:
			singularType = StringUtils.boxedPrimitiveTypeName(javaType);
			break;
		}

		vars.put("type", descriptor.isRepeated() ? "java.util.List<" + singularType + ">" : singularType);
		vars.put("singular_type", singularType);
	}
}
