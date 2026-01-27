package com.rubberjam.protobuf.compiler.java.lite;

import com.google.protobuf.Descriptors.EnumDescriptor;
import com.rubberjam.protobuf.compiler.java.JavaContext;
import com.rubberjam.protobuf.compiler.java.EnumGenerator;

import java.io.PrintWriter;

public class ImmutableEnumGenerator extends EnumGenerator
{
	private final EnumDescriptor descriptor;
	private final JavaContext context;

	public ImmutableEnumGenerator(EnumDescriptor descriptor, JavaContext context)
	{
		this.descriptor = descriptor;
		this.context = context;
	}

	@Override
	public void generate(PrintWriter printer)
	{
		printer.println("// Lite Enum Generation pending");
		printer.println("public enum " + descriptor.getName() + " implements com.google.protobuf.Internal.EnumLite {");
		// Basic implementation to compile
		printer.println("  UNRECOGNIZED(-1);");
		printer.println("  private final int value;");
		printer.println("  private " + descriptor.getName() + "(int value) { this.value = value; }");
		printer.println("  @java.lang.Override public final int getNumber() { return value; }");
		printer.println("  public static " + descriptor.getName() + " forNumber(int value) { return null; }"); // Stub
		printer.println("  public static com.google.protobuf.Internal.EnumLiteMap<" + descriptor.getName()
				+ "> internalGetValueMap() { return null; }");
		printer.println("}");
	}
}
