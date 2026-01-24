package com.rubberjam.protobuf.compiler.java.full;

import java.io.PrintWriter;

import com.rubberjam.protobuf.compiler.java.FieldGenerator;

public abstract class ImmutableFieldGenerator extends FieldGenerator
{
	public abstract com.google.protobuf.Descriptors.FieldDescriptor getDescriptor();

	public abstract int getMessageBitIndex();

	public abstract int getBuilderBitIndex();

	public abstract int getNumBitsForMessage();

	public abstract int getNumBitsForBuilder();

	public abstract void generateInterfaceMembers(PrintWriter printer);

	public abstract void generateMembers(PrintWriter printer);

	public abstract void generateBuilderMembers(PrintWriter printer);

	public abstract void generateInitializationCode(PrintWriter printer);

	public abstract void generateBuilderClearCode(PrintWriter printer);

	public abstract void generateMergingCode(PrintWriter printer);

	public abstract void generateBuildingCode(PrintWriter printer);

	public abstract void generateBuilderParsingCode(PrintWriter printer);

	public abstract void generateSerializedSizeCode(PrintWriter printer);

	public abstract void generateWriteToCode(PrintWriter printer);

	public abstract void generateFieldBuilderInitializationCode(PrintWriter printer);

	public void generateBuilderParsingCodeFromPacked(PrintWriter printer)
	{
		throw new UnsupportedOperationException(
				"generateBuilderParsingCodeFromPacked() called on field generator that does not support packing.");
	}

	public abstract void generateEqualsCode(PrintWriter printer);

	public abstract void generateHashCode(PrintWriter printer);

	public void generateOneofEqualsCode(PrintWriter printer)
	{
		throw new UnsupportedOperationException("Not supported.");
	}

	public void generateOneofHashCode(PrintWriter printer)
	{
		throw new UnsupportedOperationException("Not supported.");
	}

	public abstract String getBoxedType();
}
