package com.rubberjam.protobuf.compiler.java.proto2.lite;

import com.rubberjam.protobuf.compiler.java.FieldGenerator;

public abstract class ImmutableFieldGenerator extends FieldGenerator
{
	public abstract void generateMembers(java.io.PrintWriter printer);

	public abstract void generateBuilderMembers(java.io.PrintWriter printer);

	public abstract void generateInitializationCode(java.io.PrintWriter printer);

	// Default no-ops for methods not yet needed or implemented
	public void generateBuilderClearCode(java.io.PrintWriter printer)
	{
	}

	public void generateMergingCode(java.io.PrintWriter printer)
	{
	}

	public void generateBuildingCode(java.io.PrintWriter printer)
	{
	}

	public void generateParsingCode(java.io.PrintWriter printer)
	{
	}

	public void generateParsingDoneCode(java.io.PrintWriter printer)
	{
	}

	public void generateSerializationCode(java.io.PrintWriter printer)
	{
	}

	public void generateSerializedSizeCode(java.io.PrintWriter printer)
	{
	}

	public void generateFieldAccessor(java.io.PrintWriter printer)
	{
	}

	public void generateEqualsCode(java.io.PrintWriter printer)
	{
	}

	public void generateHashCode(java.io.PrintWriter printer)
	{
	}

	public void generateInterfaceMembers(java.io.PrintWriter printer)
	{
	}
}
