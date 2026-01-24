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
import com.google.protobuf.DescriptorProtos.SourceCodeInfo;
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
	private String packageName = "";
	private final java.util.Stack<String> messageStack = new java.util.Stack<>();

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

		if (!hadErrors)
		{
			fileBuilder.setSourceCodeInfo(
					SourceCodeInfo.newBuilder().addAllLocation(sourceLocationTable.getLocations()));
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
			location.addPath(FileDescriptorProto.MESSAGE_TYPE_FIELD_NUMBER);
			location.addPath(fileBuilder.getMessageTypeCount());
			return parseMessageDefinition(fileBuilder.addMessageTypeBuilder(), location);
		}
		else if (lookingAt("enum"))
		{
			location.addPath(FileDescriptorProto.ENUM_TYPE_FIELD_NUMBER);
			location.addPath(fileBuilder.getEnumTypeCount());
			return parseEnumDefinition(fileBuilder.addEnumTypeBuilder(), location);
		}
		else if (lookingAt("service"))
		{
			location.addPath(FileDescriptorProto.SERVICE_FIELD_NUMBER);
			location.addPath(fileBuilder.getServiceCount());
			return parseServiceDefinition(fileBuilder.addServiceBuilder(), location);
		}
		else if (lookingAt("extend"))
		{
			// Top-level extend statement
			return parseTopLevelExtend(fileBuilder, location);
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
		this.packageName = packageName.toString();
		fileBuilder.setPackage(this.packageName);
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
		String messageName = consumeIdentifier("Expected message name.");
		messageBuilder.setName(messageName);
		messageStack.push(messageName);
		boolean result = parseMessageBlock(messageBuilder, location.path);
		messageStack.pop();
		location.end();
		return result;
	}

	private boolean parseMessageBlock(DescriptorProto.Builder messageBuilder, List<Integer> scopePath)
	{
		consume("{", "Expected '{' to start message block.");
		while (!tryConsume("}"))
		{
			if (tokenizer.current().type == Tokenizer.TokenType.END)
			{
				recordError("Reached end of input in message definition (missing '}').");
				return false;
			}
			if (!parseMessageStatement(messageBuilder, scopePath))
			{
				skipStatement();
			}
		}
		return true;
	}

	private boolean parseMessageStatement(DescriptorProto.Builder messageBuilder, List<Integer> scopePath)
	{
		// Match C++ ParseMessageStatement behavior
		if (tryConsume(";"))
		{
			// empty statement
			return true;
		}

		LocationRecorder location = new LocationRecorder(this, scopePath);

		if (lookingAt("message"))
		{
			location.addPath(DescriptorProto.NESTED_TYPE_FIELD_NUMBER);
			location.addPath(messageBuilder.getNestedTypeCount());
			return parseMessageDefinition(messageBuilder.addNestedTypeBuilder(), location);
		}
		if (lookingAt("enum"))
		{
			location.addPath(DescriptorProto.ENUM_TYPE_FIELD_NUMBER);
			location.addPath(messageBuilder.getEnumTypeCount());
			return parseEnumDefinition(messageBuilder.addEnumTypeBuilder(), location);
		}
		if (lookingAt("extensions"))
		{
			return parseExtensions(messageBuilder, location);
		}
		if (lookingAt("reserved"))
		{
			return parseReserved(messageBuilder, location);
		}
		if (lookingAt("extend"))
		{
			return parseExtend(messageBuilder, scopePath);
		}
		if (lookingAt("option"))
		{
			// TODO: implement option parsing
			recordError("Option parsing not yet implemented.");
			return false;
		}
		if (lookingAt("oneof"))
		{
			location.addPath(DescriptorProto.ONEOF_DECL_FIELD_NUMBER);
			location.addPath(messageBuilder.getOneofDeclCount());
			boolean result = parseOneof(messageBuilder, location, scopePath);
			if (result)
			{
				location.end();
			}
			return result;
		}
		// Default: try to parse as a field
		location.addPath(DescriptorProto.FIELD_FIELD_NUMBER);
		location.addPath(messageBuilder.getFieldCount());
		boolean result = parseField(messageBuilder.addFieldBuilder(), -1, location, messageBuilder);
		if (result)
		{
			location.end();
		}
		return result;
	}

	private boolean parseOneof(DescriptorProto.Builder messageBuilder, LocationRecorder location, List<Integer> scopePath)
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

			LocationRecorder fieldLocation = new LocationRecorder(this, scopePath);
			fieldLocation.addPath(DescriptorProto.FIELD_FIELD_NUMBER);
			fieldLocation.addPath(messageBuilder.getFieldCount());
			boolean result = parseField(messageBuilder.addFieldBuilder(), oneofIndex, fieldLocation, messageBuilder);
			if (result)
			{
				fieldLocation.end();
			}
		}
		return true;
	}

	private boolean parseField(
			FieldDescriptorProto.Builder fieldBuilder, int oneofIndex, LocationRecorder location, DescriptorProto.Builder messageBuilder)
	{
		// Check if we can actually parse a field - if not, return false to allow skipStatement
		// Match C++ ParseMessageFieldNoLabel behavior
		if (tokenizer.current().type == Tokenizer.TokenType.END)
		{
			return false;
		}
		
		// If we're at a closing brace or other statement delimiter, this isn't a field
		if (tokenizer.current().text.equals("}") || tokenizer.current().text.equals(";"))
		{
			return false;
		}
		
		// Match C++ ParseMessageFieldNoLabel: handle "map<...>" specially BEFORE parsing label
		// Map fields cannot have labels, so we need to check for map first
		boolean typeParsed = false;
		MapFieldInfo mapFieldInfo = null;
		boolean isMapField = false;
		if (tryConsume("map"))
		{
			if (lookingAt("<"))
			{
				// This is a map field - check for label first (map fields can't have labels)
				if (oneofIndex != -1)
				{
					recordError("Map fields are not allowed in oneofs.");
					return false;
				}
				if (lookingAt("required") || lookingAt("optional") || lookingAt("repeated"))
				{
					recordError("Field labels (required/optional/repeated) are not allowed on map fields.");
					return false;
				}
				
				// Parse as map type - store info for later entry generation
				mapFieldInfo = new MapFieldInfo();
				if (!parseMapType(fieldBuilder, location, mapFieldInfo))
				{
					return false;
				}
				// Map fields are implicitly repeated
				fieldBuilder.setLabel(FieldDescriptorProto.Label.LABEL_REPEATED);
				typeParsed = true;
				isMapField = true;
			}
			else
			{
				// False positive - "map" is a regular type name
				fieldBuilder.setTypeName("map");
				typeParsed = true;
			}
		}
		
		// Parse label only if this is not a map field
		if (!isMapField)
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
		}
		
		// Parse type if not already parsed (match C++ DO(ParseType(...)) behavior)
		if (!typeParsed)
		{
			if (!parseType(fieldBuilder))
			{
				return false; // Match C++ DO macro - return false immediately on failure
			}
		}
		
		// Parse field name - match C++ DO(ConsumeIdentifier(...)) behavior  
		// If consumeIdentifier fails, return false immediately (like C++ DO macro)
		String fieldName = consumeIdentifier("Expected field name.");
		if (fieldName.isEmpty())
		{
			return false;
		}
		fieldBuilder.setName(fieldName);
		
		// Generate map entry if this is a map field (after field name is known)
		if (mapFieldInfo != null)
		{
			generateMapEntry(fieldBuilder, mapFieldInfo, messageBuilder);
		}
		
		// Parse '=' and field number - match C++ DO macro behavior
		if (!tryConsume("="))
		{
			recordError("Missing field number.");
			return false;
		}
		
		// Match C++ DO(ConsumeInteger(...)) - if not an integer, return false
		if (tokenizer.current().type != Tokenizer.TokenType.INTEGER)
		{
			recordError("Expected field number.");
			return false;
		}
		int fieldNumber = consumeInteger("Expected field number.");
		fieldBuilder.setNumber(fieldNumber);
		
		// Parse options - match C++ DO(ParseFieldOptions(...)) behavior
		if (tryConsume("["))
		{
			if (!parseFieldOptions(fieldBuilder, location))
			{
				return false;
			}
			if (!tryConsume("]"))
			{
				recordError("Expected ']' to end field options.");
				return false;
			}
		}
		
		// Match C++ behavior: groups don't end with semicolon, they have a message block
		if (fieldBuilder.hasType() && fieldBuilder.getType() == FieldDescriptorProto.Type.TYPE_GROUP)
		{
			// Group fields have a message block instead of semicolon
			// Match C++ behavior: group name must start with capital letter
			String groupName = fieldBuilder.getName();
			if (groupName.isEmpty() || groupName.charAt(0) < 'A' || groupName.charAt(0) > 'Z')
			{
				recordError("Group names must start with a capital letter.");
			}
			
			// Create the group message type with the original (capitalized) name
			DescriptorProto.Builder groupBuilder = messageBuilder.addNestedTypeBuilder();
			groupBuilder.setName(groupName);
			
			// Lowercase the field name (match C++ absl::AsciiStrToLower behavior)
			String fieldNameLower = groupName.substring(0, 1).toLowerCase() + 
			                         (groupName.length() > 1 ? groupName.substring(1) : "");
			fieldBuilder.setName(fieldNameLower);
			
			// Set the type_name to the group name (original capitalized name)
			fieldBuilder.setTypeName(groupName);
			
			// Parse the group body - match C++ LookingAt("{") check
			// parseMessageBlock will consume the "{", so we just check it exists
			if (!lookingAt("{"))
			{
				recordError("Missing group body.");
				return false;
			}
			
			// Parse the message block for the group (this will consume the "{")
			if (!parseMessageBlock(groupBuilder, location.path))
			{
				return false;
			}
		}
		else
		{
			// Regular field ends with semicolon
			if (!tryConsume(";"))
			{
				recordError("Expected ';' after field declaration.");
				return false;
			}
		}
		
		return true;
	}
	
	// Helper class to store map field information
	private static class MapFieldInfo
	{
		FieldDescriptorProto.Type keyType;
		String keyTypeName;
		FieldDescriptorProto.Type valueType;
		String valueTypeName;
	}

	private boolean parseFieldOptions(FieldDescriptorProto.Builder fieldBuilder,
			LocationRecorder location)
	{
		// Match C++ ParseFieldOptions behavior
		do
		{
			if (lookingAt("default"))
			{
				// Match C++ ParseDefaultAssignment - use DO macro pattern
				if (!tryConsume("default"))
				{
					return false;
				}
				if (!tryConsume("="))
				{
					recordError("Expected '=' after default.");
					return false;
				}
				String defaultValue = parseDefaultValue(fieldBuilder);
				if (defaultValue == null)
				{
					return false; // Match C++ DO macro - return false on failure
				}
				fieldBuilder.setDefaultValue(defaultValue);
			}
			else if (lookingAt("json_name"))
			{
				// Match C++ ParseJsonName
				if (!tryConsume("json_name"))
				{
					return false;
				}
				if (!tryConsume("="))
				{
					recordError("Expected '=' after json_name.");
					return false;
				}
				// Parse string value for json_name
				if (tokenizer.current().type != Tokenizer.TokenType.STRING)
				{
					recordError("Expected string for JSON name.");
					return false;
				}
				consumeString("Expected string for JSON name.");
				// Note: json_name is stored in field.json_name, not in options
				// For now, we'll just consume it
			}
			else if (lookingAt("deprecated"))
			{
				tokenizer.next(); // consume "deprecated"
				if (!tryConsume("="))
				{
					recordError("Expected '=' after deprecated.");
					return false;
				}
				boolean value = consumeBoolean("Expected boolean.");
				fieldBuilder.getOptionsBuilder().setDeprecated(value);
			}
			else if (lookingAt("packed"))
			{
				tokenizer.next(); // consume "packed"
				if (!tryConsume("="))
				{
					recordError("Expected '=' after packed.");
					return false;
				}
				boolean value = consumeBoolean("Expected boolean.");
				fieldBuilder.getOptionsBuilder().setPacked(value);
			}
			else
			{
				// Regular option - match C++ ParseOption
				String optionName = consumeIdentifier("Expected option name.");
				if (optionName.isEmpty())
				{
					return false; // Match C++ DO macro
				}
				if (!tryConsume("="))
				{
					recordError("Expected '=' after option name.");
					return false;
				}
				UninterpretedOption.Builder option = UninterpretedOption.newBuilder();
				option.addNameBuilder().setNamePart(optionName).setIsExtension(false);
				parseOptionValue(option);
				fieldBuilder.getOptionsBuilder().addUninterpretedOption(option);
			}
		}
		while (tryConsume(","));
		
		return true;
	}

	private String parseDefaultValue(FieldDescriptorProto.Builder fieldBuilder)
	{
		// Match C++ ParseDefaultAssignment behavior - parse based on field type
		if (!fieldBuilder.hasType() && fieldBuilder.getTypeName().isEmpty())
		{
			// Field has a type name but we don't know if it's message or enum yet
			// Just take the current token as the default value (match C++ behavior)
			String text = tokenizer.current().text;
			tokenizer.next();
			return text;
		}
		
		// Handle based on field type
		if (fieldBuilder.hasType())
		{
			FieldDescriptorProto.Type type = fieldBuilder.getType();
			switch (type)
			{
				case TYPE_INT32:
				case TYPE_INT64:
				case TYPE_SINT32:
				case TYPE_SINT64:
				case TYPE_SFIXED32:
				case TYPE_SFIXED64:
					// Match C++ behavior - handle negative sign first
					StringBuilder signedValue = new StringBuilder();
					if (tryConsume("-"))
					{
						signedValue.append("-");
					}
					// Consume integer, float, or identifier (for very large numbers)
					// Very large integers might be tokenized differently, so be lenient
					if (tokenizer.current().type == Tokenizer.TokenType.INTEGER ||
					    tokenizer.current().type == Tokenizer.TokenType.FLOAT ||
					    tokenizer.current().type == Tokenizer.TokenType.IDENTIFIER)
					{
						// Check if identifier looks like a number
						if (tokenizer.current().type == Tokenizer.TokenType.IDENTIFIER)
						{
							String text = tokenizer.current().text;
							// Accept if it's all digits (very large number)
							if (text.matches("\\d+"))
							{
								signedValue.append(text);
								tokenizer.next();
								return signedValue.toString();
							}
							recordError("Expected integer for field default value.");
							return null;
						}
						signedValue.append(tokenizer.current().text);
						tokenizer.next();
						return signedValue.toString();
					}
					recordError("Expected integer for field default value.");
					return null;
					
				case TYPE_UINT32:
				case TYPE_UINT64:
				case TYPE_FIXED32:
				case TYPE_FIXED64:
					// Match C++ behavior - unsigned types can't be negative
					if (tryConsume("-"))
					{
						recordError("Unsigned field can't have negative default value.");
					}
					// Consume integer, float, or identifier (for very large numbers)
					if (tokenizer.current().type == Tokenizer.TokenType.INTEGER ||
					    tokenizer.current().type == Tokenizer.TokenType.FLOAT ||
					    tokenizer.current().type == Tokenizer.TokenType.IDENTIFIER)
					{
						// Check if identifier looks like a number
						if (tokenizer.current().type == Tokenizer.TokenType.IDENTIFIER)
						{
							String text = tokenizer.current().text;
							// Accept if it's all digits (very large number)
							if (text.matches("\\d+"))
							{
								tokenizer.next();
								return text;
							}
							recordError("Expected integer for field default value.");
							return null;
						}
						String text = tokenizer.current().text;
						tokenizer.next();
						return text;
					}
					recordError("Expected integer for field default value.");
					return null;
					
				case TYPE_FLOAT:
				case TYPE_DOUBLE:
					// Handle negative sign
					boolean negative = tryConsume("-");
					StringBuilder value = new StringBuilder();
					if (negative)
					{
						value.append("-");
					}
					// Handle special values: inf, -inf, nan
					if (tokenizer.current().type == Tokenizer.TokenType.IDENTIFIER)
					{
						String identifier = tokenizer.current().text;
						if (identifier.equals("inf") || identifier.equals("nan"))
						{
							value.append(identifier);
							tokenizer.next();
							return value.toString();
						}
					}
					// Regular number - match C++ ConsumeNumber behavior
					// Accept both INTEGER and FLOAT tokens
					if (tokenizer.current().type == Tokenizer.TokenType.FLOAT || 
					    tokenizer.current().type == Tokenizer.TokenType.INTEGER)
					{
						value.append(tokenizer.current().text);
						tokenizer.next();
						
						// Handle scientific notation: e+38, e-45, E+38, etc.
						// The tokenizer doesn't handle scientific notation, so we need to
						// manually parse it: number e/E [+/-] exponent
						if (tokenizer.current().type == Tokenizer.TokenType.IDENTIFIER &&
						    (tokenizer.current().text.equals("e") || tokenizer.current().text.equals("E")))
						{
							value.append(tokenizer.current().text);
							tokenizer.next();
							
							// Optional + or -
							if (tokenizer.current().type == Tokenizer.TokenType.SYMBOL)
							{
								String symbol = tokenizer.current().text;
								if (symbol.equals("+") || symbol.equals("-"))
								{
									value.append(symbol);
									tokenizer.next();
								}
							}
							
							// Exponent (integer)
							if (tokenizer.current().type == Tokenizer.TokenType.INTEGER)
							{
								value.append(tokenizer.current().text);
								tokenizer.next();
							}
							else
							{
								recordError("Expected exponent after 'e' or 'E'.");
								return null;
							}
						}
						
						String rawValue = value.toString();
						try
						{
							if (type == FieldDescriptorProto.Type.TYPE_FLOAT)
							{
								float f = Float.parseFloat(rawValue);
								if (Float.isInfinite(f))
								{
									return f > 0 ? "inf" : "-inf";
								}
								return String.format(java.util.Locale.US, "%.9g", f);
							}
							else
							{
								double d = Double.parseDouble(rawValue);
								if (Double.isInfinite(d))
								{
									return d > 0 ? "inf" : "-inf";
								}
								return String.format(java.util.Locale.US, "%.17g", d);
							}
						}
						catch (NumberFormatException e)
						{
							return rawValue;
						}
					}
					recordError("Expected number for float/double default value.");
					return null;
					
				case TYPE_BOOL:
					if (tryConsume("true"))
					{
						return "true";
					}
					else if (tryConsume("false"))
					{
						return "false";
					}
					recordError("Expected \"true\" or \"false\".");
					return null;
					
				case TYPE_STRING:
				case TYPE_BYTES:
					if (tokenizer.current().type == Tokenizer.TokenType.STRING)
					{
						return consumeString("Expected string.");
					}
					recordError("Expected string for field default value.");
					return null;
					
				default:
					// For other numeric types (ENUM, etc.), consume integer, float, or identifier
					// Large integers might be tokenized as FLOAT, so check both
					if (tokenizer.current().type == Tokenizer.TokenType.INTEGER ||
					    tokenizer.current().type == Tokenizer.TokenType.FLOAT)
					{
						String text = tokenizer.current().text;
						tokenizer.next();
						return text;
					}
					else if (tokenizer.current().type == Tokenizer.TokenType.IDENTIFIER)
					{
						String text = tokenizer.current().text;
						tokenizer.next();
						return text;
					}
					recordError("Expected default value.");
					return null;
			}
		}
		else
		{
			// User-defined type (message or enum) - just consume identifier
			if (tokenizer.current().type == Tokenizer.TokenType.IDENTIFIER)
			{
				String text = tokenizer.current().text;
				tokenizer.next();
				return text;
			}
			recordError("Expected default value.");
			return null;
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

	private boolean parseType(FieldDescriptorProto.Builder fieldBuilder)
	{
		// Match C++ ParseType behavior - can be primitive or user-defined type
		String typeName = consumeIdentifier("Expected field type.");
		if (typeName.isEmpty())
		{
			return false; // Match C++ DO macro - return false on failure
		}
		
		FieldDescriptorProto.Type type = typeNameToType.get(typeName);
		if (type != null)
		{
			fieldBuilder.setType(type);
		}
		else
		{
			// User-defined type - may have dots (e.g., "com.example.Message")
			// Match C++ ParseUserDefinedType behavior
			StringBuilder fullTypeName = new StringBuilder(typeName);
			while (tryConsume("."))
			{
				fullTypeName.append(".");
				String part = consumeIdentifier("Expected identifier.");
				if (part.isEmpty())
				{
					return false; // Match C++ DO macro
				}
				fullTypeName.append(part);
			}
			fieldBuilder.setTypeName(fullTypeName.toString());
		}
		return true;
	}

	private boolean parseReserved(DescriptorProto.Builder messageBuilder, LocationRecorder location)
	{
		// Match C++ ParseReserved behavior
		if (!tryConsume("reserved"))
		{
			return false;
		}
		
		// Check if it's reserved names (strings) or reserved numbers
		if (tokenizer.current().type == Tokenizer.TokenType.STRING)
		{
			// Reserved names - match C++ ParseReservedNames
			do
			{
				String name = consumeString("Expected field name string literal.");
				if (name == null || name.isEmpty())
				{
					return false;
				}
				messageBuilder.addReservedName(name);
			}
			while (tryConsume(","));
			
			if (!tryConsume(";"))
			{
				recordError("Expected ';' after reserved names.");
				return false;
			}
			return true;
		}
		else if (tokenizer.current().type == Tokenizer.TokenType.IDENTIFIER)
		{
			// Reserved identifiers (for editions) - match C++ ParseReservedIdentifiers
			do
			{
				String identifier = consumeIdentifier("Expected field name identifier.");
				if (identifier.isEmpty())
				{
					return false;
				}
				messageBuilder.addReservedName(identifier);
			}
			while (tryConsume(","));
			
			if (!tryConsume(";"))
			{
				recordError("Expected ';' after reserved identifiers.");
				return false;
			}
			return true;
		}
		else
		{
			// Reserved numbers/ranges - match C++ ParseReservedNumbers
			boolean first = true;
			do
			{
				DescriptorProto.ReservedRange.Builder rangeBuilder = DescriptorProto.ReservedRange.newBuilder();
				
				// Parse start number
				if (tokenizer.current().type != Tokenizer.TokenType.INTEGER)
				{
					recordError(first ? "Expected field name or number range." : "Expected field number range.");
					return false;
				}
				int start = consumeInteger(first ? "Expected field name or number range." : "Expected field number range.");
				rangeBuilder.setStart(start);
				
				// Check for range (e.g., "10 to 20") or single number
				if (tryConsume("to"))
				{
					if (tryConsume("max"))
					{
						// Set to max value - use a sentinel that will be adjusted later
						rangeBuilder.setEnd(536870911); // kMaxRangeSentinel - 1
					}
					else
					{
						if (tokenizer.current().type != Tokenizer.TokenType.INTEGER)
						{
							recordError("Expected integer.");
							return false;
						}
						int end = consumeInteger("Expected integer.");
						rangeBuilder.setEnd(end);
					}
				}
				else
				{
					// Single number - range is just that number
					rangeBuilder.setEnd(start);
				}
				
				messageBuilder.addReservedRange(rangeBuilder);
				first = false;
			}
			while (tryConsume(","));
			
			if (!tryConsume(";"))
			{
				recordError("Expected ';' after reserved numbers.");
				return false;
			}
			return true;
		}
	}

	private boolean parseMapType(FieldDescriptorProto.Builder fieldBuilder, LocationRecorder location, MapFieldInfo mapFieldInfo)
	{
		// Match C++ ParseMapType behavior
		// Note: We check for oneof and labels before calling this method in parseField,
		// but we keep these checks here for safety and to match C++ behavior
		if (fieldBuilder.hasOneofIndex())
		{
			recordError("Map fields are not allowed in oneofs.");
			return false;
		}
		if (fieldBuilder.hasLabel())
		{
			recordError("Field labels (required/optional/repeated) are not allowed on map fields.");
			return false;
		}
		if (fieldBuilder.hasExtendee())
		{
			recordError("Map fields are not allowed to be extensions.");
			return false;
		}
		// Map fields are implicitly repeated
		fieldBuilder.setLabel(FieldDescriptorProto.Label.LABEL_REPEATED);
		
		if (!tryConsume("<"))
		{
			recordError("Expected '<' after map.");
			return false;
		}
		
		// Parse key type
		FieldDescriptorProto.Builder keyFieldBuilder = FieldDescriptorProto.newBuilder();
		if (!parseType(keyFieldBuilder))
		{
			return false;
		}
		mapFieldInfo.keyType = keyFieldBuilder.hasType() ? keyFieldBuilder.getType() : null;
		mapFieldInfo.keyTypeName = keyFieldBuilder.getTypeName();
		
		if (!tryConsume(","))
		{
			recordError("Expected ',' between map key and value types.");
			return false;
		}
		
		// Parse value type
		FieldDescriptorProto.Builder valueFieldBuilder = FieldDescriptorProto.newBuilder();
		if (!parseType(valueFieldBuilder))
		{
			return false;
		}
		mapFieldInfo.valueType = valueFieldBuilder.hasType() ? valueFieldBuilder.getType() : null;
		mapFieldInfo.valueTypeName = valueFieldBuilder.getTypeName();
		
		if (!tryConsume(">"))
		{
			recordError("Expected '>' to end map type.");
			return false;
		}
		
		// Don't set type_name yet - will be set when generating map entry after field name is known
		
		return true;
	}
	
	private String mapEntryName(String fieldName)
	{
		// Match C++ MapEntryName behavior
		StringBuilder result = new StringBuilder();
		boolean capNext = true;
		for (char c : fieldName.toCharArray())
		{
			if (c == '_')
			{
				capNext = true;
			}
			else if (capNext)
			{
				if (c >= 'a' && c <= 'z')
				{
					result.append((char)(c - 'a' + 'A'));
				}
				else
				{
					result.append(c);
				}
				capNext = false;
			}
			else
			{
				result.append(c);
			}
		}
		result.append("Entry");
		return result.toString();
	}
	
	private void generateMapEntry(FieldDescriptorProto.Builder fieldBuilder, MapFieldInfo mapFieldInfo, DescriptorProto.Builder messageBuilder)
	{
		// Match C++ GenerateMapEntry behavior
		String entryName = mapEntryName(fieldBuilder.getName());

		StringBuilder fullName = new StringBuilder();
		if (!packageName.isEmpty())
		{
			fullName.append(".").append(packageName);
		}
		for (String parent : messageStack)
		{
			fullName.append(".").append(parent);
		}
		fullName.append(".").append(entryName);

		fieldBuilder.setTypeName(fullName.toString());
		fieldBuilder.setType(FieldDescriptorProto.Type.TYPE_MESSAGE);
		
		// Create the map entry message
		DescriptorProto.Builder entryBuilder = messageBuilder.addNestedTypeBuilder();
		entryBuilder.setName(entryName);
		entryBuilder.getOptionsBuilder().setMapEntry(true);
		
		// Create key field
		FieldDescriptorProto.Builder keyFieldBuilder = entryBuilder.addFieldBuilder();
		keyFieldBuilder.setName("key");
		keyFieldBuilder.setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL);
		keyFieldBuilder.setNumber(1);
		if (mapFieldInfo.keyTypeName != null && !mapFieldInfo.keyTypeName.isEmpty())
		{
			keyFieldBuilder.setTypeName(mapFieldInfo.keyTypeName);
		}
		else if (mapFieldInfo.keyType != null)
		{
			keyFieldBuilder.setType(mapFieldInfo.keyType);
		}
		
		// Create value field
		FieldDescriptorProto.Builder valueFieldBuilder = entryBuilder.addFieldBuilder();
		valueFieldBuilder.setName("value");
		valueFieldBuilder.setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL);
		valueFieldBuilder.setNumber(2);
		if (mapFieldInfo.valueTypeName != null && !mapFieldInfo.valueTypeName.isEmpty())
		{
			valueFieldBuilder.setTypeName(mapFieldInfo.valueTypeName);
		}
		else if (mapFieldInfo.valueType != null)
		{
			valueFieldBuilder.setType(mapFieldInfo.valueType);
		}
	}

	private boolean parseEnumDefinition(EnumDescriptorProto.Builder enumBuilder,
			LocationRecorder location)
	{
		tokenizer.next(); // consume "enum"
		enumBuilder.setName(consumeIdentifier("Expected enum name."));
		boolean result = parseEnumBlock(enumBuilder, location.path);
		location.end();
		return result;
	}

	private boolean parseEnumBlock(EnumDescriptorProto.Builder enumBuilder, List<Integer> scopePath)
	{
		consume("{", "Expected '{' to start enum block.");
		while (!tryConsume("}"))
		{
			if (tokenizer.current().type == Tokenizer.TokenType.END)
			{
				recordError("Reached end of input in enum definition (missing '}').");
				return false;
			}
			if (!parseEnumStatement(enumBuilder, scopePath))
			{
				skipStatement();
			}
		}
		return true;
	}

	private boolean parseEnumStatement(EnumDescriptorProto.Builder enumBuilder, List<Integer> scopePath)
	{
		if (tryConsume(";"))
		{
			// empty statement
			return true;
		}
		else if (lookingAt("option"))
		{
			// Parse enum option - match C++ ParseOption behavior
			tokenizer.next(); // consume "option"
			String optionName = consumeIdentifier("Expected option name.");
			if (optionName.isEmpty())
			{
				return false;
			}
			
			// Handle allow_alias option
			if (optionName.equals("allow_alias"))
			{
				consume("=", "Expected '=' after option name.");
				boolean value = consumeBoolean("Expected 'true' or 'false'.");
				enumBuilder.getOptionsBuilder().setAllowAlias(value);
				consume(";", "Expected ';' after option declaration.");
				return true;
			}
			
			// Generic option handling
			com.google.protobuf.DescriptorProtos.EnumOptions.Builder optionsBuilder = enumBuilder.getOptionsBuilder();
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
			return true;
		}
		else if (lookingAt("reserved"))
		{
			// Parse reserved statement for enum - match C++ ParseReserved for enum
			tokenizer.next(); // consume "reserved"
			
			if (tokenizer.current().type == Tokenizer.TokenType.STRING)
			{
				// Reserved names (string literals)
				do
				{
					String name = consumeString("Expected enum value name string literal.");
					if (name.isEmpty()) return false;
					enumBuilder.addReservedName(name);
				} while (tryConsume(","));
			}
			else
			{
				// Reserved numbers
				do
				{
					EnumDescriptorProto.EnumReservedRange.Builder range = enumBuilder.addReservedRangeBuilder();
					
					if (tokenizer.current().type != Tokenizer.TokenType.INTEGER)
					{
						recordError("Expected enum value number.");
						return false;
					}
					int start = consumeInteger("Expected enum value number.");
					range.setStart(start);
					
					if (tryConsume("to"))
					{
						if (lookingAt("max"))
						{
							tokenizer.next(); // consume "max"
							range.setEnd(Integer.MAX_VALUE);
						}
						else if (tokenizer.current().type == Tokenizer.TokenType.INTEGER)
						{
							int end = consumeInteger("Expected integer.");
							range.setEnd(end);
						}
						else
						{
							recordError("Expected integer or 'max'.");
							return false;
						}
					}
					else
					{
						range.setEnd(start);
					}
				} while (tryConsume(","));
			}
			
			consume(";", "Expected ';' after reserved statement.");
			return true;
		}
		else
		{
			// Parse enum constant
			LocationRecorder location = new LocationRecorder(this, scopePath);
			location.addPath(EnumDescriptorProto.VALUE_FIELD_NUMBER);
			location.addPath(enumBuilder.getValueCount());
			boolean result = parseEnumConstant(enumBuilder.addValueBuilder(), location);
			if (result)
			{
				location.end();
			}
			return result;
		}
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
			if (lookingAt("deprecated"))
			{
				tokenizer.next();
				consume("=", "Expected '=' after deprecated.");
				boolean value = consumeBoolean("Expected boolean.");
				enumValueBuilder.getOptionsBuilder().setDeprecated(value);
			}
			else
			{
				String optionName = consumeIdentifier("Expected option name.");
				UninterpretedOption.Builder option = UninterpretedOption.newBuilder();
				option.addNameBuilder().setNamePart(optionName).setIsExtension(false);
				consume("=", "Expected '=' after option name.");
				parseOptionValue(option);
				enumValueBuilder.getOptionsBuilder().addUninterpretedOption(option);
			}
		}
		while (tryConsume(","));
	}

	private boolean parseServiceDefinition(ServiceDescriptorProto.Builder serviceBuilder,
			LocationRecorder location)
	{
		tokenizer.next(); // consume "service"
		serviceBuilder.setName(consumeIdentifier("Expected service name."));
		boolean result = parseServiceBlock(serviceBuilder, location.path);
		location.end();
		return result;
	}

	private boolean parseServiceBlock(ServiceDescriptorProto.Builder serviceBuilder,
			List<Integer> scopePath)
	{
		consume("{", "Expected '{' to start service block.");
		while (!tryConsume("}"))
		{
			if (tokenizer.current().type == Tokenizer.TokenType.END)
			{
				recordError("Reached end of input in service definition (missing '}').");
				return false;
			}
			if (!parseServiceStatement(serviceBuilder, scopePath))
			{
				skipStatement();
			}
		}
		return true;
	}

	private boolean parseServiceStatement(ServiceDescriptorProto.Builder serviceBuilder, List<Integer> scopePath)
	{
		if (tryConsume(";"))
		{
			// empty statement
			return true;
		}
		else if (lookingAt("option"))
		{
			// Parse service option - match C++ ParseOption behavior
			tokenizer.next(); // consume "option"
			String optionName = consumeIdentifier("Expected option name.");
			if (optionName.isEmpty())
			{
				return false;
			}
			
			com.google.protobuf.DescriptorProtos.ServiceOptions.Builder optionsBuilder = serviceBuilder.getOptionsBuilder();
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
			return true;
		}
		else
		{
			LocationRecorder location = new LocationRecorder(this, scopePath);
			location.addPath(ServiceDescriptorProto.METHOD_FIELD_NUMBER);
			location.addPath(serviceBuilder.getMethodCount());
			boolean result = parseServiceMethod(serviceBuilder.addMethodBuilder(), location);
			if (result)
			{
				location.end();
			}
			return result;
		}
	}

	private boolean parseServiceMethod(MethodDescriptorProto.Builder methodBuilder,
			LocationRecorder location)
	{
		if (!tryConsume("rpc"))
		{
			return false;
		}
		
		String methodName = consumeIdentifier("Expected method name.");
		if (methodName.isEmpty())
		{
			return false;
		}
		methodBuilder.setName(methodName);
		
		// Parse input type - match C++ ParseServiceMethod behavior
		if (!tryConsume("("))
		{
			recordError("Expected '(' to start method request type.");
			return false;
		}
		
		// Check for stream keyword
		if (lookingAt("stream"))
		{
			methodBuilder.setClientStreaming(true);
			tokenizer.next(); // consume "stream"
		}
		
		// Parse user-defined type (can be qualified with dots)
		StringBuilder inputType = new StringBuilder();
		inputType.append(consumeIdentifier("Expected method request type."));
		if (inputType.length() == 0)
		{
			return false;
		}
		while (tryConsume("."))
		{
			inputType.append(".");
			inputType.append(consumeIdentifier("Expected type component."));
		}
		methodBuilder.setInputType(inputType.toString());
		
		if (!tryConsume(")"))
		{
			recordError("Expected ')' to end method request type.");
			return false;
		}
		
		// Parse returns keyword
		if (!tryConsume("returns"))
		{
			recordError("Expected 'returns' after method request type.");
			return false;
		}
		
		// Parse output type
		if (!tryConsume("("))
		{
			recordError("Expected '(' to start method response type.");
			return false;
		}
		
		// Check for stream keyword
		if (lookingAt("stream"))
		{
			methodBuilder.setServerStreaming(true);
			tokenizer.next(); // consume "stream"
		}
		
		// Parse user-defined type (can be qualified with dots)
		StringBuilder outputType = new StringBuilder();
		outputType.append(consumeIdentifier("Expected method response type."));
		if (outputType.length() == 0)
		{
			return false;
		}
		while (tryConsume("."))
		{
			outputType.append(".");
			outputType.append(consumeIdentifier("Expected type component."));
		}
		methodBuilder.setOutputType(outputType.toString());
		
		if (!tryConsume(")"))
		{
			recordError("Expected ')' to end method response type.");
			return false;
		}
		
		// Parse method options or semicolon
		if (tryConsume("{"))
		{
			// parseMethodOptions will consume the closing "}"
			parseMethodOptions(methodBuilder, location);
		}
		else
		{
			if (!tryConsume(";"))
			{
				recordError("Expected ';' or '{' after method definition.");
				return false;
			}
		}
		return true;
	}

	private boolean parseExtensions(DescriptorProto.Builder messageBuilder,
			LocationRecorder location)
	{
		// Match C++ ParseExtensions behavior
		if (!tryConsume("extensions"))
		{
			return false;
		}
		
		int oldRangeSize = messageBuilder.getExtensionRangeCount();
		
		do
		{
			DescriptorProto.ExtensionRange.Builder range = messageBuilder.addExtensionRangeBuilder();
			
			// Parse start number
			if (tokenizer.current().type != Tokenizer.TokenType.INTEGER)
			{
				recordError("Expected field number range.");
				return false;
			}
			int start = consumeInteger("Expected field number range.");
			if (start == Integer.MAX_VALUE)
			{
				recordError("Field number out of bounds.");
				return false;
			}
			
			int end;
			if (tryConsume("to"))
			{
				if (lookingAt("max"))
				{
					tokenizer.next(); // consume "max"
					// Use sentinel value - 1, will be adjusted later
					end = Integer.MAX_VALUE - 1;
				}
				else
				{
					if (tokenizer.current().type != Tokenizer.TokenType.INTEGER)
					{
						recordError("Expected integer.");
						return false;
					}
					end = consumeInteger("Expected integer.");
					if (end == Integer.MAX_VALUE)
					{
						recordError("Field number out of bounds.");
						return false;
					}
				}
			}
			else
			{
				end = start;
			}
			
			// Users specify inclusive ranges, but we store exclusive end
			end++;
			
			range.setStart(start);
			range.setEnd(end);
		} while (tryConsume(","));
		
		// Parse extension range options if present
		if (tryConsume("["))
		{
			// Parse options for the first range
			com.google.protobuf.DescriptorProtos.ExtensionRangeOptions.Builder options = 
				messageBuilder.getExtensionRangeBuilder(oldRangeSize).getOptionsBuilder();
			
			do
			{
				String optionName = consumeIdentifier("Expected option name.");
				if (optionName.isEmpty())
				{
					return false;
				}
				UninterpretedOption.Builder option = UninterpretedOption.newBuilder();
				option.addNameBuilder().setNamePart(optionName).setIsExtension(false);
				while (tryConsume("."))
				{
					option.addNameBuilder().setNamePart(consumeIdentifier("Expected option name component."))
							.setIsExtension(false);
				}
				consume("=", "Expected '=' after option name.");
				parseOptionValue(option);
				options.addUninterpretedOption(option);
			} while (tryConsume(","));
			
			consume("]", "Expected ']' to end extension range options.");
			
			// Copy options to all other ranges
			com.google.protobuf.DescriptorProtos.ExtensionRangeOptions firstOptions = options.build();
			for (int i = oldRangeSize + 1; i < messageBuilder.getExtensionRangeCount(); i++)
			{
				messageBuilder.getExtensionRangeBuilder(i).setOptions(firstOptions);
			}
		}
		
		consume(";", "Expected ';' after extensions declaration.");
		return true;
	}

	private boolean parseTopLevelExtend(FileDescriptorProto.Builder fileBuilder,
			LocationRecorder location)
	{
		// Match C++ ParseExtend behavior for top-level extend statements
		if (!tryConsume("extend"))
		{
			return false;
		}
		
		// Parse the extendee type (can be qualified with dots)
		StringBuilder extendee = new StringBuilder();
		extendee.append(consumeIdentifier("Expected extendee type name."));
		if (extendee.length() == 0)
		{
			return false;
		}
		while (tryConsume("."))
		{
			extendee.append(".");
			extendee.append(consumeIdentifier("Expected type component."));
		}
		
		// Parse the extend block
		if (!tryConsume("{"))
		{
			recordError("Expected '{' to start extend block.");
			return false;
		}
		
		while (!tryConsume("}"))
		{
			if (tokenizer.current().type == Tokenizer.TokenType.END)
			{
				recordError("Reached end of input in extend definition (missing '}').");
				return false;
			}
			
			// Parse extension field
			LocationRecorder fieldLocation = new LocationRecorder(this);
			FieldDescriptorProto.Builder fieldBuilder = fileBuilder.addExtensionBuilder();
			fieldLocation.addPath(FileDescriptorProto.EXTENSION_FIELD_NUMBER);
			fieldLocation.addPath(fileBuilder.getExtensionCount() - 1);

			fieldBuilder.setExtendee(extendee.toString());
			
			// Parse label (optional/repeated)
			parseLabel(fieldBuilder);
			
			// Parse type
			if (!parseType(fieldBuilder))
			{
				return false;
			}
			
			// Parse field name
			String fieldName = consumeIdentifier("Expected field name.");
			if (fieldName.isEmpty())
			{
				return false;
			}
			fieldBuilder.setName(fieldName);
			
			// Parse '=' and field number
			if (!tryConsume("="))
			{
				recordError("Missing field number.");
				return false;
			}
			
			if (tokenizer.current().type != Tokenizer.TokenType.INTEGER)
			{
				recordError("Expected field number.");
				return false;
			}
			int fieldNumber = consumeInteger("Expected field number.");
			fieldBuilder.setNumber(fieldNumber);
			
			// Parse options
			if (tryConsume("["))
			{
				if (!parseFieldOptions(fieldBuilder, fieldLocation))
				{
					return false;
				}
				if (!tryConsume("]"))
				{
					recordError("Expected ']' to end field options.");
					return false;
				}
			}
			
			// Extension fields end with semicolon
			if (!tryConsume(";"))
			{
				recordError("Expected ';' after extension field declaration.");
				return false;
			}
			fieldLocation.end();
		}
		
		return true;
	}

	private boolean parseExtend(DescriptorProto.Builder messageBuilder,
			List<Integer> scopePath)
	{
		// Match C++ ParseExtend behavior - extend statements inside messages
		if (!tryConsume("extend"))
		{
			return false;
		}
		
		// Parse the extendee type (can be qualified with dots)
		StringBuilder extendee = new StringBuilder();
		extendee.append(consumeIdentifier("Expected extendee type name."));
		if (extendee.length() == 0)
		{
			return false;
		}
		while (tryConsume("."))
		{
			extendee.append(".");
			extendee.append(consumeIdentifier("Expected type component."));
		}
		
		// Parse the extend block
		if (!tryConsume("{"))
		{
			recordError("Expected '{' to start extend block.");
			return false;
		}
		
		while (!tryConsume("}"))
		{
			if (tokenizer.current().type == Tokenizer.TokenType.END)
			{
				recordError("Reached end of input in extend definition (missing '}').");
				return false;
			}
			
			// Parse extension field
			LocationRecorder fieldLocation = new LocationRecorder(this, scopePath);
			FieldDescriptorProto.Builder fieldBuilder = messageBuilder.addExtensionBuilder();
			fieldLocation.addPath(DescriptorProto.EXTENSION_FIELD_NUMBER);
			fieldLocation.addPath(messageBuilder.getExtensionCount() - 1);

			fieldBuilder.setExtendee(extendee.toString());
			
			// Parse label (optional/repeated)
			parseLabel(fieldBuilder);
			
			// Parse type
			if (!parseType(fieldBuilder))
			{
				return false;
			}
			
			// Parse field name
			String fieldName = consumeIdentifier("Expected field name.");
			if (fieldName.isEmpty())
			{
				return false;
			}
			fieldBuilder.setName(fieldName);
			
			// Parse '=' and field number
			if (!tryConsume("="))
			{
				recordError("Missing field number.");
				return false;
			}
			
			if (tokenizer.current().type != Tokenizer.TokenType.INTEGER)
			{
				recordError("Expected field number.");
				return false;
			}
			int fieldNumber = consumeInteger("Expected field number.");
			fieldBuilder.setNumber(fieldNumber);
			
			// Parse options
			if (tryConsume("["))
			{
				if (!parseFieldOptions(fieldBuilder, fieldLocation))
				{
					return false;
				}
				if (!tryConsume("]"))
				{
					recordError("Expected ']' to end field options.");
					return false;
				}
			}
			
			// Extension fields end with semicolon
			if (!tryConsume(";"))
			{
				recordError("Expected ';' after extension field declaration.");
				return false;
			}
			fieldLocation.end();
		}
		
		return true;
	}

	private void parseMethodOptions(MethodDescriptorProto.Builder methodBuilder,
			LocationRecorder location)
	{
		// Match C++ ParseMethodOptions behavior
		while (!tryConsume("}"))
		{
			if (tokenizer.current().type == Tokenizer.TokenType.END)
			{
				recordError("Reached end of input in method options (missing '}').");
				return;
			}
			
			if (tryConsume(";"))
			{
				// empty statement; ignore
				continue;
			}
			
			// Parse option using the same pattern as other option parsing
			if (!lookingAt("option"))
			{
				// Not an option, skip this statement
				skipStatement();
				continue;
			}
			
			tokenizer.next(); // consume "option"
			String optionName = consumeIdentifier("Expected option name.");
			if (optionName.isEmpty())
			{
				skipStatement();
				continue;
			}
			
			UninterpretedOption.Builder option = UninterpretedOption.newBuilder();
			option.addNameBuilder().setNamePart(optionName).setIsExtension(false);
			while (tryConsume("."))
			{
				option.addNameBuilder().setNamePart(consumeIdentifier("Expected option name component."))
						.setIsExtension(false);
			}
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
		return text.equals(tokenizer.current().text);
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
			// Don't advance tokenizer - match C++ behavior where ConsumeIdentifier returns false
			// and the calling function returns false immediately via DO macro
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
		private String leadingComments;

		LocationRecorder(Parser parser)
		{
			this(parser, null);
		}

		LocationRecorder(Parser parser, List<Integer> parentPath)
		{
			this.parser = parser;
			this.startLine = parser.tokenizer.current().line;
			this.startColumn = parser.tokenizer.current().column;
			this.leadingComments = parser.tokenizer.current().leadingComments;
			if (parentPath != null)
			{
				this.path.addAll(parentPath);
			}
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
			String trailingComments = parser.tokenizer.previous().trailingComments;
			parser.sourceLocationTable.add(pathArray, spanArray, leadingComments, trailingComments);
		}
	}
}
