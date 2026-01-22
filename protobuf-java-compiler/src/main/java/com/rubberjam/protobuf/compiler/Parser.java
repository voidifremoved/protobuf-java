// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
//
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file or at
// https://developers.google.com/open-source/licenses/bsd

package com.rubberjam.protobuf.compiler;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.UninterpretedOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements parsing of .proto files to FileDescriptorProtos.
 */
public class Parser
{

	private Tokenizer tokenizer;
	private ErrorCollector errorCollector;
	private SourceLocationTable sourceLocationTable;
	private boolean hadErrors;
	private String syntax;

	private static final Map<String, FieldDescriptorProto.Type> typeNameToType = new HashMap<>();

	static
	{
		typeNameToType.put("double", FieldDescriptorProto.Type.TYPE_DOUBLE);
		typeNameToType.put("float", FieldDescriptorProto.Type.TYPE_FLOAT);
		typeNameToType.put("int32", FieldDescriptorProto.Type.TYPE_INT32);
		typeNameToType.put("int64", FieldDescriptorProto.Type.TYPE_INT64);
		typeNameToType.put("uint32", FieldDescriptorProto.Type.TYPE_UINT32);
		typeNameToType.put("uint64", FieldDescriptorProto.Type.TYPE_UINT64);
		typeNameToType.put("sint32", FieldDescriptorProto.Type.TYPE_SINT32);
		typeNameToType.put("sint64", FieldDescriptorProto.Type.TYPE_SINT64);
		typeNameToType.put("fixed32", FieldDescriptorProto.Type.TYPE_FIXED32);
		typeNameToType.put("fixed64", FieldDescriptorProto.Type.TYPE_FIXED64);
		typeNameToType.put("sfixed32", FieldDescriptorProto.Type.TYPE_SFIXED32);
		typeNameToType.put("sfixed64", FieldDescriptorProto.Type.TYPE_SFIXED64);
		typeNameToType.put("bool", FieldDescriptorProto.Type.TYPE_BOOL);
		typeNameToType.put("string", FieldDescriptorProto.Type.TYPE_STRING);
		typeNameToType.put("bytes", FieldDescriptorProto.Type.TYPE_BYTES);
		typeNameToType.put("group", FieldDescriptorProto.Type.TYPE_GROUP);
	}

	public Parser(ErrorCollector errorCollector, SourceLocationTable sourceLocationTable)
	{
		this.errorCollector = errorCollector;
		this.sourceLocationTable = sourceLocationTable;
	}

	/**
	 * Parses the input from the tokenizer and populates the fileBuilder.
	 *
	 * @param tokenizer
	 *            The tokenizer to read from.
	 * @param fileBuilder
	 *            The builder to populate.
	 * @return true if parsing was successful, false otherwise.
	 */
	public boolean parse(Tokenizer tokenizer, FileDescriptorProto.Builder fileBuilder)
	{
		this.tokenizer = tokenizer;
		this.hadErrors = false;

		if (tokenizer.current().type == Tokenizer.TokenType.START)
		{
			tokenizer.next();
		}

		// Parse syntax identifier, if present.
		if (lookingAt("syntax"))
		{
			parseSyntax(fileBuilder);
		}
		else
		{
			syntax = "proto2";
		}

		while (tokenizer.current().type != Tokenizer.TokenType.END)
		{
			if (!parseTopLevelStatement(fileBuilder))
			{
				// Error recovery: skip to next declaration
				skipStatement();
			}
		}

		return !hadErrors;
	}

	private boolean parseTopLevelStatement(FileDescriptorProto.Builder fileBuilder)
	{
		if (tryConsume(";"))
		{
			// empty statement
			return true;
		}

		LocationRecorder location = new LocationRecorder(this);
		if (lookingAt("package"))
		{
			return parsePackage(fileBuilder, location);
		}
		else if (lookingAt("import"))
		{
			return parseImport(fileBuilder, location);
		}
		else if (lookingAt("option"))
		{
			return parseOption(fileBuilder.getOptionsBuilder(), location);
		}
		else if (lookingAt("message"))
		{
			return parseMessageDefinition(fileBuilder.addMessageTypeBuilder(), location);
		}
		else if (lookingAt("enum"))
		{
			return parseEnumDefinition(fileBuilder.addEnumTypeBuilder(), location);
		}
		else if (lookingAt("service"))
		{
			return parseServiceDefinition(fileBuilder.addServiceBuilder(), location);
		}
		else
		{
			recordError("Expected top-level statement (e.g. \"message\").");
			return false;
		}
	}

