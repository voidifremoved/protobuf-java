package com.rubberjam.protobuf.compiler.java.lite;

import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.rubberjam.protobuf.compiler.java.Context;
import com.rubberjam.protobuf.compiler.java.ServiceGenerator;

import java.io.PrintWriter;

public class ImmutableServiceGenerator extends ServiceGenerator
{
	public ImmutableServiceGenerator(ServiceDescriptor descriptor, Context context)
	{
		super(descriptor);
	}

	@Override
	public void generate(PrintWriter printer)
	{
		// Lite services are typically not generated or are empty
	}
}
