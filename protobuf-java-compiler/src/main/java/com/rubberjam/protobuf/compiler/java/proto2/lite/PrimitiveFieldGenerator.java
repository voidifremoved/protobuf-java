package com.rubberjam.protobuf.compiler.java.proto2.lite;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.JavaContext;
import com.rubberjam.protobuf.compiler.java.StringUtils;

import java.io.PrintWriter;

public class PrimitiveFieldGenerator extends ImmutableFieldGenerator
{
	private final FieldDescriptor descriptor;
	private final JavaContext context;
	private final int fieldNumber;

	public PrimitiveFieldGenerator(FieldDescriptor descriptor, JavaContext context)
	{
		this.descriptor = descriptor;
		this.context = context;
		this.fieldNumber = descriptor.getNumber();
	}
	
	@Override
	public int getFieldNumber()
	{
		return fieldNumber;
	}

	@Override
	public void generateMembers(PrintWriter printer)
	{
		String name = StringUtils.camelCaseFieldName(descriptor);
		String type = StringUtils.getJavaType(descriptor).toString().toLowerCase(); // Simplified
		// JavaType is an enum, we need the primitive type name (int, long, etc)
		// Actually StringUtils.getJavaType returns JavaType enum, we need a
		// helper to get the java type string
		// Let's assume a helper or map exists or just use a switch/case

		// Quick fix: assuming standard primitive types mapping for now or use
		// descriptor.getJavaType().name().toLowerCase() but that might not
		// match exactly (INT -> int is fine, but FLOAT -> float etc.)
		// Better: use descriptor.getJavaType() which returns
		// Descriptors.FieldDescriptor.JavaType
		// and map it.

		// Actually, let's look at `com.google.protobuf.compiler.java.JavaType`.
		// It has a method? No.

		// Let's implement a quick local mapping or use what we have.
		type = getPrimitiveTypeName(descriptor);

		printer.println("  private " + type + " " + name + "_;");
		printer.println("  public " + type + " get" + StringUtils.capitalizedFieldName(descriptor) + "() {");
		printer.println("    return " + name + "_;");
		printer.println("  }");
	}

	private String getPrimitiveTypeName(FieldDescriptor descriptor)
	{
		switch (descriptor.getJavaType())
		{
		case INT:
			return "int";
		case LONG:
			return "long";
		case FLOAT:
			return "float";
		case DOUBLE:
			return "double";
		case BOOLEAN:
			return "boolean";
		case STRING:
			return "java.lang.String";
		case BYTE_STRING:
			return "com.google.protobuf.ByteString";
		case ENUM:
			return "int"; // Lite enums are often just ints or need special
							// handling
		case MESSAGE:
			return ""; // Should not be here
		default:
			return "java.lang.Object";
		}
	}

	@Override
	public void generateBuilderMembers(PrintWriter printer)
	{
		String name = StringUtils.camelCaseFieldName(descriptor);
		String capitalizedName = StringUtils.capitalizedFieldName(descriptor);
		String type = getPrimitiveTypeName(descriptor);

		printer.println("    public " + type + " get" + capitalizedName + "() {");
		printer.println("      return instance.get" + capitalizedName + "();");
		printer.println("    }");

		printer.println("    public Builder set" + capitalizedName + "(" + type + " value) {");
		printer.println("      copyOnWrite();");
		printer.println("      instance.set" + capitalizedName + "(value);");
		printer.println("      return this;");
		printer.println("    }");
	}

	@Override
	public void generateInitializationCode(PrintWriter printer)
	{
		// Stub
	}
}