	private boolean parseSyntax(FileDescriptorProto.Builder fileBuilder)
	{
		LocationRecorder location = new LocationRecorder(this);
		location.addPath(FileDescriptorProto.SYNTAX_FIELD_NUMBER);
		tokenizer.next(); // consume "syntax"
		consume("=", "Expected '=' after 'syntax'.");
		Tokenizer.Token syntaxToken = tokenizer.current();
		String syntaxIdentifier = consumeString("Expected syntax identifier.");
		consume(";", "Expected ';' after syntax declaration.");
		location.end();

		this.syntax = syntaxIdentifier;
		fileBuilder.setSyntax(syntaxIdentifier);

		if (!syntaxIdentifier.equals("proto2") && !syntaxIdentifier.equals("proto3"))
		{
			recordError(syntaxToken, "Unrecognized syntax identifier \"" + syntaxIdentifier +
					"\". This parser only recognizes \"proto2\" and \"proto3\".");
		}

		return true;
	}

	private boolean parsePackage(FileDescriptorProto.Builder fileBuilder, LocationRecorder location)
	{
		location.addPath(FileDescriptorProto.PACKAGE_FIELD_NUMBER);
		tokenizer.next(); // consume "package"
		StringBuilder packageName = new StringBuilder();
		packageName.append(consumeIdentifier("Expected package name."));
		while (tryConsume("."))
		{
			packageName.append(".");
			packageName.append(consumeIdentifier("Expected package name component."));
		}
		consume(";", "Expected ';' after package declaration.");
		fileBuilder.setPackage(packageName.toString());
		location.end();
		return true;
	}

	private boolean parseImport(FileDescriptorProto.Builder fileBuilder, LocationRecorder location)
	{
		tokenizer.next(); // consume "import"
		boolean isPublic = tryConsume("public");
		String importFile = consumeString("Expected a string naming the file to import.");
		consume(";", "Expected ';' after import declaration.");

		fileBuilder.addDependency(importFile);
		if (isPublic)
		{
			location.addPath(FileDescriptorProto.PUBLIC_DEPENDENCY_FIELD_NUMBER);
			fileBuilder.addPublicDependency(fileBuilder.getDependencyCount() - 1);
		}
		else
		{
			location.addPath(FileDescriptorProto.DEPENDENCY_FIELD_NUMBER);
		}
		location.end();
		return true;
	}

	private boolean parseOption(FileOptions.Builder optionsBuilder, LocationRecorder location)
	{
		tokenizer.next(); // consume "option"
		String optionName = consumeIdentifier("Expected option name.");
		if (optionName.equals("java_package"))
		{
			consume("=", "Expected '=' after option name.");
			optionsBuilder.setJavaPackage(consumeString("Expected string value."));
			consume(";", "Expected ';' after option declaration.");
			return true;
		}
		else if (optionName.equals("java_outer_classname"))
		{
			consume("=", "Expected '=' after option name.");
			optionsBuilder.setJavaOuterClassname(consumeString("Expected string value."));
			consume(";", "Expected ';' after option declaration.");
			return true;
		}
		else if (optionName.equals("optimize_for"))
		{
			consume("=", "Expected '=' after option name.");
			String optimizeFor = consumeIdentifier("Expected identifier value.");
			if (optimizeFor.equals("SPEED"))
			{
				optionsBuilder.setOptimizeFor(FileOptions.OptimizeMode.SPEED);
			}
			else if (optimizeFor.equals("CODE_SIZE"))
			{
				optionsBuilder.setOptimizeFor(FileOptions.OptimizeMode.CODE_SIZE);
			}
			else if (optimizeFor.equals("LITE_RUNTIME"))
			{
				optionsBuilder.setOptimizeFor(FileOptions.OptimizeMode.LITE_RUNTIME);
			}
			else
			{
				recordError("Invalid value for optimize_for option.");
			}
			consume(";", "Expected ';' after option declaration.");
			return true;
		}
		else if (optionName.equals("cc_generic_services"))
		{
			consume("=", "Expected '=' after option name.");
			optionsBuilder.setCcGenericServices(consumeBoolean("Expected 'true' or 'false'."));
			consume(";", "Expected ';' after option declaration.");
			return true;
		}
		else if (optionName.equals("java_generic_services"))
		{
			consume("=", "Expected '=' after option name.");
			optionsBuilder.setJavaGenericServices(consumeBoolean("Expected 'true' or 'false'."));
			consume(";", "Expected ';' after option declaration.");
			return true;
		}
		else
		{
			location.addPath(FileOptions.UNINTERPRETED_OPTION_FIELD_NUMBER);
			UninterpretedOption.Builder option = UninterpretedOption.newBuilder();
			option.addNameBuilder().setNamePart(optionName).setIsExtension(false);
			while (tryConsume("."))
			{
				option.addNameBuilder().setNamePart(consumeIdentifier("Expected option name component."))
						.setIsExtension(false);
			}
			consume("=", "Expected '=' after option name.");
			parseOptionValue(option);
			consume(";", "Expected ';' after option declaration.");
			optionsBuilder.addUninterpretedOption(option);
			location.end();
			return true;
		}
	}

