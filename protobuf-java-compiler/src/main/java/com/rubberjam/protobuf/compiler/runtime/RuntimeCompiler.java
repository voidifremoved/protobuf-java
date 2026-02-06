package com.rubberjam.protobuf.compiler.runtime;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

public final class RuntimeCompiler
{

	public static final RuntimeCompiler DEFAULT = new RuntimeCompiler(RuntimeCompiler.class.getClassLoader());

	private static final Pattern LINE_PATTERN = Pattern.compile("\n");

	private final ReentrantLock lock = new ReentrantLock();

	private final RuntimeGeneratedClassLoader classLoader;
	private final JavaCompiler compiler;
	private final DiagnosticCollector<JavaFileObject> diagnosticCollector;

	private Map<String, String> sources = new TreeMap<>();

	public RuntimeCompiler(ClassLoader loader)
	{
		compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null)
		{
			throw new IllegalStateException("Cannot find the system Java compiler.\n"
					+ "Maybe you're using the JRE without the JDK: either the classpath lacks a jar (tools.jar)"
					+ " xor the modulepath lacks a module (java.compiler).");
		}
		classLoader = new RuntimeGeneratedClassLoader(loader);
		diagnosticCollector = new DiagnosticCollector<>();
	}

	public Map<String, String> getSources()
	{
		return sources;
	}

	public <T> Class<? extends T> compile(String fullClassName, String javaSource, Class<T> superType)
	{
		lock.lock();
		try
		{
			sources.put(fullClassName, javaSource);

			RuntimeGeneratedSource fileObject = new RuntimeGeneratedSource(fullClassName, javaSource);

			JavaFileManager standardFileManager = compiler.getStandardFileManager(diagnosticCollector, null, null);
			try (RuntimeGeneratedJavaFileManager javaFileManager = new RuntimeGeneratedJavaFileManager(standardFileManager,
					classLoader))
			{
				CompilationTask task = compiler.getTask(null, javaFileManager, diagnosticCollector,
						null, null, Collections.singletonList(fileObject));
				boolean success = task.call();
				if (!success)
				{
					String compilationMessages = diagnosticCollector.getDiagnostics().stream()
							.map(d -> d.getKind() + ":[" + d.getLineNumber() + "," + d.getColumnNumber() + "] "
									+ d.getMessage(null)
									+ "\n        "
									+ (d.getLineNumber() <= 0 ? ""
											: LINE_PATTERN.splitAsStream(javaSource).skip(d.getLineNumber() - 1).findFirst()
													.orElse("")))
							.collect(Collectors.joining("\n"));
					throw new IllegalStateException("The generated class (" + fullClassName + ") failed to compile.\n"
							+ compilationMessages);
				}
			}
			catch (IOException e)
			{
				throw new IllegalStateException("The generated class (" + fullClassName + ") failed to compile because the "
						+ JavaFileManager.class.getSimpleName() + " didn't close.", e);
			}
			Class<T> compiledClass;
			try
			{
				compiledClass = (Class<T>) classLoader.loadClass(fullClassName);
			}
			catch (ClassNotFoundException e)
			{
				throw new IllegalStateException("The generated class (" + fullClassName + ") compiled, but failed to load.", e);
			}
			if (superType != null && !superType.isAssignableFrom(compiledClass))
			{
				throw new ClassCastException("The generated compiledClass (" + compiledClass
						+ ") cannot be assigned to the superclass/interface (" + superType + ").");
			}
			return compiledClass;
		}
		finally
		{
			lock.unlock();
		}
	}
}