package com.rubberjam.protobuf.compiler.runtime;

import java.util.HashMap;
import java.util.Map;

public final class RuntimeGeneratedClassLoader extends ClassLoader
{

	private final Map<String, RuntimeGeneratedClass> fileObjectMap = new HashMap<>();

	public RuntimeGeneratedClassLoader(ClassLoader parent)
	{
		super(parent);
	}

	@Override
	protected Class<?> findClass(String fullClassName) throws ClassNotFoundException
	{
		RuntimeGeneratedClass fileObject = fileObjectMap.get(fullClassName);
		if (fileObject != null)
		{
			byte[] classBytes = fileObject.getClassBytes();
			return defineClass(fullClassName, classBytes, 0, classBytes.length);
		}
		return super.findClass(fullClassName);
	}

	public void addJavaFileObject(String qualifiedName, RuntimeGeneratedClass fileObject)
	{
		fileObjectMap.put(qualifiedName, fileObject);
	}

}