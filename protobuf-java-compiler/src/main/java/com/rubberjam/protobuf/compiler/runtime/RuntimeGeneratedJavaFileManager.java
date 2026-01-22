package com.rubberjam.protobuf.compiler.runtime;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

public final class RuntimeGeneratedJavaFileManager extends ForwardingJavaFileManager<JavaFileManager>
{

	private final RuntimeGeneratedClassLoader classLoader;

	public RuntimeGeneratedJavaFileManager(JavaFileManager fileManager, RuntimeGeneratedClassLoader classLoader)
	{
		super(fileManager);
		this.classLoader = classLoader;
	}

	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String qualifiedName, Kind kind, FileObject sibling)
	{
		if (kind != Kind.CLASS)
		{
			throw new IllegalArgumentException("Unsupported kind (" + kind + ") for class (" + qualifiedName + ").");
		}
		RuntimeGeneratedClass fileObject = new RuntimeGeneratedClass(qualifiedName);
		classLoader.addJavaFileObject(qualifiedName, fileObject);
		return fileObject;
	}

	@Override
	public ClassLoader getClassLoader(Location location)
	{
		return classLoader;
	}

}