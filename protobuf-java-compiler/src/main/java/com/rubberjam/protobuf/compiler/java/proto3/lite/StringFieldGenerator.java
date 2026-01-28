package com.rubberjam.protobuf.compiler.java.proto3.lite;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.JavaContext;
import com.rubberjam.protobuf.compiler.java.StringUtils;

import java.io.PrintWriter;

public class StringFieldGenerator extends ImmutableFieldGenerator
{
	private final FieldDescriptor descriptor;
	private final JavaContext context;
	private final int fieldNumber;

	public StringFieldGenerator(FieldDescriptor descriptor, JavaContext context)
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
		printer.println("  private java.lang.String " + name + "_;");
		printer.println("  public java.lang.String get" + StringUtils.capitalizedFieldName(descriptor) + "() {");
		printer.println("    return " + name + "_;");
		printer.println("  }");
		printer.println(
				"  public com.google.protobuf.ByteString get" + StringUtils.capitalizedFieldName(descriptor) + "Bytes() {");
		printer.println("    return com.google.protobuf.ByteString.copyFromUtf8(" + name + "_);");
		printer.println("  }");
	}

	@Override
	public void generateBuilderMembers(PrintWriter printer)
	{
		// Stub
	}

	@Override
	public void generateInitializationCode(PrintWriter printer)
	{
		// Stub
	}
}
