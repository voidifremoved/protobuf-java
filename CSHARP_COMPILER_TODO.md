# C# Code Generator – Step-by-Step Port Plan

This document is an exhaustive, step-by-step todo list for porting the **C++ C# protobuf code generator** into **Java**, creating a C# compiler in Java that generates C# protobuf messages compatible with the original compiler. The approach mirrors the existing Java generator port: same package layout pattern, same test strategy (parity tests vs. gold masters), and same use of shared compiler infrastructure (Parser, Importer, CodeGenerator, Printer, etc.).

**Reference:** Java generator lives under `protobuf-java-compiler/src/main/java/com/rubberjam/protobuf/compiler/java/`. C++ C# source lives under `src/google/protobuf/compiler/csharp/`. CONVERSION_ORDER.md defines the C# phases (Phase C#1–C#4). PROTOC.md describes C# generator behaviour (reflection class, message layout, bit fields, etc.).

---

## Part A: Project and Test Infrastructure

### A.1 – Maven and package layout
- [ ] **A.1.1** Create package `com.rubberjam.protobuf.compiler.csharp` under `protobuf-java-compiler/src/main/java/` (no new Maven module; C# generator lives in the same compiler module as the Java generator).
- [ ] **A.1.2** Confirm `protoc-maven-plugin` execution `id=csharp` in `pom.xml` already generates gold masters into `src/test/resources/expected/csharp/` (it does; ensure it runs in `generate-test-sources` and uses the same `src/test/protobuf` inputs as Java).
- [ ] **A.1.3** Decide C# output layout: single file per proto (like current C++ generator) and default file extension `.cs`. Document in a short `csharp/README.md` or in code comments.

### A.2 – Test harness (parity tests)
- [ ] **A.2.1** Add `CSharpCodeGenerator` (or equivalent) in `com.rubberjam.protobuf.compiler` that extends `CodeGenerator` and delegates to the C# generator, returning generated file name + content (similar to `JavaCodeGenerator` and `GeneratedJavaFile`).
- [ ] **A.2.2** Add `GeneratedCSharpFile` (or re-use a generic `GeneratedSourceFile`) with: `fileName`, `namespace` (or package equivalent), `className` (reflection class name), `source`.
- [ ] **A.2.3** Add `RuntimeCSharpGenerator` in `com.rubberjam.protobuf.compiler.runtime` that: (1) takes `FileDescriptorProto` + dependencies map + parameter string, (2) builds `FileDescriptor` (reuse logic from `RuntimeJavaGenerator.buildFileDescriptor` and well-known types), (3) calls the C# code generator to produce one or more C# files, (4) returns the main generated file for single-file output (or a list if you support multiple files later).
- [ ] **A.2.4** Create `AbstractCSharpParityTest` base class in `src/test/java/com/rubberjam/protobuf/compiler/csharp/` that: (1) reads a proto from `src/test/protobuf/`, (2) parses it via `Parser` + `Tokenizer` to `FileDescriptorProto`, (3) calls `RuntimeCSharpGenerator.generateCSharpSource(...)` to get generated C#, (4) reads expected C# from `src/test/resources/expected/csharp/<ExpectedFileName>.cs`, (5) normalizes (e.g. trim, line endings) and compares with a unified diff; on mismatch writes `target/<name>_Expected.txt` and `_Actual.txt` and asserts (mirror `AbstractProtoParityTest` for Java).
- [ ] **A.2.5** Add dependency on `com.github.difflib` (or same as Java tests) for diff generation in C# parity tests if not already present.

### A.3 – Gold masters and test coverage scope
- [ ] **A.3.1** Run `mvn generate-test-sources` and confirm `expected/csharp/` is populated with `.cs` files from native protoc for all test protos (ComprehensiveTest*, IncrementalTest*, etc.).
- [ ] **A.3.2** List which protos produce which expected C# file (e.g. `comprehensive_test_v2.proto` → `ComprehensiveTestV2.cs`). Document or derive from C++ `GetOutputFile` / namespace rules so parity tests use the correct expected path.
- [ ] **A.3.3** Create one parity test class per “family” (mirror Java): e.g. `ComprehensiveTestV2CSharpParityTest`, `ComprehensiveTestV2LiteRuntimeCSharpParityTest`, `ComprehensiveTestEdgeCasesCSharpParityTest`, `ComprehensiveTestNestedCSharpParityTest`, `ComprehensiveTestExtensionsCSharpParityTest`, `IncrementalTestStep1CSharpParityTest` … `IncrementalTestStep10CSharpParityTest`, and any V3 / Minimal / Speed variants that have expected C#. Each test method calls `verifyParity(protoFileName, expectedCsFileName)`.
- [ ] **A.3.4** Ensure expected file names match C# generator output (single file per proto; name from `GetOutputFile` logic). If C# generator uses directory layout (e.g. namespace path), either flatten expecteds for tests (like Java’s antrun flatten) or adapt test to resolve expected path from namespace + class name.

---

## Part B: C# Generator – Options, Names, Helpers, Doc, Base (Phase C#1)

Port C++ files in order; each item = translate the C++ type/API into a Java class in `com.rubberjam.protobuf.compiler.csharp`, preserving behaviour.

### B.1 – Options
- [ ] **B.1.1** Port `csharp_options.h` → `Options.java`: fields `file_extension` (default `.cs`), `base_namespace`, `base_namespace_specified`, `internal_access`, `serializable`, `strip_nonfunctional_codegen`; constructor defaults; getters/setters or immutable style.
- [ ] **B.1.2** Implement `Options.fromParameter(String parameter)`: parse `parameter` (e.g. `key1=value1,key2=value2` or `key:value`), map to options (file_extension, base_namespace, internal_access, serializable, experimental_strip_nonfunctional_codegen). Mirror C++ `ParseGeneratorParameter` behaviour used in `csharp_generator.cc`.

### B.2 – Names
- [ ] **B.2.1** Port `names.h` / `names.cc` → `Names.java`: C# naming helpers (e.g. namespace from package, PascalCase type names, property names). Key functions to port: anything that converts proto package to C# namespace, underscore_to_PascalCase (or equivalent), file/class name from proto name.
- [ ] **B.2.2** Add unit test `NamesTest.java` if C++ has name tests; otherwise add a few assertions for namespace and type name generation.

### B.3 – Helpers
- [ ] **B.3.1** Port `csharp_helpers.h` / `csharp_helpers.cc` → `CSharpHelpers.java`: type mapping (proto type → C# type string), default value formatting for C#, wire type / tag helpers if used by C# generator. Include handling for primitives, enums, messages, groups, wrappers.
- [ ] **B.3.2** Ensure float/double/inf/nan and default value formatting match C++ (see PROTOC.md learnings for Java; apply analogous rules for C#).

### B.4 – Doc comments
- [ ] **B.4.1** Port `csharp_doc_comment.h` / `csharp_doc_comment.cc` → `CSharpDocComment.java`: XML doc (e.g. `///`) generation from `SourceCodeInfo`; escape rules for C# (`&`, `<`, `>` etc.). Reuse `SourceCodeInfo` path logic from `DocComment.java` (e.g. declaration-order field index) if sharing is feasible; otherwise duplicate only the path/location lookup needed for C#.
- [ ] **B.4.2** Add tests for doc comment escaping and placement (or rely on parity tests).

### B.5 – Source generator base
- [ ] **B.5.1** Port `csharp_source_generator_base.h` / `csharp_source_generator_base.cc` → `SourceGeneratorBase.java`: abstract base holding `Options`; method `classAccessLevel()` (public vs internal); `writeGeneratedCodeAttributes(Printer)` for `[GeneratedCode]` etc. Subclasses are message generator, enum generator, reflection class generator.

---

## Part C: C# Generator – Field Base and Field Types (Phase C#2)

### C.1 – Field base
- [ ] **C.1.1** Port `csharp_field_base.h` / `csharp_field_base.cc` → `CSharpFieldBase.java`: base type for all field generators (primitive, enum, message, map, wrapper, repeated*). Holds descriptor, options, printer; abstract methods for generating property, serialization, parsing, size, etc. Mirror the C++ `FieldGeneratorBase` API used by the concrete field generators.

### C.2 – Primitive field
- [ ] **C.2.1** Port `csharp_primitive_field.*` → `CSharpPrimitiveFieldGenerator.java`: optional/singular primitive; has-bit handling (`_hasBits0` etc.), property getter/setter, `WriteTo`, `getSerializedSize`, parsing, clearing. Use `CSharpDocComment` for property docs.

### C.3 – Enum field
- [ ] **C.3.1** Port `csharp_enum_field.*` → `CSharpEnumFieldGenerator.java`: enum field codegen; handle closed vs open enums if C# supports it (e.g. `UNRECOGNIZED`).

### C.4 – Message field
- [ ] **C.4.1** Port `csharp_message_field.*` → `CSharpMessageFieldGenerator.java`: singular message field; null handling, `WriteTo`, parsing, size.

### C.5 – Map field
- [ ] **C.5.1** Port `csharp_map_field.*` → `CSharpMapFieldGenerator.java`: map<K,V>; use of `MapField` / codec; serialization and parsing.

### C.6 – Wrapper field
- [ ] **C.6.1** Port `csharp_wrapper_field.*` → `CSharpWrapperFieldGenerator.java`: well-known type wrappers (e.g. Int32Value); property and serialization.

### C.7 – Repeated primitive
- [ ] **C.7.1** Port `csharp_repeated_primitive_field.*` → `CSharpRepeatedPrimitiveFieldGenerator.java`: repeated primitives; packed/unpacked; `RepeatedField<T>` style API.

### C.8 – Repeated enum
- [ ] **C.8.1** Port `csharp_repeated_enum_field.*` → `CSharpRepeatedEnumFieldGenerator.java`: repeated enums; packed if applicable.

### C.9 – Repeated message
- [ ] **C.9.1** Port `csharp_repeated_message_field.*` → `CSharpRepeatedMessageFieldGenerator.java`: repeated message fields; collection property, serialization, parsing.

### C.10 – Field factory
- [ ] **C.10.1** Add `CSharpFieldGeneratorFactory` (or equivalent): given a `FieldDescriptor` and options, return the appropriate `CSharpFieldBase` implementation (primitive, enum, message, map, wrapper, repeated primitive/enum/message). Mirror C++ decision logic.

---

## Part D: C# Generator – Message, Enum, Reflection, Generator (Phase C#3)

### D.1 – Message generator
- [ ] **D.1.1** Port `csharp_message.h` / `csharp_message.cc` → `CSharpMessageGenerator.java`: generates `sealed partial class` for a message; `_hasBits0` (and `_hasBitsN` if needed); fields sorted by field number; nested types inside `public static partial class Types`; properties and methods (WriteTo, getSerializedSize, merge, clone, etc.). Recursively instantiate generators for nested messages/enums.
- [ ] **D.1.2** Implement field ordering: sort by field number for serialization/size/parsing to match C++ and expected output.

### D.2 – Enum generator
- [ ] **D.2.1** Port `csharp_enum.h` / `csharp_enum.cc` → `CSharpEnumGenerator.java`: generates C# enum type with value names and numbers; XML doc from `CSharpDocComment`.

### D.3 – Reflection class generator
- [ ] **D.3.1** Port `csharp_reflection_class.h` / `csharp_reflection_class.cc` → `CSharpReflectionClassGenerator.java`: generates the file-level reflection container (e.g. `ComprehensiveTestV2Reflection`): (1) static `FileDescriptor` built from embedded descriptor bytes, (2) Base64-encoded `FileDescriptorProto` split into 60-char chunks in `string.Concat(...)`, (3) registration of extensions if any, (4) nested message and enum declarations (or delegates to message/enum generators). This is the top-level “file” view in C# (one big partial class with nested types).
- [ ] **D.3.2** Ensure descriptor bytes are produced in the same format as C++ (e.g. no extra options that change serialization); optionally run a small test that compares descriptor bytes for a known proto with C++ output.

### D.4 – C# generator (entry point)
- [ ] **D.4.1** Port `csharp_generator.h` / `csharp_generator.cc` → `CSharpGenerator.java`: implements `CodeGenerator.generate(FileDescriptor, String parameter, GeneratorContext)`. Parse parameter into `Options`; compute output file path (single .cs file) via `GetOutputFile` logic (namespace → path, base_namespace option, file_extension); open output from context; create `Printer`; call `ReflectionClassGenerator.Generate(printer)` (or equivalent) to produce the single file; write to context.
- [ ] **D.4.2** Implement `GetOutputFile`: return relative path (e.g. `Namespace/ReflectionClassName.cs` or flat `ReflectionClassName.cs` depending on options). Match C++ behaviour so parity test expected paths align.
- [ ] **D.4.3** Override `getSupportedFeatures()`, `getMinimumEdition()`, `getMaximumEdition()` to match C++ (e.g. PROTO3_OPTIONAL, EDITION_PROTO2, EDITION_2024).

### D.5 – Bridge to compiler
- [ ] **D.5.1** Create `CSharpCodeGenerator` in `com.rubberjam.protobuf.compiler` that extends `CodeGenerator`: in `generate()`, delegates to `CSharpGenerator` and collects generated output into `GeneratedCSharpFile` (or list) for use by `RuntimeCSharpGenerator` and parity tests.
- [ ] **D.5.2** (Optional) Register C# generator in `CommandLineInterface` or a main entry (e.g. `--csharp_out`) so the compiler can be invoked for C# from the CLI; ensure parameter is passed through (e.g. `base_namespace=...,file_extension=.cs`).

---

## Part E: Tests and Parity (Phase C#4 + same approach as Java)

### E.1 – Parity test implementation
- [ ] **E.1.1** Implement `AbstractCSharpParityTest.verifyParity(protoFileName, expectedCsFileName)`: parse proto → `FileDescriptorProto`; build `FileDescriptor` (with dependencies); run C# generator; read expected from `src/test/resources/expected/csharp/<expectedCsFileName>.cs`; compare trimmed content with unified diff; on failure write `target/<name>_Expected.txt` / `_Actual.txt` and assert.
- [ ] **E.1.2** Handle C# output layout: if generator writes one file per proto with a known name, expected file name should match (e.g. `ComprehensiveTestV2.cs`). If generator uses subdirs by namespace, either flatten expecteds for tests or resolve path in test.

### E.2 – Per-proto parity test classes
- [ ] **E.2.1** Add parity test for each proto that has an expected C# file: e.g. `ComprehensiveTestV2CSharpParityTest`, `ComprehensiveTestV2LiteRuntimeCSharpParityTest`, `ComprehensiveTestV2MinimalCSharpParityTest`, `ComprehensiveTestV3CSharpParityTest`, `ComprehensiveTestV3LiteRuntimeCSharpParityTest`, `ComprehensiveTestV3MinimalCSharpParityTest`, `ComprehensiveTestV3SpeedCSharpParityTest`, `ComprehensiveTestNestedCSharpParityTest`, `ComprehensiveTestNestedLiteRuntimeCSharpParityTest`, `ComprehensiveTestNestedMinimalCSharpParityTest`, `ComprehensiveTestEdgeCasesCSharpParityTest`, `ComprehensiveTestEdgeCasesLiteRuntimeCSharpParityTest`, `ComprehensiveTestEdgeCasesMinimalCSharpParityTest`, `ComprehensiveTestExtensionsCSharpParityTest`, `ComprehensiveTestExtensionsLiteRuntimeCSharpParityTest`, `ComprehensiveTestExtensionsMinimalCSharpParityTest`, `IncrementalTestStep1CSharpParityTest` … `IncrementalTestStep10CSharpParityTest`. Each contains a single test method that calls `verifyParity(protoFile, expectedCsFile)`.
- [ ] **E.2.2** Ensure proto file names and expected .cs file names are correct (e.g. `comprehensive_test_v2_lite_runtime.proto` → `ComprehensiveTestV2LiteRuntime.cs` if that’s what native protoc produces).

### E.3 – Unit tests for C# components (optional but recommended)
- [ ] **E.3.1** Port or adapt C++ `csharp_bootstrap_unittest.cc` → `CSharpBootstrapTest.java`: minimal proto → generate C# → sanity check (e.g. contains expected class name and namespace).
- [ ] **E.3.2** Port or adapt C++ `csharp_generator_unittest.cc` → `CSharpGeneratorTest.java`: test parameter parsing, output path, and possibly a small proto round-trip.
- [ ] **E.3.3** Add unit tests for `Names`, `CSharpHelpers`, `Options`, `CSharpDocComment` where it adds value (e.g. edge cases in naming or default values).

### E.4 – Gold master refresh
- [ ] **E.4.1** Document how to regenerate expected C# files: run `mvn generate-test-sources` (which runs native protoc for C# into `expected/csharp/`). Do not commit unrelated changes when updating gold masters.
- [ ] **E.4.2** If C# generator produces files in a directory structure (e.g. by namespace), add antrun or script to flatten `expected/csharp` for parity tests if needed (similar to Java’s flatten step), or make tests read from the structured path.

---

## Part F: Documentation and Cleanup

### F.1 – Docs
- [ ] **F.1.1** Update `CONVERSION_ORDER.md`: mark C# Phase C#1–C#3 items as **Done** as you complete them; add a short “C# generator (Java)” section if useful.
- [ ] **F.1.2** Update `README.md` or add `protobuf-java-compiler/README.md` to mention C# code generation (e.g. “Generates Java and C# code compatible with official protoc”).
- [ ] **F.1.3** Add brief `protobuf-java-compiler/src/main/java/.../csharp/README.md` describing package layout and how it maps from C++ `compiler/csharp/`.

### F.2 – Code quality
- [ ] **F.2.1** Run existing Java parity tests and ensure no regressions.
- [ ] **F.2.2** Run all C# parity tests; fix any mismatches (formatting, ordering, default values, doc comments, descriptor encoding) until they pass.
- [ ] **F.2.3** Add a Maven profile or note for “run only C# tests” (e.g. `-Dtest=*CSharp*ParityTest`) for faster iteration.

---

## Summary Checklist (high level)

| Phase | Description | Status |
|-------|-------------|--------|
| A    | Project layout, test harness, gold masters, parity test scope | |
| B    | C# options, names, helpers, doc comment, source generator base | |
| C    | Field base + all field types (primitive, enum, message, map, wrapper, repeated*) | |
| D    | Message generator, enum generator, reflection class generator, C# generator entry, CLI bridge | |
| E    | Parity tests for all test protos, unit tests, gold master process | |
| F    | Documentation and cleanup | |

---

## Dependency order (for implementation)

1. **A.1–A.3** (infra and test list) can be done first.
2. **B.1 → B.2 → B.3 → B.4 → B.5** in order (options used by generator; names and helpers used by fields and reflection; doc comment used by fields; base used by message/enum/reflection).
3. **C.1** then **C.2–C.9** (field types), then **C.10** (factory).
4. **D.2** (enum) and **D.1** (message) can be done after C; **D.3** (reflection) uses message and enum; **D.4** uses D.3; **D.5** wraps D.4.
5. **E** after D is working (at least one parity test green); then expand to full list.
6. **F** ongoing.

Use the same coding style and patterns as the Java generator (e.g. `Printer` with `$variable$`, `Options` from parameter, context objects where useful). For each C++ `.cc`/`.h` pair, create a single Java class (or split only where it improves readability), and add a short comment “Ported from compiler/csharp/…” at the top of the file.
