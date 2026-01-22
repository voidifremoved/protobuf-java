package com.rubberjam.protobuf.compiler.runtime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public final class RuntimeGeneratedClass extends SimpleJavaFileObject
{

	private ByteArrayOutputStream classOutputStream;

	public RuntimeGeneratedClass(String fullClassName)
	{
		super(URI.create("bytes:///" + fullClassName), Kind.CLASS);
	}

	@Override
	public InputStream openInputStream()
	{
		return new ByteArrayInputStream(getClassBytes());
	}

	@Override
	public OutputStream openOutputStream()
	{
		classOutputStream = new ByteArrayOutputStream();
		return classOutputStream;
	}

	public byte[] getClassBytes()
	{
		return classOutputStream.toByteArray();
	}
}