package com.rubberjam.protobuf.compiler.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.JavaFeaturesProto;
import com.google.protobuf.Syntax;
import com.rubberjam.protobuf.io.Printer;

/**
 * Internal helpers for Java code generation. Ported from internal_helpers.h.
 */
public final class InternalHelpers
{

	// Max number of constants in a generated Java class.
	private static final int MAX_ENUMS = 1000;
	
	private InternalHelpers()
	{
	}

	public static boolean supportUnknownEnumValue(FieldDescriptor field)
	{
		if (field.getFile() != null && field.getFile().toProto().getSyntax().equals("proto3"))
		{
			return true;
		}
		return !field.getOptions().getFeatures().getExtension(JavaFeaturesProto.java_).getLegacyClosedEnum();
	}

	public static boolean checkUtf8(FieldDescriptor descriptor)
	{
		if (descriptor.getType() != FieldDescriptor.Type.STRING)
		{
			return false;
		}
		if (descriptor.getFile() != null && descriptor.getFile().toProto().getSyntax().equals("proto3"))
		{
			return true;
		}
		return descriptor.getOptions().getFeatures().getExtension(JavaFeaturesProto.java_)
				.getUtf8Validation() == JavaFeaturesProto.JavaFeatures.Utf8Validation.VERIFY;
	}

	public static boolean checkLargeEnum(EnumDescriptor descriptor)
	{
		return descriptor.getOptions().getFeatures().getExtension(JavaFeaturesProto.java_).getLargeEnum();
	}



