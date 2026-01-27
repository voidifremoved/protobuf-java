// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.rubberjam.protobuf.compiler.java;

import com.rubberjam.protobuf.compiler.FieldGeneratorInfo;
import com.rubberjam.protobuf.compiler.ContextVariables;
import com.rubberjam.protobuf.compiler.OneofGeneratorInfo;
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
			FieldGeneratorInfo<Options> info,
			ContextVariables variables)
	{
		variables.setFieldName(descriptor.getName());
		variables.setName(info.name);
		variables.setClassname(descriptor.getContainingType().getName());
		variables.setCapitalizedName(info.capitalizedName);
		variables.setDisambiguatedReason(info.disambiguatedReason);
		variables.setConstantName(StringUtils.fieldConstantName(descriptor));
		variables.setNumber(Integer.toString(descriptor.getNumber()));
		variables.setKtDslBuilder("_builder");
		variables.setKtName(getKotlinName(info.name));
		String ktPropertyName = getKotlinPropertyName(info.capitalizedName);
		variables.setKtPropertyName(ktPropertyName);
		variables.setKtSafeName(getKotlinSafeName(ktPropertyName));
		variables.setKtCapitalizedName(getKotlinName(info.capitalizedName));
		variables.setJvmSynthetic(jvmSynthetic(info.options.jvmDsl));
		if (!descriptor.isRepeated())
		{
			variables.setAnnotationFieldType(StringUtils.getFieldTypeName(descriptor.getType()));
		}
		else if (descriptor.isMapField())
		{
			variables.setAnnotationFieldType(StringUtils.getFieldTypeName(descriptor.getType()) + "MAP");
		}
		else
		{
			variables.setAnnotationFieldType(StringUtils.getFieldTypeName(descriptor.getType()) + "_LIST");
			if (descriptor.isPacked())
			{
				variables.setAnnotationFieldType(
						StringUtils.getFieldTypeName(descriptor.getType()) + "_LIST_PACKED");
			}
		}
	}

	public static void setCommonOneofVariables(
			FieldDescriptor descriptor,
			OneofGeneratorInfo info,
			ContextVariables variables)
	{
		variables.setOneofName(info.name);
		variables.setOneofCapitalizedName(info.capitalizedName);
		variables.setOneofIndex(Integer.toString(descriptor.getContainingOneof().getIndex()));
		variables.setOneofStoredType(getOneofStoredType(descriptor));
		variables.setSetOneofCaseMessage(info.name + "Case_ = " + descriptor.getNumber());
		variables.setClearOneofCaseMessage(info.name + "Case_ = 0");
		variables.setHasOneofCaseMessage(info.name + "Case_ == " + descriptor.getNumber());
	}

	public static void printExtraFieldInfo(
			ContextVariables variables, PrintWriter out)
	{
		String reason = variables.getDisambiguatedReason();
		if (reason != null && !reason.isEmpty())
		{
			out.format(
					"// An alternative name is used for field \"%s\" because:\n", variables.getFieldName());
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
