package com.rubberjam.protobuf.compiler.java.lite;

import java.io.PrintWriter;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;

public class EnumFieldGenerator extends ImmutableFieldGenerator
{
	private final FieldDescriptor descriptor;
	private final Context context;
	private final int fieldNumber;

	public EnumFieldGenerator(FieldDescriptor descriptor, Context context)
	{
		this.descriptor = descriptor;
		this.context = context;
		this.fieldNumber = descriptor.getNumber();
	}

	@Override
	public void generateMembers(PrintWriter printer)
	{
		// Stub
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

	@Override
	public int getFieldNumber()
	{
		return fieldNumber;
	}
}