	public static void generateLarge(Printer printer, EnumDescriptor descriptor, boolean immutable, Context context,
			ClassNameResolver nameResolver)
	{

		int interfaceCount = (int) Math.ceil((double) descriptor.getValues().size() / MAX_ENUMS);

		// A map of all aliased values to the canonical value.
		Map<EnumValueDescriptor, EnumValueDescriptor> aliases = new HashMap<>();

		int canonicalValuesCounter = 0;
		for (EnumValueDescriptor value : descriptor.getValues())
		{
			EnumValueDescriptor canonicalValue = descriptor.findValueByNumber(value.getNumber());
			if (value == canonicalValue)
			{
				canonicalValuesCounter++;
			}
			else
			{
				aliases.put(value, canonicalValue);
			}
		}

		int numCanonicalValues = canonicalValuesCounter;
		
		Map<String, Object> vars = new HashMap<>();
		vars.put("classname", descriptor.getName());
		// IsOwnFile logic: generally !nestedInFileClass.
		// Assuming ClassNameResolver or Helpers handles this logic check.
		// Here we approximate based on the C++ context:
		
		//TODO - implement Helpers.nestedInFileClass(descriptor, immutable); and uncomment this
		boolean isOwnFile = false;//!Helpers.nestedInFileClass(descriptor, immutable);
		vars.put("static", isOwnFile ? " " : " static ");
		vars.put("deprecation", descriptor.getOptions().getDeprecated() ? "@java.lang.Deprecated" : "");
		vars.put("unrecognized_index", descriptor.getValues().size());
		vars.put("proto_enum_class", context.enforceLite()
				? "com.google.protobuf.Internal.EnumLite"
				: "com.google.protobuf.ProtocolMessageEnum");

		// Lambdas for dynamic generation
		vars.put("proto_non_null_annotation", new Printer.PrinterValue(() ->
		{
			// Assuming generic open source generation where we might not use
			// internal annotations
			// or we can implement a check similar to IsOss().
			// For this port, we'll skip the internal-only annotations or assume
			// OSS.
			return true;
		}));

		vars.put("method_return_null_annotation", new Printer.PrinterValue(() ->
		{
			// Similar to above
			return true;
		}));

		vars.put("interface_names", new Printer.PrinterValue(() ->
		{
			List<String> interfaceNames = new ArrayList<>();
			for (int count = 0; count < interfaceCount; count++)
			{
				interfaceNames.add(String.format("%s%d", descriptor.getName(), count));
			}
			printer.emit(Map.of("interface_names", String.join(", ", interfaceNames)), "$interface_names$");
			return true;
		}));

		vars.put("gen_code_version_validator", new Printer.PrinterValue(() ->
		{
			if (!context.enforceLite())
			{
				// Helpers.printGencodeVersionValidator(printer, true,
				// descriptor.getName());
				// For simplicity in this snippet, we can omit or comment out if
				// the helper isn't fully ready.
			}
			return true;
		}));

		vars.put("get_number_func", new Printer.PrinterValue(() ->
		{
			if (!descriptor.getFile().toProto().getSyntax().equals("proto3"))
			{
				// "is_closed" approximation: proto2 enums are closed, proto3
				// are open.
				// Actually C++ is_closed() logic is slightly more complex, but
				// usually refers to proto2 semantics
				// checking if the value is known.
				printer.emit("""
						if (this == UNRECOGNIZED) {
						  throw new java.lang.IllegalArgumentException(
						    "Can't get the number of an unknown enum value.");
						}
						""");
			}
			printer.emit("return value;");
			return true;
		}));

		vars.put("deprecated_value_of_func", new Printer.PrinterValue(() ->
		{
			// Assuming OSS behavior
			printer.emit("""
					/**
					 * @param value The numeric wire value of the corresponding enum entry.
					 * @return The enum associated with the given numeric wire value.
					 * @deprecated Use {@link #forNumber(int)} instead.
					 */
					@java.lang.Deprecated
					public static $classname$ valueOf(int value) {
					  return forNumber(value);
					}
					""");
			return true;
		}));

		vars.put("for_number_func", new Printer.PrinterValue(() ->
		{
			printer.emit("$classname$ found = null;");
			for (int count = 0; count < interfaceCount; count++)
			{
				printer.emit(Map.of("count", count), """
						found = $classname$$count$.forNumber$count$(value);
						if (found != null) {
						  return found;
						}
						""");
			}
			printer.emit("return null;");
			return true;
		}));

		vars.put("value_of_func", new Printer.PrinterValue(() ->
		{
			printer.emit("$classname$ found = null;");
			for (int count = 0; count < interfaceCount; count++)
			{
				printer.emit(Map.of("count", count), """
						found = $classname$$count$.valueOf$count$(name);
						if (found != null) {
						  return found;
						}
						""");
			}
			printer.emit("""
					throw new java.lang.IllegalArgumentException(
					  "No enum constant $classname$." + name);
					""");
			return true;
		}));

		vars.put("canonical_values_func", new Printer.PrinterValue(() ->
		{
			printer.emit(Map.of("values_size", numCanonicalValues + 1), """
					int ordinal = 0;
					$classname$[] values = new $classname$[$values_size$];
					""");

			for (int count = 0; count < interfaceCount; count++)
			{
				printer.emit(Map.of("count", count), """
						$classname$[] values$count$ = $classname$$count$.values$count$();
						System.arraycopy(values$count$, 0, values, ordinal, values$count$.length);
						ordinal += values$count$.length;
						""");
			}
			printer.emit(Map.of("unrecognized_index", numCanonicalValues), """
					values[$unrecognized_index$] = UNRECOGNIZED;
					return values;
					""");
			return true;
		}));

		vars.put("enum_verifier_func", new Printer.PrinterValue(() ->
		{
			if (context.enforceLite())
			{
				printer.emit("""
						public static com.google.protobuf.Internal.EnumVerifier
						    internalGetVerifier() {
						  return $classname$Verifier.INSTANCE;
						}

						private static final class $classname$Verifier implements
						     com.google.protobuf.Internal.EnumVerifier {
						        static final com.google.protobuf.Internal.EnumVerifier
						          INSTANCE = new $classname$Verifier();
						        @java.lang.Override
						        public boolean isInRange(int number) {
						          return $classname$.forNumber(number) != null;
						        }
						      };
						""");
			}
			return true;
		}));

		vars.put("descriptor_methods", new Printer.PrinterValue(() ->
		{
			if (!context.enforceLite())
			{ // HasDescriptorMethods check
				printer.emit("""
						public final com.google.protobuf.Descriptors.EnumValueDescriptor
						    getValueDescriptor() {
						""");
				if (!descriptor.getFile().toProto().getSyntax().equals("proto3"))
				{
					printer.emit("""
							if (this == UNRECOGNIZED) {
							  throw new java.lang.IllegalStateException(
							      "Can't get the descriptor of an unrecognized enum value.");
							}
							""");
				}
				printer.emit("""
						  return getDescriptor().getValue(index());
						}
						public final com.google.protobuf.Descriptors.EnumDescriptor
						    getDescriptorForType() {
						  return getDescriptor();
						}
						public static final com.google.protobuf.Descriptors.EnumDescriptor
						    getDescriptor() {
						""");

				if (descriptor.getContainingType() == null)
				{
					printer.emit(
							Map.of(
									"file", nameResolver.getClassName(descriptor.getFile(), immutable),
									"index", descriptor.getIndex()),
							"  return $file$.getDescriptor().getEnumType($index$);\n");
				}
				else
				{
					printer.emit(
							Map.of(
									"parent", nameResolver.getClassName(descriptor.getContainingType(), immutable),
									"descriptor", "getDescriptor()", // Simplified
																		// from
																		// C++
																		// logic
									"index", descriptor.getIndex()),
							"  return $parent$.$descriptor$.getEnumType($index$);\n");
				}
				printer.emit("}\n\n");

				printer.emit(
						Map.of("classname", descriptor.getName()),
						"""
								public static $classname$ valueOf(
								    com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
								  if (desc.getType() != getDescriptor()) {
								    throw new java.lang.IllegalArgumentException(
								      "EnumValueDescriptor is not for this type.");
								  }
								  $classname$ found = $classname$.forNumber(desc.getNumber());
								  if (found != null) {
								    return found;
								  }
								""");

				if (!descriptor.getFile().toProto().getSyntax().equals("proto3"))
				{
					printer.emit("  return UNRECOGNIZED;\n");
				}
				else
				{
					printer.emit("  throw new java.lang.IllegalArgumentException(\n" +
							"      \"EnumValueDescriptor has an invalid number.\");\n");
				}
				printer.emit("}\n");
			}
			return true;
		}));

		// Main Class Emission
		printer.emit(vars, """
				$proto_non_null_annotation$
				$deprecation$
				public$static$final class $classname$
				  implements $proto_enum_class$, java.io.Serializable, $interface_names$ {
				  static {
				    $gen_code_version_validator$
				  }

				  public static final $classname$ UNRECOGNIZED = new $classname$(-1, $unrecognized_index$, "UNRECOGNIZED");

				  $deprecated_value_of_func$

				  public final int getNumber() {
				    $get_number_func$
				  }

				  /**
				   * @param value The numeric wire value of the corresponding enum entry.
				   * @return The enum associated with the given numeric wire value.
				   */
				  $method_return_null_annotation$
				  public static $classname$ forNumber(int value) {
				    $for_number_func$
				  }

				  /**
				   * @param name The string name of the corresponding enum entry.
				   * @return The enum associated with the given string name.
				   */
				  public static $classname$ valueOf(String name) {
				    $value_of_func$
				  }

				  public static $classname$[] values() {
				    //~ In non-large enums, values() is the automatic one and only
				    //~ returns canonicals, so we match that here.
				    $canonical_values_func$
				  }

				  private final int value;
				  private final String name;
				  private final int index;

				  $classname$(int v, int i, String n) {
				    this.value = v;
				    this.index = i;
				    this.name = n;
				  }

				  public int index() {
				    return index;
				  }

				  public int value() {
				    return value;
				  }

				  public String name() {
				    return name;
				  }

				  // For Kotlin code.
				  public String getName() {
				    return name;
				  }

				  @java.lang.Override
				  public String toString() {
				    return name;
				  }

				  public static com.google.protobuf.Internal.EnumLiteMap<$classname$> internalGetValueMap() {
				    return internalValueMap;
				  }

				  private static final com.google.protobuf.Internal.EnumLiteMap<
				    $classname$> internalValueMap =
				      new com.google.protobuf.Internal.EnumLiteMap<$classname$>() {
				        public $classname$ findValueByNumber(int number) {
				          return $classname$.forNumber(number);
				        }
				      };

				  $enum_verifier_func$

				  $descriptor_methods$
				}
				""");

		// Interface Generation loop
		for (int count = 0; count < interfaceCount; count++)
		{
			int start = count * MAX_ENUMS;
			int end = Math.min(start + MAX_ENUMS, descriptor.getValues().size());

			final int currentCount = count; // for capture

			Map<String, Object> interfaceVars = new HashMap<>();
			interfaceVars.put("classname", descriptor.getName());
			interfaceVars.put("count", count);
			interfaceVars.put("method_return_null_annotation", vars.get("method_return_null_annotation"));

			interfaceVars.put("enums", new Printer.PrinterValue(() ->
			{
				for (int i = start; i < end; i++)
				{
					EnumValueDescriptor value = descriptor.getValues().get(i);
					DocComment.writeEnumValueDocComment(printer, value, context);
					String deprecation = value.getOptions().getDeprecated() ? "@java.lang.Deprecated " : "";

					if (aliases.containsKey(value))
					{
						EnumValueDescriptor canonical = aliases.get(value);
						int canonicalInterfaceIndex = canonical.getIndex() / MAX_ENUMS;
						printer.emit(
								Map.of(
										"name", value.getName(),
										"canonical_name", canonical.getName(),
										"canonical_interface_index", canonicalInterfaceIndex,
										"deprecation", deprecation),
								"""
										$deprecation$
										public static final $classname$ $name$ = $classname$$canonical_interface_index$.$canonical_name$;
										""");
					}
					else
					{
						printer.emit(
								Map.of(
										"name", value.getName(),
										"number", value.getNumber(),
										"index", value.getIndex(),
										"deprecation", deprecation),
								"""
										$deprecation$
										public static final $classname$ $name$ = new $classname$($number$, $index$, "$name$");
										""");
					}
					printer.emit(
							Map.of(
									"name", value.getName(),
									"number", value.getNumber(),
									"deprecation", deprecation),
							"""
									$deprecation$
									public static final int $name$_VALUE = $number$;
									""");
				}
				return true;
			}));

			interfaceVars.put("value_of_func", new Printer.PrinterValue(() ->
			{
				printer.emit("switch (name) {");
				for (int i = start; i < end; i++)
				{
					EnumValueDescriptor value = descriptor.getValues().get(i);
					if (aliases.containsKey(value)) continue;
					printer.emit(Map.of("name", value.getName()), "  case \"$name$\": return $name$;");
				}
				printer.emit("  default: return null;");
				printer.emit("}");
				return true;
			}));

			interfaceVars.put("for_number_func", new Printer.PrinterValue(() ->
			{
				printer.emit("switch (value) {");
				for (int i = start; i < end; i++)
				{
					EnumValueDescriptor value = descriptor.getValues().get(i);
					if (aliases.containsKey(value)) continue;
					printer.emit(
							Map.of("name", value.getName(), "number", value.getNumber()),
							"  case $number$: return $name$;");
				}
				printer.emit("  default: return null;");
				printer.emit("}");
				return true;
			}));

			interfaceVars.put("canonical_values_func", new Printer.PrinterValue(() ->
			{
				List<String> values = new ArrayList<>();
				for (int i = start; i < end; i++)
				{
					EnumValueDescriptor value = descriptor.getValues().get(i);
					if (aliases.containsKey(value)) continue;
					values.add(value.getName());
				}
				printer.emit("return new $classname$[] {");
				printer.emit(String.join(", ", values));
				printer.emit("};");
				return true;
			}));

			printer.emit(interfaceVars, """
					interface $classname$$count$ {

					  $enums$

					  /**
					   * @param value The numeric wire value of the corresponding enum entry.
					   * @return The enum associated with the given numeric wire value.
					   */
					  $method_return_null_annotation$
					  public static $classname$ forNumber$count$(int value) {
					    $for_number_func$
					  }

					  /**
					   * @param name The string name of the corresponding enum entry.
					   * @return The enum associated with the given string name.
					   */
					  $method_return_null_annotation$
					  public static $classname$ valueOf$count$(String name) {
					    $value_of_func$
					  }

					  public static $classname$[] values$count$() {
					    $canonical_values_func$
					  }
					}
					""");
		}
	}

	// Only the lowest two bytes of the return value are used.
	public static int getExperimentalJavaFieldType(FieldDescriptor field)
	{
		int result = Helpers.getJavaType(field).ordinal();

		// bit 0: whether the field is required.
		if (field.isRequired())
		{
			result |= 0x100;
		}

		// bit 1: whether the field requires UTF-8 validation.
		if (checkUtf8(field))
		{
			result |= 0x200;
		}

		// bit 2: whether the field needs isInitialized check.
		if (Helpers.getJavaType(field) == Helpers.JavaType.MESSAGE && Helpers.hasRequiredFields(field.getMessageType()))
		{
			result |= 0x400;
		}

		// bit 3: whether the field is a closed enum.
		if (field.getType() == FieldDescriptor.Type.ENUM && !supportUnknownEnumValue(field))
		{
			result |= 0x800;
		}

		return result;
	}

	public static boolean hasHasbit(FieldDescriptor descriptor)
	{
		return com.google.protobuf.CompilerInternalHelpers.hasHasbit(descriptor);
	}
}
