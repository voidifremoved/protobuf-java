# Protobuf Java Compiler Conversion TODO

This project aims to create a fully compatible Java version of the C++ protobuf compiler.
The goal is to replicate the functionality of `src/google/protobuf/compiler/java` in `protobuf-java-compiler/src/main/java/com/google/protobuf/compiler/java`.

## Phase 1: Core Framework (Generic Compiler)
- [x] `CommandLineInterface` (Basic implementation exists)
- [x] `Parser` (Basic implementation exists)
- [x] `CodeGenerator` (Interface exists)
- [x] `JavaCodeGenerator` (Basic implementation exists, needs refactoring to use full Java generator logic)

## Phase 2: Java-Specific Generator Structure (`com.google.protobuf.compiler.java`)
Mirroring `src/google/protobuf/compiler/java/`:

### Shared Components
- [ ] `Options.java` (Port `options.h`) - *In Progress (Exists as placeholder/misplaced)*
- [x] `Context.java` (Port `context.cc`) - *Exists*
- [ ] `NameResolver.java` (Port `name_resolver.cc`) - *Exists as `ClassNameResolver.java`, verify completeness*
- [x] `DocComment.java` (Port `doc_comment.cc`) - *Exists*
- [x] `FieldCommon.java` (Port `field_common.cc`) - *Exists*
- [ ] `Helpers.java` (Port `helpers.cc`) - *Exists as `StringUtils.java`, need to verify/rename?*
- [ ] `InternalHelpers.java` (Port `internal_helpers.cc`)
- [ ] `Names.java` (Port `names.cc`)
- [ ] `SharedCodeGenerator.java` (Port `shared_code_generator.cc`)
- [ ] `MessageSerialization.java` (Port `message_serialization.cc`)
- [ ] `JavaFeatures.java` (Port `java_features.pb.cc` or generate it)
- [ ] `FileGenerator.java` (Port `file.cc`) - *Crucial for orchestration*
- [ ] `GeneratorFactory.java` (Port `generator_factory.h` - Interface?)

### Full Runtime Generators (`com.google.protobuf.compiler.java.full`)
Mirroring `src/google/protobuf/compiler/java/full/` (likely `java` dir in C++ maps to both shared and full/lite logic, but file lists showed `full/` subdir):

- [ ] `GeneratorFactory.java` (Implementation for Full)
- [ ] `EnumGenerator.java` (`enum.cc`)
- [ ] `EnumFieldGenerator.java` (`enum_field.cc`)
- [ ] `ExtensionGenerator.java` (`extension.cc`)
- [ ] `FieldGenerator.java` (`field_generator.h` - Base/Interface)
- [ ] `MakeFieldGens.java` (`make_field_gens.cc`)
- [ ] `MapFieldGenerator.java` (`map_field.cc`)
- [ ] `MessageGenerator.java` (`message.cc`)
- [ ] `MessageBuilderGenerator.java` (`message_builder.cc`)
- [ ] `MessageFieldGenerator.java` (`message_field.cc`)
- [ ] `PrimitiveFieldGenerator.java` (`primitive_field.cc`)
- [ ] `ServiceGenerator.java` (`service.cc`)
- [ ] `StringFieldGenerator.java` (`string_field.cc`)

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
(Service generator is typically not supported in Lite, need to verify)

## Phase 3: Implementation & Refactoring Steps
1.  **Refactor `Options`**: Consolidate `com.google.protobuf.compiler.Options` into `com.google.protobuf.compiler.java.Options`.
2.  **Refactor `JavaCodeGenerator`**: Update `com.google.protobuf.compiler.JavaCodeGenerator` to initialize the correct `GeneratorFactory` (Full/Lite) and delegate to `FileGenerator`.
3.  **Implement `FileGenerator`**: Implement the orchestration logic to generate the outer class and call other generators.
4.  **Implement `MessageGenerator`**: Core message generation logic.
5.  **Implement Field Generators**: Implement `FieldGenerator` hierarchy and factory `MakeFieldGens`.
6.  **Implement `EnumGenerator`**, `ServiceGenerator`, `ExtensionGenerator`.

## Phase 4: Verification
- [ ] Verify Output against C++ compiler output.
- [ ] Run existing tests in `protobuf-java-compiler`.
- [ ] Add new tests covering complex proto features.
