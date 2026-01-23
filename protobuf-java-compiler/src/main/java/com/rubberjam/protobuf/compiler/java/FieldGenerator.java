package com.rubberjam.protobuf.compiler.java;

import java.io.PrintWriter;

public abstract class FieldGenerator implements Comparable<FieldGenerator>
{
	public abstract int getFieldNumber();
	
	public abstract void generateSerializationCode(PrintWriter printer);

	@Override
	public int compareTo(FieldGenerator o)
	{
		return Integer.compare(getFieldNumber(), o.getFieldNumber());
	}
	
	
}