	private boolean consumeBoolean(String error)
	{
		if (lookingAt("true"))
		{
			tokenizer.next();
			return true;
		}
		else if (lookingAt("false"))
		{
			tokenizer.next();
			return false;
		}
		else
		{
			recordError(error);
			return false;
		}
	}

	private void parseOptionValue(UninterpretedOption.Builder option)
	{
		if (lookingAt("{"))
		{
			// Aggregate value.
			tokenizer.next();
			StringBuilder sb = new StringBuilder();
			while (!tryConsume("}"))
			{
				sb.append(tokenizer.current().text);
				tokenizer.next();
			}
			option.setAggregateValue(sb.toString());
		}
		else if (tokenizer.current().type == Tokenizer.TokenType.STRING)
		{
			option.setStringValue(ByteString.copyFromUtf8(consumeString("Expected string value.")));
		}
		else if (tokenizer.current().type == Tokenizer.TokenType.IDENTIFIER)
		{
			option.setIdentifierValue(consumeIdentifier("Expected identifier value."));
		}
		else if (tokenizer.current().type == Tokenizer.TokenType.INTEGER)
		{
			option.setPositiveIntValue(consumeInteger("Expected integer value."));
		}
		else if (tokenizer.current().type == Tokenizer.TokenType.FLOAT)
		{
			option.setDoubleValue(consumeDouble("Expected double value."));
		}
		else
		{
			recordError("Invalid option value.");
		}
	}

	private boolean parseMessageDefinition(DescriptorProto.Builder messageBuilder,
			LocationRecorder location)
	{
		tokenizer.next(); // consume "message"
		messageBuilder.setName(consumeIdentifier("Expected message name."));
		boolean result = parseMessageBlock(messageBuilder, location);
		location.end();
		return result;
	}

	private boolean parseMessageBlock(DescriptorProto.Builder messageBuilder, LocationRecorder location)
	{
		consume("{", "Expected '{' to start message block.");
		while (!tryConsume("}"))
		{
			if (tokenizer.current().type == Tokenizer.TokenType.END)
			{
				recordError("Reached end of input in message definition (missing '}').");
				return false;
			}
			if (!parseMessageStatement(messageBuilder, location))
			{
				skipStatement();
			}
		}
		return true;
	}

	private boolean parseMessageStatement(DescriptorProto.Builder messageBuilder,
			LocationRecorder location)
	{
		if (tryConsume(";"))
		{
			// empty statement
			return true;
		}
		if (lookingAt("oneof"))
		{
			return parseOneof(messageBuilder, location);
		}
		if (lookingAt("message"))
		{
			return parseMessageDefinition(messageBuilder.addNestedTypeBuilder(), location);
		}
		if (lookingAt("enum"))
		{
			return parseEnumDefinition(messageBuilder.addEnumTypeBuilder(), location);
		}
		return parseField(messageBuilder.addFieldBuilder(), -1, location);
	}

