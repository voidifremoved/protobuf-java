package com.rubberjam.protobuf.compiler;

import java.io.OutputStream;
import java.io.IOException;

/**
 * The abstract interface to a class which generates code implementing a
 * particular proto file in a particular language. A number of these may be
 * registered with CommandLineInterface to support various languages.
 */
public interface GeneratorContext
{
	/**
	 * Opens the given file, truncating it if it exists, and returns a
	 * OutputStream that writes to the file. The caller takes ownership of the
	 * returned object.
	 */
	OutputStream open(String filename) throws IOException;
}
