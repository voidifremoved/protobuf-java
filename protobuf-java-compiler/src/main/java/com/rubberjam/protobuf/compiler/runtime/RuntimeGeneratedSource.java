package com.rubberjam.protobuf.compiler.runtime;

import java.net.URI;

import javax.tools.SimpleJavaFileObject;

public final class RuntimeGeneratedSource extends SimpleJavaFileObject
{

	private final String javaSource;

	public RuntimeGeneratedSource(String fullClassName, String javaSource)
	{
		super(URI.create("string:///" + fullClassName.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
		this.javaSource = javaSource;
	}

	@Override
	public String getCharContent(boolean ignoreEncodingErrors)
	{
		return javaSource;
	}

}