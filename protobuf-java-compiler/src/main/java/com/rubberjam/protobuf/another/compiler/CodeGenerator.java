package com.rubberjam.protobuf.another.compiler;

import com.google.protobuf.Descriptors.FileDescriptor;
import java.util.List;

/**
 * The abstract interface to a class which generates code implementing a
 * particular proto file in a particular language. A number of these may be
 * registered with CommandLineInterface to support various languages.
 */
public abstract class CodeGenerator
{
	/**
	 * Generates code for the given proto file, generating one or more files in
	 * the given output directory.
	 *
	 * @param file
	 *            The file to generate code for.
	 * @param parameter
	 *            A parameter to be passed to the generator.
	 * @param generatorContext
	 *            The context in which to generate the code.
	 * @throws GenerationException
	 *             if an error occurred during generation.
	 */
	public abstract void generate(
			FileDescriptor file, String parameter, GeneratorContext generatorContext)
			throws GenerationException;

	/**
	 * Generates code for all given proto files.
	 *
	 * @param files
	 *            The files to generate code for.
	 * @param parameter
	 *            A parameter to be passed to the generator.
	 * @param generatorContext
	 *            The context in which to generate the code.
	 * @throws GenerationException
	 *             if an error occurred during generation.
	 */
	public void generateAll(
			List<FileDescriptor> files, String parameter, GeneratorContext generatorContext)
			throws GenerationException
	{
		for (FileDescriptor file : files)
		{
			generate(file, parameter, generatorContext);
		}
	}

	/**
	 * An exception that occurred during code generation.
	 */
	public static class GenerationException extends Exception
	{
		public GenerationException(String message)
		{
			super(message);
		}

		public GenerationException(Throwable cause)
		{
			super(cause);
		}
	}

	/**
	 * This must be kept in sync with plugin.proto. See that file for
	 * documentation on each value.
	 */
	public enum Feature
	{
		FEATURE_PROTO3_OPTIONAL(1),
		FEATURE_SUPPORTS_EDITIONS(2);

		private final int value;

		Feature(int value)
		{
			this.value = value;
		}

		public int getValue()
		{
			return value;
		}
	}

	/**
	 * Implement this to indicate what features this code generator supports.
	 *
	 * @return A bitwise OR of values from the Feature enum above (or zero).
	 */
	public long getSupportedFeatures()
	{
		return 0;
	}

	/**
	 * Returns the minimum edition (inclusive) supported by this generator. Any
	 * proto files with an edition before this will result in an error.
	 */
	public Edition getMinimumEdition()
	{
		return Edition.EDITION_UNKNOWN;
	}

	/**
	 * Returns the maximum edition (inclusive) supported by this generator. Any
	 * proto files with an edition after this will result in an error.
	 */
	public Edition getMaximumEdition()
	{
		return Edition.EDITION_UNKNOWN;
	}

	/**
	 * Builds a default feature set mapping for this generator.
	 */
	public FeatureSetDefaults buildFeatureSetDefaults()
	{
		return null;
	}
}
