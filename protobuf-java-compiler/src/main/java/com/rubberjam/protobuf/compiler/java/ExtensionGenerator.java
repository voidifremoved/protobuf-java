package com.rubberjam.protobuf.compiler.java;

import com.google.protobuf.Descriptors.FieldDescriptor;
import java.util.Map;

public abstract class ExtensionGenerator
{
	public abstract void generate(java.io.PrintWriter printer);

	public abstract int generateNonNestedInitializationCode(java.io.PrintWriter printer);

	public abstract int generateRegistrationCode(java.io.PrintWriter printer);
}
