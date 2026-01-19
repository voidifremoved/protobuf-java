package com.rubberjam.protobuf.compiler;

class StringUtils
{
	static String toUpperCamelCase(String filename)
	{
		String[] parts = filename.split("_");
		String camelCaseString = "";
		for (String part : parts)
		{
			camelCaseString = camelCaseString + toProperCase(part);
		}
		return camelCaseString;
	}

	static String toLowerCamelCase(String filename)
	{
		String camelCaseString = toUpperCamelCase(filename);
		return camelCaseString.substring(0, 1).toLowerCase() + camelCaseString.substring(1);
	}

	static String toProperCase(String s)
	{
		if (s == null || s.isEmpty())
		{
			return "";
		}
		if (s.length() == 1)
		{
			return s.toUpperCase();
		}
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}
}
