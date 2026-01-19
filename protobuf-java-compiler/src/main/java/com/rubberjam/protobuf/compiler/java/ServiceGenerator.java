package com.rubberjam.protobuf.compiler.java;

import com.google.protobuf.Descriptors.ServiceDescriptor;

public abstract class ServiceGenerator
{
	protected final ServiceDescriptor descriptor;

	public ServiceGenerator(ServiceDescriptor descriptor)
	{
		this.descriptor = descriptor;
	}

	public abstract void generate(java.io.PrintWriter printer);
}