	private boolean parseOneof(DescriptorProto.Builder messageBuilder, LocationRecorder location)
	{
		tokenizer.next(); // consume "oneof"
		String oneofName = consumeIdentifier("Expected oneof name.");
		int oneofIndex = messageBuilder.getOneofDeclCount();
		messageBuilder.addOneofDeclBuilder().setName(oneofName);

		consume("{", "Expected '{' to start oneof block.");
		while (!tryConsume("}"))
		{
			if (tokenizer.current().type == Tokenizer.TokenType.END)
			{
				recordError("Reached end of input in oneof definition (missing '}').");
				return false;
			}
			parseField(messageBuilder.addFieldBuilder(), oneofIndex, location);
		}
		return true;
	}

	private boolean parseField(
			FieldDescriptorProto.Builder fieldBuilder, int oneofIndex, LocationRecorder location)
	{
		if (oneofIndex != -1)
		{
			if (lookingAt("required") || lookingAt("optional") || lookingAt("repeated"))
			{
				recordError("Fields in oneofs must not have labels (required / optional / repeated).");
				tokenizer.next(); // Consume the label to recover
			}
			fieldBuilder.setOneofIndex(oneofIndex);
			fieldBuilder.setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL);
		}
		else
		{
			parseLabel(fieldBuilder);
		}
		parseType(fieldBuilder);
		fieldBuilder.setName(consumeIdentifier("Expected field name."));
		consume("=", "Missing field number.");
		fieldBuilder.setNumber(consumeInteger("Expected field number."));
		if (tryConsume("["))
		{
			parseFieldOptions(fieldBuilder, location);
			consume("]", "Expected ']' to end field options.");
		}
		consume(";", "Expected ';' after field declaration.");
		return true;
	}

	private void parseFieldOptions(FieldDescriptorProto.Builder fieldBuilder,
			LocationRecorder location)
	{
		do
		{
			String optionName = consumeIdentifier("Expected option name.");
			if (optionName.equals("default"))
			{
				consume("=", "Expected '=' after option name.");
				fieldBuilder.setDefaultValue(parseDefaultValue());
			}
			else
			{
				UninterpretedOption.Builder option = UninterpretedOption.newBuilder();
				option.addNameBuilder().setNamePart(optionName).setIsExtension(false);
				consume("=", "Expected '=' after option name.");
				parseOptionValue(option);
				fieldBuilder.getOptionsBuilder().addUninterpretedOption(option);
			}
		}
		while (tryConsume(","));
	}

	private String parseDefaultValue()
	{
		if (tokenizer.current().type == Tokenizer.TokenType.STRING)
		{
			return consumeString("Expected string value.");
		}
		else if (tokenizer.current().type == Tokenizer.TokenType.IDENTIFIER)
		{
			String text = tokenizer.current().text;
			tokenizer.next();
			return text;
		}
		else if (tokenizer.current().type == Tokenizer.TokenType.INTEGER)
		{
			String text = tokenizer.current().text;
			tokenizer.next();
			return text;
		}
		else if (tokenizer.current().type == Tokenizer.TokenType.FLOAT)
		{
			String text = tokenizer.current().text;
			tokenizer.next();
			return text;
		}
		else
		{
			recordError("Expected default value.");
			return "";
		}
	}

	private void parseLabel(FieldDescriptorProto.Builder fieldBuilder)
	{
		if (tryConsume("optional"))
		{
			fieldBuilder.setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL);
		}
		else if (tryConsume("repeated"))
		{
			fieldBuilder.setLabel(FieldDescriptorProto.Label.LABEL_REPEATED);
		}
		else if (tryConsume("required"))
		{
			fieldBuilder.setLabel(FieldDescriptorProto.Label.LABEL_REQUIRED);
		}
		else
		{
			if ("proto3".equals(syntax))
			{
				fieldBuilder.setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL);
			}
		}
	}

	private void parseType(FieldDescriptorProto.Builder fieldBuilder)
	{
		String typeName = consumeIdentifier("Expected field type.");
		FieldDescriptorProto.Type type = typeNameToType.get(typeName);
		if (type != null)
		{
			fieldBuilder.setType(type);
		}
		else
		{
			fieldBuilder.setTypeName(typeName);
		}
	}

	private boolean parseEnumDefinition(EnumDescriptorProto.Builder enumBuilder,
			LocationRecorder location)
	{
		tokenizer.next(); // consume "enum"
		enumBuilder.setName(consumeIdentifier("Expected enum name."));
		boolean result = parseEnumBlock(enumBuilder, location);
		location.end();
		return result;
	}

	private boolean parseEnumBlock(EnumDescriptorProto.Builder enumBuilder, LocationRecorder location)
	{
		consume("{", "Expected '{' to start enum block.");
		while (!tryConsume("}"))
		{
			if (tokenizer.current().type == Tokenizer.TokenType.END)
			{
				recordError("Reached end of input in enum definition (missing '}').");
				return false;
			}
			if (!parseEnumStatement(enumBuilder, location))
			{
				skipStatement();
			}
		}
		return true;
	}

	private boolean parseEnumStatement(EnumDescriptorProto.Builder enumBuilder,
			LocationRecorder location)
	{
		if (tryConsume(";"))
		{
			// empty statement
			return true;
		}
		return parseEnumConstant(enumBuilder.addValueBuilder(), location);
	}

	private boolean parseEnumConstant(EnumValueDescriptorProto.Builder enumValueBuilder,
			LocationRecorder location)
	{
		enumValueBuilder.setName(consumeIdentifier("Expected enum constant name."));
		consume("=", "Missing numeric value for enum constant.");
		enumValueBuilder.setNumber(consumeInteger("Expected integer."));
		if (tryConsume("["))
		{
			parseEnumConstantOptions(enumValueBuilder, location);
			consume("]", "Expected ']' to end enum constant options.");
		}
		consume(";", "Expected ';' after enum constant declaration.");
		return true;
	}

	private void parseEnumConstantOptions(EnumValueDescriptorProto.Builder enumValueBuilder,
			LocationRecorder location)
	{
		do
		{
			String optionName = consumeIdentifier("Expected option name.");
			UninterpretedOption.Builder option = UninterpretedOption.newBuilder();
			option.addNameBuilder().setNamePart(optionName).setIsExtension(false);
			consume("=", "Expected '=' after option name.");
			parseOptionValue(option);
			enumValueBuilder.getOptionsBuilder().addUninterpretedOption(option);
		}
		while (tryConsume(","));
	}

	private boolean parseServiceDefinition(ServiceDescriptorProto.Builder serviceBuilder,
			LocationRecorder location)
	{
		tokenizer.next(); // consume "service"
		serviceBuilder.setName(consumeIdentifier("Expected service name."));
		boolean result = parseServiceBlock(serviceBuilder, location);
		location.end();
		return result;
	}

	private boolean parseServiceBlock(ServiceDescriptorProto.Builder serviceBuilder,
			LocationRecorder location)
	{
		consume("{", "Expected '{' to start service block.");
		while (!tryConsume("}"))
		{
			if (tokenizer.current().type == Tokenizer.TokenType.END)
			{
				recordError("Reached end of input in service definition (missing '}').");
				return false;
			}
			if (!parseServiceStatement(serviceBuilder, location))
			{
				skipStatement();
			}
		}
		return true;
	}

	private boolean parseServiceStatement(ServiceDescriptorProto.Builder serviceBuilder,
			LocationRecorder location)
	{
		if (tryConsume(";"))
		{
			// empty statement
			return true;
		}
		return parseServiceMethod(serviceBuilder.addMethodBuilder(), location);
	}

	private boolean parseServiceMethod(MethodDescriptorProto.Builder methodBuilder,
			LocationRecorder location)
	{
		consume("rpc", "Expected 'rpc' to start method definition.");
		methodBuilder.setName(consumeIdentifier("Expected method name."));
		consume("(", "Expected '(' to start method request type.");
		methodBuilder.setInputType(consumeIdentifier("Expected method request type."));
		consume(")", "Expected ')' to end method request type.");
		consume("returns", "Expected 'returns' after method request type.");
		consume("(", "Expected '(' to start method response type.");
		methodBuilder.setOutputType(consumeIdentifier("Expected method response type."));
		consume(")", "Expected ')' to end method response type.");
		if (tryConsume("{"))
		{
			parseMethodOptions(methodBuilder, location);
			consume("}", "Expected '}' to end method options.");
		}
		else
		{
			consume(";", "Expected ';' or '{' after method definition.");
		}
		return true;
	}

	private void parseMethodOptions(MethodDescriptorProto.Builder methodBuilder,
			LocationRecorder location)
	{
		while (!lookingAt("}"))
		{
			String optionName = consumeIdentifier("Expected option name.");
			UninterpretedOption.Builder option = UninterpretedOption.newBuilder();
			option.addNameBuilder().setNamePart(optionName).setIsExtension(false);
			consume("=", "Expected '=' after option name.");
			parseOptionValue(option);
			methodBuilder.getOptionsBuilder().addUninterpretedOption(option);
			tryConsume(";");
		}
	}

	private void skipStatement()
	{
		while (tokenizer.current().type != Tokenizer.TokenType.END)
		{
			if (tryConsume(";"))
			{
				return;
			}
			if (tokenizer.current().text.equals("{"))
			{
				skipRestOfBlock();
				return;
			}
			if (tokenizer.current().text.equals("}"))
			{
				return;
			}
			tokenizer.next();
		}
	}

	private void skipRestOfBlock()
	{
		int braceDepth = 1;
		tokenizer.next(); // Consume '{'
		while (tokenizer.current().type != Tokenizer.TokenType.END)
		{
			if (tokenizer.current().text.equals("{"))
			{
				braceDepth++;
			}
			else if (tokenizer.current().text.equals("}"))
			{
				braceDepth--;
				if (braceDepth == 0)
				{
					tokenizer.next();
					return;
				}
			}
			tokenizer.next();
		}
	}

	private boolean lookingAt(String text)
	{
		return tokenizer.current().text.equals(text);
	}

	private boolean tryConsume(String text)
	{
		if (lookingAt(text))
		{
			tokenizer.next();
			return true;
		}
		return false;
	}

	private void consume(String text, String error)
	{
		if (!tryConsume(text))
		{
			recordError(error);
		}
	}

	private String consumeIdentifier(String error)
	{
		if (tokenizer.current().type == Tokenizer.TokenType.IDENTIFIER)
		{
			String text = tokenizer.current().text;
			tokenizer.next();
			return text;
		}
		else
		{
			recordError(error);
			return "";
		}
	}

	private String consumeString(String error)
	{
		if (tokenizer.current().type == Tokenizer.TokenType.STRING)
		{
			String text = tokenizer.current().text;
			tokenizer.next();
			return text;
		}
		else
		{
			recordError(error);
			return "";
		}
	}

	private int consumeInteger(String error)
	{
		if (tokenizer.current().type == Tokenizer.TokenType.INTEGER)
		{
			try
			{
				int value = Integer.parseInt(tokenizer.current().text);
				tokenizer.next();
				return value;
			}
			catch (NumberFormatException e)
			{
				recordError("Invalid integer literal.");
				return 0;
			}
		}
		else
		{
			recordError(error);
			return 0;
		}
	}

	private double consumeDouble(String error)
	{
		if (tokenizer.current().type == Tokenizer.TokenType.FLOAT)
		{
			try
			{
				double value = Double.parseDouble(tokenizer.current().text);
				tokenizer.next();
				return value;
			}
			catch (NumberFormatException e)
			{
				recordError("Invalid double literal.");
				return 0;
			}
		}
		else
		{
			recordError(error);
			return 0;
		}
	}

	private void recordError(String message)
	{
		hadErrors = true;
		errorCollector.recordError(tokenizer.current().line, tokenizer.current().column, message);
	}

	private void recordError(Tokenizer.Token token, String message)
	{
		hadErrors = true;
		errorCollector.recordError(token.line, token.column, message);
	}

	private class LocationRecorder
	{
		private final Parser parser;
		private final List<Integer> path = new ArrayList<>();
		private int startLine;
		private int startColumn;
		private int endLine;
		private int endColumn;

		LocationRecorder(Parser parser)
		{
			this.parser = parser;
			this.startLine = parser.tokenizer.current().line;
			this.startColumn = parser.tokenizer.current().column;
		}

		void addPath(int pathComponent)
		{
			path.add(pathComponent);
		}

		void end()
		{
			this.endLine = parser.tokenizer.previous().line;
			this.endColumn = parser.tokenizer.previous().column;

			int[] pathArray = new int[path.size()];
			for (int i = 0; i < path.size(); i++)
			{
				pathArray[i] = path.get(i);
			}

			int[] spanArray = new int[] { startLine, startColumn, endLine, endColumn };
			parser.sourceLocationTable.add(pathArray, spanArray);
		}
	}
}
