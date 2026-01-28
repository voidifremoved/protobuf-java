package com.rubberjam.protobuf.compiler.java.proto2.lite;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.JavaContext;

import java.io.PrintWriter;

public class MessageFieldGenerator extends ImmutableFieldGenerator
{
	private final FieldDescriptor descriptor;
	private final JavaContext context;
	private final int fieldNumber;

	public MessageFieldGenerator(FieldDescriptor descriptor, JavaContext context)
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
}
