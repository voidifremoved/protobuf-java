package com.rubberjam.protobuf.compiler.java.proto3.lite;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.rubberjam.protobuf.compiler.java.JavaContext;
import com.rubberjam.protobuf.compiler.java.ExtensionGenerator;

import java.io.PrintWriter;

public class ImmutableExtensionGenerator extends ExtensionGenerator
{
	public ImmutableExtensionGenerator(FieldDescriptor descriptor, JavaContext context)
	{
	}

	@Override
	public void generate(PrintWriter printer)
	{
		// Stub
	}

	@Override
	public int generateNonNestedInitializationCode(PrintWriter printer)
	{
		return 0;
	}

	@Override
	public int generateRegistrationCode(PrintWriter printer)
	{
		return 0;
	}
}
