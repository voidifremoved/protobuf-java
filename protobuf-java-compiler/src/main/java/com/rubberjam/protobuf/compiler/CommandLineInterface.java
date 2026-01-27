// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.rubberjam.protobuf.compiler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FileDescriptor;

/** A command-line interface for the protocol buffer compiler. */
public final class CommandLineInterface
{
	public static void main(String[] args)
	{
		System.exit(run(args));
	}

	/**
	 * Runs the command-line interface.
	 *
	 * @param args
	 *            The command-line arguments.
	 * @return The exit code.
	 */
	public static int run(String[] args)
	{
		if (args.length == 0)
		{
			printHelp();
			return 0;
		}

		String javaOut = null;
		String cppOut = null;
		String csharpOut = null;
		List<String> protoFiles = new ArrayList<>();

		for (String arg : args)
		{
			if (arg.startsWith("--java_out="))
			{
				javaOut = arg.substring("--java_out=".length());
			}
			else if (arg.startsWith("--cpp_out="))
			{
				cppOut = arg.substring("--cpp_out=".length());
			}
			else if (arg.startsWith("--csharp_out="))
			{
				csharpOut = arg.substring("--csharp_out=".length());
			}
			else
			{
				protoFiles.add(arg);
			}
		}

		if (javaOut == null && cppOut == null && csharpOut == null)
		{
			System.err.println("Missing output directive.");
			return 1;
		}

		if (protoFiles.isEmpty())
		{
			System.err.println("Missing input file.");
			return 1;
		}

		for (String protoFile : protoFiles)
		{
			try
			{
				if (!processProtoFile(protoFile, javaOut, cppOut, csharpOut))
				{
					return 1;
				}
			}
			catch (IOException e)
			{
				System.err.println("Error processing file " + protoFile + ": " + e.getMessage());
				return 1;
			}
		}

		return 0;
	}

	private static boolean processProtoFile(String protoFile, String javaOut, String cppOut, String csharpOut)
			throws IOException
	{
		File file = new File(protoFile);
		if (!file.exists())
		{
			System.err.println(protoFile + ": No such file or directory.");
			return false;
		}

		FileDescriptorProto.Builder fileBuilder = FileDescriptorProto.newBuilder();
		fileBuilder.setName(protoFile);

		ErrorCollector errorCollector = (line, column, message) -> System.err
				.println(protoFile + ":" + line + ":" + column + ": " + message);

		try (Reader reader = new FileReader(file))
		{
			Tokenizer tokenizer = new Tokenizer(reader, errorCollector);
			Parser parser = new Parser(errorCollector, new SourceLocationTable());
			if (!parser.parse(tokenizer, fileBuilder))
			{
				System.err.println("Error parsing " + protoFile);
				return false;
			}
		}

		try
		{
			FileDescriptor fileDescriptor = FileDescriptor.buildFrom(fileBuilder.build(), new FileDescriptor[0]);

			if (javaOut != null)
			{
				JavaCodeGenerator codeGenerator = new JavaCodeGenerator();
				GeneratorContextImpl generatorContext = new GeneratorContextImpl(javaOut);
				codeGenerator.generate(fileDescriptor, "", generatorContext);
			}

			if (cppOut != null)
			{
				com.rubberjam.protobuf.compiler.cpp.CppCodeGenerator codeGenerator = new com.rubberjam.protobuf.compiler.cpp.CppCodeGenerator();
				GeneratorContextImpl generatorContext = new GeneratorContextImpl(cppOut);
				codeGenerator.generate(fileDescriptor, "", generatorContext);
			}

			if (csharpOut != null)
			{
				com.rubberjam.protobuf.compiler.csharp.CSharpCodeGenerator codeGenerator = new com.rubberjam.protobuf.compiler.csharp.CSharpCodeGenerator();
				GeneratorContextImpl generatorContext = new GeneratorContextImpl(csharpOut);
				codeGenerator.generate(fileDescriptor, "", generatorContext);
			}
		}
		catch (Descriptors.DescriptorValidationException | CodeGenerator.GenerationException e)
		{
			System.err.println("Error processing " + protoFile + ": " + e.getMessage());
			return false;
		}

		return true;
	}

	private static void printHelp()
	{
		System.out.println("Usage: protoc [OPTION] PROTO_FILES");
		System.out.println("Parse PROTO_FILES and generate output based on the options given:");
		System.out.println("  --java_out=OUT_DIR      Generate Java source files.");
		System.out.println("  --cpp_out=OUT_DIR       Generate C++ source files.");
		System.out.println("  --csharp_out=OUT_DIR    Generate C# source files.");
	}
}
