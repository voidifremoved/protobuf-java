package com.rubberjam.protobuf.another.compiler.java;

/**
 * Generator options. Equivalent to the C++ Options struct in options.h.
 */
public class Options
{

	private boolean generateImmutableCode = false;
	private boolean generateMutableCode = false;
	private boolean generateSharedCode = false;

	// When set, the protoc will generate the current files and all the
	// transitive
	// dependencies as lite runtime.
	private boolean enforceLite = false;

	// If true, we should build .meta files and emit @Generated annotations into
	// generated code.
	private boolean annotateCode = false;

	// Name of a file where we will write a list of generated .meta file names,
	// one per line.
	private String annotationListFile = "";

	// Name of a file where we will write a list of generated file names, one
	// per line.
	private String outputListFile = "";

	// If true, strip out nonfunctional codegen.
	private boolean stripNonfunctionalCodegen = false;

	// If true, generate JVM-specific DSL code. This defaults to true for
	// compatibility with the old behavior.
	private boolean jvmDsl = true;

	// If true, the generated DSL code will only utilize concrete types, never
	// referring to the OrBuilder interfaces.
	private boolean dslUseConcreteTypes = false;

	// Used by protobuf itself and not supported for direct use by users.
	private boolean bootstrap = false;

	public Options()
	{
		// Fields are initialized inline to match the C++ constructor
		// initialization list.
		//
	}

	public boolean isGenerateImmutableCode()
	{
		return generateImmutableCode;
	}

	public void setGenerateImmutableCode(boolean generateImmutableCode)
	{
		this.generateImmutableCode = generateImmutableCode;
	}

	public boolean isGenerateMutableCode()
	{
		return generateMutableCode;
	}

	public void setGenerateMutableCode(boolean generateMutableCode)
	{
		this.generateMutableCode = generateMutableCode;
	}

	public boolean isGenerateSharedCode()
	{
		return generateSharedCode;
	}

	public void setGenerateSharedCode(boolean generateSharedCode)
	{
		this.generateSharedCode = generateSharedCode;
	}

	public boolean isEnforceLite()
	{
		return enforceLite;
	}

	public void setEnforceLite(boolean enforceLite)
	{
		this.enforceLite = enforceLite;
	}

	public boolean isAnnotateCode()
	{
		return annotateCode;
	}

	public void setAnnotateCode(boolean annotateCode)
	{
		this.annotateCode = annotateCode;
	}

	public String getAnnotationListFile()
	{
		return annotationListFile;
	}

	public void setAnnotationListFile(String annotationListFile)
	{
		this.annotationListFile = annotationListFile;
	}

	public String getOutputListFile()
	{
		return outputListFile;
	}

	public void setOutputListFile(String outputListFile)
	{
		this.outputListFile = outputListFile;
	}

	public boolean isStripNonfunctionalCodegen()
	{
		return stripNonfunctionalCodegen;
	}

	public void setStripNonfunctionalCodegen(boolean stripNonfunctionalCodegen)
	{
		this.stripNonfunctionalCodegen = stripNonfunctionalCodegen;
	}

	public boolean isJvmDsl()
	{
		return jvmDsl;
	}

	public void setJvmDsl(boolean jvmDsl)
	{
		this.jvmDsl = jvmDsl;
	}

	public boolean isDslUseConcreteTypes()
	{
		return dslUseConcreteTypes;
	}

	public void setDslUseConcreteTypes(boolean dslUseConcreteTypes)
	{
		this.dslUseConcreteTypes = dslUseConcreteTypes;
	}

	public boolean isBootstrap()
	{
		return bootstrap;
	}

	public void setBootstrap(boolean bootstrap)
	{
		this.bootstrap = bootstrap;
	}

	public boolean isOpensourceRuntime()
	{
		return true;
	}

}