// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.rubberjam.protobuf.compiler.java;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import java.io.PrintWriter;
import java.util.Map;

/** A utility class for common field generation logic. */
public final class FieldCommon
{

	private FieldCommon()
	{
	}

	public static void setCommonFieldVariables(
			FieldDescriptor descriptor,
			FieldGeneratorInfo info,
			Map<String, String> variables)
	{
		variables.put("field_name", descriptor.getName());
		variables.put("name", info.name);
		variables.put("classname", descriptor.getContainingType().getName());
		variables.put("capitalized_name", info.capitalizedName);
		variables.put("disambiguated_reason", info.disambiguatedReason);
		variables.put("constant_name", StringUtils.fieldConstantName(descriptor));
		variables.put("number", Integer.toString(descriptor.getNumber()));
		variables.put("kt_dsl_builder", "_builder");
		variables.put("{", "");
		variables.put("}", "");
		variables.put("kt_name", getKotlinName(info.name));
		String ktPropertyName = getKotlinPropertyName(info.capitalizedName);
		variables.put("kt_property_name", ktPropertyName);
		variables.put("kt_safe_name", getKotlinSafeName(ktPropertyName));
		variables.put("kt_capitalized_name", getKotlinName(info.capitalizedName));
		variables.put("jvm_synthetic", jvmSynthetic(info.options.jvmDsl));
		if (!descriptor.isRepeated())
		{
			variables.put("annotation_field_type", StringUtils.getFieldTypeName(descriptor.getType()));
		}
		else if (descriptor.isMapField())
		{
			variables.put("annotation_field_type", StringUtils.getFieldTypeName(descriptor.getType()) + "MAP");
		}
		else
		{
			variables.put("annotation_field_type", StringUtils.getFieldTypeName(descriptor.getType()) + "_LIST");
			if (descriptor.isPacked())
			{
				variables.put(
						"annotation_field_type", StringUtils.getFieldTypeName(descriptor.getType()) + "_LIST_PACKED");
			}
		}
	}

	public static void setCommonOneofVariables(
			FieldDescriptor descriptor,
			OneofGeneratorInfo info,
			Map<String, String> variables)
	{
		variables.put("oneof_name", info.name);
		variables.put("oneof_capitalized_name", info.capitalizedName);
		variables.put("oneof_index", Integer.toString(descriptor.getContainingOneof().getIndex()));
		variables.put("oneof_stored_type", getOneofStoredType(descriptor));
		variables.put("set_oneof_case_message", info.name + "Case_ = " + descriptor.getNumber());
		variables.put("clear_oneof_case_message", info.name + "Case_ = 0");
		variables.put("has_oneof_case_message", info.name + "Case_ == " + descriptor.getNumber());
	}

	public static void printExtraFieldInfo(
			Map<String, String> variables, PrintWriter out)
	{
		String reason = variables.get("disambiguated_reason");
		if (reason != null && !reason.isEmpty())
		{
			out.format(
					"// An alternative name is used for field \"%s\" because:\n", variables.get("field_name"));
			out.format("//     %s\n", reason);
		}
	}

	private static boolean isUpper(char c)
	{
		return c >= 'A' && c <= 'Z';
	}

	private static char toLower(char c)
	{
		return isUpper(c) ? (char) (c - 'A' + 'a') : c;
	}

	public static String getKotlinPropertyName(String capitalizedName)
	{
		StringBuilder ktPropertyName = new StringBuilder(capitalizedName);
		int firstNonCapital;
		for (firstNonCapital = 0; firstNonCapital < capitalizedName.length()
				&& isUpper(capitalizedName.charAt(firstNonCapital)); firstNonCapital++)
		{
		}
		int stop = firstNonCapital;
		if (stop > 1 && stop < capitalizedName.length())
		{
			stop--;
		}
		for (int i = 0; i < stop; i++)
		{
			ktPropertyName.setCharAt(i, toLower(ktPropertyName.charAt(i)));
		}
		return ktPropertyName.toString();
	}

	private static String getKotlinName(String name)
	{
		return isForbiddenKotlin(name) ? name + "_" : name;
	}

	private static String getKotlinSafeName(String name)
	{
		return isForbiddenKotlin(name) ? "`" + name + "`" : name;
	}

	private static String jvmSynthetic(boolean jvmDsl)
	{
		return jvmDsl ? "@kotlin.jvm.JvmSynthetic\n" : "";
	}

	private static String getOneofStoredType(FieldDescriptor field)
	{
		JavaType javaType = StringUtils.getJavaType(field);
		switch (javaType)
		{
		case ENUM:
			return "java.lang.Integer";
		case MESSAGE:
			return new ClassNameResolver().getClassName(field.getMessageType(), true);
		default:
			return StringUtils.boxedPrimitiveTypeName(javaType);
		}
	}

	private static boolean isForbiddenKotlin(String name)
	{
		switch (name)
		{
		case "as":
		case "as?":
		case "break":
		case "class":
		case "continue":
		case "do":
		case "else":
		case "false":
		case "for":
		case "fun":
		case "if":
		case "in":
		case "!in":
		case "interface":
		case "is":
		case "!is":
		case "null":
		case "object":
		case "package":
		case "return":
		case "super":
		case "this":
		case "throw":
		case "true":
		case "try":
		case "typealias":
		case "typeof":
		case "val":
		case "var":
		case "when":
		case "while":
			return true;
		default:
			return false;
		}
	}
}
