# Protobuf Java Compiler Conversion TODO

This project aims to create a fully compatible Java version of the C++ protobuf compiler.
The goal is to replicate the functionality of `src/google/protobuf/compiler/java` in `protobuf-java-compiler/src/main/java/com/google/protobuf/compiler/java`.

## Phase 1: Core Framework (Generic Compiler)
- [x] `CommandLineInterface` (Basic implementation exists)
- [x] `Parser` (Basic implementation exists)
- [x] `CodeGenerator` (Interface exists)
- [x] `JavaCodeGenerator` (Refactored to use `FileGenerator`)

## Phase 2: Java-Specific Generator Structure (`com.google.protobuf.compiler.java`)
Mirroring `src/google/protobuf/compiler/java/`:

### Shared Components
- [x] `Options.java` (Port `options.h`)
- [x] `Context.java` (Port `context.cc`)
- [ ] `NameResolver.java` (Port `name_resolver.cc`) - *Exists as `ClassNameResolver.java`*
- [x] `DocComment.java` (Port `doc_comment.cc`)
- [x] `FieldCommon.java` (Port `field_common.cc`)
- [x] `Helpers.java` (Port `helpers.cc`) - *Exists as `StringUtils.java`*
- [ ] `InternalHelpers.java` (Port `internal_helpers.cc`)
- [ ] `Names.java` (Port `names.cc`)
- [x] `SharedCodeGenerator.java` (Port `shared_code_generator.cc`)
- [x] `MessageSerialization.java` (Port `message_serialization.cc`) - *Skeleton Implemented*
- [ ] `JavaFeatures.java` (Port `java_features.pb.cc` or generate it)
- [x] `FileGenerator.java` (Port `file.cc`)
- [x] `GeneratorFactory.java` (Port `generator_factory.h`)

### Full Runtime Generators (`com.google.protobuf.compiler.java.full`)
Mirroring `src/google/protobuf/compiler/java/full/`:

- [x] `GeneratorFactory.java` (Implementation for Full)
- [x] `EnumGenerator.java` (`enum.cc`)
- [x] `EnumFieldGenerator.java` (`enum_field.cc`)
- [x] `ExtensionGenerator.java` (`extension.cc`)
- [x] `FieldGenerator.java` (`field_generator.h` - Base/Interface)
- [x] `MakeFieldGens.java` (`make_field_gens.cc`)
- [x] `MapFieldGenerator.java` (`map_field.cc`)
- [x] `MessageGenerator.java` (`message.cc`) - *Implemented*
- [x] `MessageBuilderGenerator.java` (`message_builder.cc`)
- [x] `MessageFieldGenerator.java` (`message_field.cc`)
- [x] `PrimitiveFieldGenerator.java` (`primitive_field.cc`)
- [x] `ServiceGenerator.java` (`service.cc`)
- [x] `StringFieldGenerator.java` (`string_field.cc`)

### Lite Runtime Generators (`com.google.protobuf.compiler.java.lite`)
Mirroring `src/google/protobuf/compiler/java/lite/`:

- [ ] `GeneratorFactory.java` (Implementation for Lite)
- [ ] `EnumGenerator.java`
- [ ] `EnumFieldGenerator.java`
- [ ] `ExtensionGenerator.java`
- [ ] `FieldGenerator.java`
- [ ] `MakeFieldGens.java`
- [ ] `MapFieldGenerator.java`
- [ ] `MessageGenerator.java`
- [ ] `MessageBuilderGenerator.java`
- [ ] `MessageFieldGenerator.java`
- [ ] `PrimitiveFieldGenerator.java`
- [ ] `StringFieldGenerator.java`

## Phase 3: Implementation & Refactoring Steps
1.  **Refactor `Options`**: Consolidate `com.google.protobuf.compiler.Options` into `com.google.protobuf.compiler.java.Options`. - DONE
2.  **Refactor `JavaCodeGenerator`**: Update `com.google.protobuf.compiler.JavaCodeGenerator` to initialize the correct `GeneratorFactory` (Full/Lite) and delegate to `FileGenerator`. - DONE
3.  **Implement `FileGenerator`**: Implement the orchestration logic to generate the outer class and call other generators. - DONE
4.  **Implement `MessageGenerator`**: Core message generation logic. - *In Progress (Skeleton)*
5.  **Implement Field Generators**: Implement `FieldGenerator` hierarchy and factory `MakeFieldGens`. - *DONE (Primitive, String, Enum, Message, Map)*
6.  **Implement `EnumGenerator`**, `ServiceGenerator`, `ExtensionGenerator`. - *DONE*

## Phase 4: Verification
- [ ] Verify Output against C++ compiler output.
- [ ] Run existing tests in `protobuf-java-compiler`.
- [ ] Add new tests covering complex proto features.
