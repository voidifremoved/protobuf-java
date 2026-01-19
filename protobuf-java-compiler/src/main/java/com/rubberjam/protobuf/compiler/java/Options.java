// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.rubberjam.protobuf.compiler.java;

import java.util.HashMap;
import java.util.Map;

/**
 * Generator options.
 */
public final class Options
{
	public boolean generateImmutableCode = false;
	public boolean generateMutableCode = false;
	public boolean generateSharedCode = false;
	public boolean enforceLite = false;
	public boolean annotateCode = false;
	public String annotationListFile;
	public String outputListFile;
	public boolean stripNonfunctionalCodegen = false;
	public boolean jvmDsl = true;
	public boolean dslUseConcreteTypes = false;
	public boolean bootstrap = false;

	public static Options fromParameter(String parameter)
	{
		Options options = new Options();
		Map<String, String> parsedOptions = parseGeneratorParameter(parameter);

		options.outputListFile = parsedOptions.get("output_list_file");
		options.annotationListFile = parsedOptions.get("annotation_list_file");

		options.generateImmutableCode = parsedOptions.containsKey("immutable");
		options.generateMutableCode = parsedOptions.containsKey("mutable");
		options.generateSharedCode = parsedOptions.containsKey("shared");
		options.enforceLite = parsedOptions.containsKey("lite");
		options.annotateCode = parsedOptions.containsKey("annotate_code");
		options.stripNonfunctionalCodegen = parsedOptions.containsKey("experimental_strip_nonfunctional_codegen");
		options.bootstrap = parsedOptions.containsKey("bootstrap");

		// Default behaviors mirroring C++ constructor
		if (!options.generateImmutableCode && !options.generateMutableCode && !options.generateSharedCode)
		{
			// C++ logic in JavaGenerator::Generate:
			// if (!options.generate_immutable_code &&
			// !options.generate_mutable_code && !options.generate_shared_code)
			// {
			// options.generate_immutable_code = true;
			// options.generate_shared_code = true;
			// }
			// Wait, the logic I saw in JavaCodeGenerator earlier was:
			// if (!fileOptions.generateImmutableCode &&
			// !fileOptions.generateMutableCode &&
			// !fileOptions.generateSharedCode) {
			// fileOptions.generateImmutableCode = true;
			// fileOptions.generateSharedCode = true;
			// }
			// I will keep that logic in JavaCodeGenerator or move it here?
			// The C++ struct constructor initializes them to false. The
			// *Generator* class sets defaults if none are set.
			// So I'll keep them false here.
		}

		return options;
	}

	private static Map<String, String> parseGeneratorParameter(String parameter)
	{
		Map<String, String> options = new HashMap<>();
		if (parameter != null && !parameter.isEmpty())
		{
			String[] parts = parameter.split(",");
			for (String part : parts)
			{
				String[] keyValue = part.split("=");
				if (keyValue.length == 1)
				{
					options.put(keyValue[0], "");
				}
				else if (keyValue.length == 2)
				{
					options.put(keyValue[0], keyValue[1]);
				}
			}
		}
		return options;
	}
}
