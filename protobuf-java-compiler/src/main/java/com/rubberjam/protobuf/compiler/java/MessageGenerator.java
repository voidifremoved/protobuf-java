package com.rubberjam.protobuf.compiler.java;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.OneofDescriptor;
import java.util.Map;
import java.util.TreeMap;

public abstract class MessageGenerator
{
	protected final Descriptor descriptor;
	protected final Map<Integer, OneofDescriptor> oneofs;

	public MessageGenerator(Descriptor descriptor)
	{
		this.descriptor = descriptor;
		this.oneofs = new TreeMap<>();
		// Initialize oneofs logic if needed
	}

	public abstract void generateStaticVariables(java.io.PrintWriter printer, int[] bytecodeEstimate);

	public abstract int generateStaticVariableInitializers(java.io.PrintWriter printer);

	public abstract void generate(java.io.PrintWriter printer);

	public abstract void generateInterface(java.io.PrintWriter printer);

	public abstract void generateExtensionRegistrationCode(java.io.PrintWriter printer);
}
