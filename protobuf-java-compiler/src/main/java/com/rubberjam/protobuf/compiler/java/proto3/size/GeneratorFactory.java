package com.rubberjam.protobuf.compiler.java.proto3.size;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.rubberjam.protobuf.compiler.java.JavaContext;
import com.rubberjam.protobuf.compiler.java.EnumGenerator;
import com.rubberjam.protobuf.compiler.java.ExtensionGenerator;
import com.rubberjam.protobuf.compiler.java.MessageGenerator;
import com.rubberjam.protobuf.compiler.java.ServiceGenerator;

import java.io.PrintWriter;

public class GeneratorFactory implements com.rubberjam.protobuf.compiler.java.GeneratorFactory
{
	private final JavaContext context;

	public GeneratorFactory(JavaContext context)
	{
		this.context = context;
	}

	@Override
	public MessageGenerator newMessageGenerator(Descriptor descriptor)
	{
		return new ImmutableMessageGenerator(descriptor, context);
	}

	@Override
	public EnumGenerator newEnumGenerator(EnumDescriptor descriptor)
	{
		return new ImmutableEnumGenerator(descriptor, context);
	}

	@Override
	public ExtensionGenerator newExtensionGenerator(FieldDescriptor descriptor)
	{
		return new ImmutableExtensionGenerator(descriptor, context);
	}

	@Override
	public ServiceGenerator newServiceGenerator(ServiceDescriptor descriptor)
	{
		return new ImmutableServiceGenerator(descriptor, context);
	}
}
