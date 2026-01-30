# C++ Protobuf Compiler → Java Conversion Order

**Future Agents: Please mark items as **Done** in the Status column upon completion.**

This list orders the C++ source files under `protobuf-java/src/google/protobuf/compiler/` (shared compiler, `compiler/java/`, `compiler/cpp/`, and `compiler/csharp/`) so that each converted Java type can depend on already-converted types. Dependencies on `google/protobuf/io/*`, `google/protobuf/descriptor*`, and `google/protobuf/*.pb.h` are assumed satisfied by existing Java protobuf runtime (e.g. `com.google.protobuf.*`) or by already translated classes (eg. `com.rubberjam.protobuf.io.Printer`).

The target for this translation is the package com.rubberjam.protobuf.another. Under the compiler subpackage in this package, there are already some translated classes in (ClassNameResolver, DocComment, FieldCommon, Helpers, Names, Options, Parser, Importer, CodeGenerator, CommandLineInterface, Context, GeneratorFactory) and tests for each of these in the test subdirectory. IO classes like Printer already exist in com.rubberjam.protobuf.io, and should be translated to there. These should serve as a model for how to proceed, ie translate a class into this target location and

**Scope:** Shared compiler + Java + C++ + C# code generators. Other language dirs (kotlin/, objectivec/, python/, php/, rust/, ruby/) and test/harness files (`*_unittest.cc`, `*_test.cc`, `command_line_interface_tester`, `fake_plugin`, `mock_code_generator`, `test_plugin`) are listed at the end; convert after the main pipeline.

---

## Phase 1: Compiler shared – leaf types

| # | C++ file(s) | Notes / dependencies | Status |
|---|-------------|----------------------|--------|
| 1 | `package_info.h` | Package/version constants only. | **Done** |
| 2 | `code_generator_lite.h`, `code_generator_lite.cc` | Descriptor, descriptor.pb; `ParseGeneratorParameter`. | **Done** |
| 3 | `retention.h`, `retention.cc` | Descriptor, descriptor.pb; strip source retention. | **Done** |
| 4 | `scc.h` | Header-only; descriptor (SCC for message deps). | **Done** |
| 5 | `plugin.h`, `plugin.cc` | CodeGenerator usage; plugin.pb (use existing Java plugin proto). | **Done** |

---

## Phase 2: Parser and importer

| # | C++ file(s) | Notes / dependencies | Status |
|---|-------------|----------------------|--------|
| 6 | `parser.h`, `parser.cc` | Tokenizer, descriptor, descriptor.pb; .proto parser. | **Done** |
| 7 | `importer.h`, `importer.cc` | Parser, descriptor, descriptor_database; SourceTree, Importer, ErrorCollector. | **Done** |

---

## Phase 3: Code generator and shared infra

| # | C++ file(s) | Notes / dependencies | Status |
|---|-------------|----------------------|--------|
| 8 | `code_generator.h`, `code_generator.cc` | code_generator_lite, descriptor, plugin.pb, feature_resolver. | **Done** |
| 9 | `versions.h`, `versions.cc` | plugin.pb; runtime version handling. | **Done** |
| 10 | `zip_writer.h`, `zip_writer.cc` | io/coded_stream; zip output. | **Done** |
| 11 | `subprocess.h`, `subprocess.cc` | io (win32), message; plugin subprocess. | **Done** |

---

## Phase 4: Command-line and main

| # | C++ file(s) | Notes / dependencies | Status |
|---|-------------|----------------------|--------|
| 12 | `command_line_interface.h`, `command_line_interface.cc` | code_generator_lite, plugin.pb, importer, code_generator, plugin, retention, subprocess, versions, zip_writer, descriptor. | **Done** |
| 13 | `main.cc`, `main_no_generators.cc` | command_line_interface. | **Done** |

---

## Phase 5: Java – options and names

| # | C++ file(s) | Notes / dependencies | Status |
|---|-------------|----------------------|--------|
| 14 | `java/options.h` | Port / options only. | **Done** |
| 15 | `java/names.h`, `java/names.cc` | options; Java naming. | **Done** |
| 16 | `java/names_internal.h` | code_generator_lite, descriptor; internal name helpers. | **Done** |

---

## Phase 6: Java – field common, resolver, helpers, doc

| # | C++ file(s) | Notes / dependencies | Status |
|---|-------------|----------------------|--------|
| 17 | `java/field_common.h`, `java/field_common.cc` | options, descriptor; FieldGeneratorInfo, oneof vars. | **Done** |
| 18 | `java/name_resolver.h`, `java/name_resolver.cc` | options; ClassNameResolver. | **Done** |
| 19 | `java/generator_common.h` | name_resolver, descriptor, io/printer; FieldGenerator base. | **Done** |
| 20 | `java/helpers.h`, `java/helpers.cc` | names, options, descriptor, io/printer. | **Done** |
| 21 | `java/doc_comment.h`, `java/doc_comment.cc` | options, descriptor; doc comment extraction. | **Done** |
| 22 | `java/internal_helpers.h`, `java/internal_helpers.cc` | Helpers used by message/field generators. | **Done** |

---

## Phase 7: Java – context and generator factory interface

| # | C++ file(s) | Notes / dependencies | Status |
|---|-------------|----------------------|--------|
| 23 | `java/context.h`, `java/context.cc` | field_common, helpers, options; per-file generation context. | **Done** |
| 24 | `java/generator_factory.h` | Abstract factory for full vs lite (no .cc at this level). | **Done** |

---

## Phase 8: Java full – field generators

| # | C++ file(s) | Notes / dependencies |
|---|-------------|----------------------|
| 25 | `java/full/field_generator.h` | generator_common, io/printer; ImmutableFieldGenerator. | **Done** |
| 26 | `java/full/primitive_field.h`, `primitive_field.cc` | field_generator, context. | **Done** |
| 27 | `java/full/enum_field.h`, `enum_field.cc` | field_generator, context. |
| 28 | `java/full/string_field.h`, `string_field.cc` | field_generator, context. |
| 29 | `java/full/message_field.h`, `message_field.cc` | field_generator, context. |
| 30 | `java/full/map_field.h`, `map_field.cc` | field_generator, context. |

---

## Phase 9: Java full – message, builder, extension, service

| # | C++ file(s) | Notes / dependencies |
|---|-------------|----------------------|
| 31 | `java/full/extension.h`, `extension.cc` | context, descriptor. |
| 32 | `java/full/message_builder.h`, `message_builder.cc` | context, field_generator, message. |
| 33 | `java/full/message.h`, `message.cc` | generator_factory, field_generator, context. |
| 34 | `java/full/service.h`, `service.cc` | context, descriptor. |
| 35 | `java/full/generator_factory.cc` | context; MakeImmutableGeneratorFactory. |
| 36 | `java/full/make_field_gens.h`, `make_field_gens.cc` | All full field types; factory for field generators. |

---

## Phase 10: Java lite – same structure as full

| # | C++ file(s) | Notes / dependencies |
|---|-------------|----------------------|
| 37 | `java/lite/field_generator.h` | generator_common, io/printer. |
| 38 | `java/lite/primitive_field.h`, `primitive_field.cc` | |
| 39 | `java/lite/enum_field.h`, `enum_field.cc` | |
| 40 | `java/lite/string_field.h`, `string_field.cc` | |
| 41 | `java/lite/message_field.h`, `message_field.cc` | |
| 42 | `java/lite/map_field.h`, `map_field.cc` | |
| 43 | `java/lite/extension.h`, `extension.cc` | |
| 44 | `java/lite/message_builder.h`, `message_builder.cc` | |
| 45 | `java/lite/message.h`, `message.cc` | |
| 46 | `java/lite/generator_factory.cc` | |
| 47 | `java/lite/make_field_gens.h`, `make_field_gens.cc` | |

---

## Phase 11: Java – file generator and shared code

| # | C++ file(s) | Notes / dependencies |
|---|-------------|----------------------|
| 48 | `java/file.h`, `java/file.cc` | context, message (full/lite), generator_factory, extension, name_resolver; FileGenerator. |
| 49 | `java/message_serialization.h`, `message_serialization.cc` | generator_common, helpers, descriptor, io/printer. |
| 50 | `java/shared_code_generator.h`, `shared_code_generator.cc` | code_generator, helpers, name_resolver, names, options, retention, versions, descriptor, io/printer. |
| 51 | `java/java_features.pb.h`, `java_features.pb.cc` | Generated from java_features.proto; use existing or generate. |

---

## Phase 12: Java generator and plugin main

| # | C++ file(s) | Notes / dependencies |
|---|-------------|----------------------|
| 52 | `java/generator.h`, `generator.cc` | code_generator, file, helpers, name_resolver, options, shared_code_generator, descriptor; JavaGenerator. |
| 53 | `java/plugin_main.cc` | generator, plugin; protoc-gen-java entry. |

---

---

## C++ compiler (`compiler/cpp/`)

**Prerequisite:** Phases 1–4 (shared compiler: code_generator, descriptor, plugin, etc.). C++ generator depends on shared `CodeGenerator`, descriptor, io/printer, scc.

### Phase C++1: C++ – options and names

| # | C++ file(s) | Notes / dependencies |
|---|-------------|----------------------|
| 1 | `cpp/options.h` | Port; C++ generator options (EnforceOptimizeMode, etc.). |
| 2 | `cpp/names.h` | Header-only; C++ naming (class names, field names, etc.). |

### Phase C++2: C++ – helpers and layout

| # | C++ file(s) | Notes / dependencies |
|---|-------------|----------------------|
| 3 | `cpp/helpers.h`, `cpp/helpers.cc` | code_generator, names, options, scc, descriptor; C++ codegen helpers. |
| 4 | `cpp/message_layout_helper.h`, `cpp/message_layout_helper.cc` | descriptor, options; message layout / field ordering. |
| 5 | `cpp/parse_function_generator.h`, `cpp/parse_function_generator.cc` | descriptor, helpers, options; parse from string. |
| 6 | `cpp/padding_optimizer.h` | Header-only; padding for struct layout. |
| 7 | `cpp/ifndef_guard.h`, `cpp/ifndef_guard.cc` | Guard macro generation. |
| 8 | `cpp/namespace_printer.h`, `cpp/namespace_printer.cc` | helpers, options; namespace open/close. |

### Phase C++3: C++ – field and generators

| # | C++ file(s) | Notes / dependencies |
|---|-------------|----------------------|
| 9 | `cpp/field.h`, `cpp/field.cc` | helpers, options, descriptor; FieldGeneratorBase, field chunk. |
| 10 | `cpp/field_chunk.h`, `cpp/field_chunk.cc` | Field chunk grouping. |
| 11 | `cpp/field_generators/generators.h` | field, helpers, options; Make*Generator declarations. |
| 12 | `cpp/field_generators/primitive_field.cc` | field, helpers. |
| 13 | `cpp/field_generators/enum_field.cc` | field, helpers. |
| 14 | `cpp/field_generators/string_field.cc`, `string_view_field.cc` | field, helpers. |
| 15 | `cpp/field_generators/message_field.cc` | field, helpers. |
| 16 | `cpp/field_generators/map_field.cc` | field, helpers. |
| 17 | `cpp/field_generators/cord_field.cc` | field, helpers. |

### Phase C++4: C++ – enum, extension, service, message, file

| # | C++ file(s) | Notes / dependencies |
|---|-------------|----------------------|
| 18 | `cpp/enum.h`, `cpp/enum.cc` | options, descriptor; EnumGenerator. |
| 19 | `cpp/extension.h`, `cpp/extension.cc` | helpers, options; ExtensionGenerator. |
| 20 | `cpp/service.h`, `cpp/service.cc` | helpers, options, descriptor; ServiceGenerator. |
| 21 | `cpp/message.h`, `cpp/message.cc` | enum, extension, field, helpers, message_layout_helper, options, parse_function_generator; MessageGenerator. |
| 22 | `cpp/file.h`, `cpp/file.cc` | enum, extension, helpers, message, options, service, scc; FileGenerator. |
| 23 | `cpp/tracker.h`, `cpp/tracker.cc` | helpers, options, descriptor; Tracker for cross-file refs. |

### Phase C++5: C++ generator and main

| # | C++ file(s) | Notes / dependencies |
|---|-------------|----------------------|
| 24 | `cpp/generator.h`, `cpp/generator.cc` | code_generator, file, helpers, options; C++ Generator (CodeGenerator impl). |
| 25 | `cpp/cpp_generator.h` | Thin wrapper including generator.h (for main.cc). |
| 26 | `cpp/main.cc` | cpp_generator; protoc C++ plugin entry (optional; main.cc in compiler/ also registers cpp). |

### Phase C++6: C++ tests and tools (optional / last)

| # | C++ file(s) | Notes |
|---|-------------|--------|
| 27 | `cpp/unittest.h`, `unittest.cc`, `unittest.inc` | Test harness; unittest.pb.h, etc. |
| 28 | `cpp/generator_unittest.cc`, `bootstrap_unittest.cc`, `file_unittest.cc`, etc. | Tests. |
| 29 | `cpp/tools/analyze_profile_proto.h`, `analyze_profile_proto.cc`, `analyze_profile_proto_main.cc` | Profile analysis tool. |

---

## C# compiler (`compiler/csharp/`)

**Prerequisite:** Phases 1–4 (shared compiler). C# generator depends on shared `CodeGenerator`, descriptor, io/printer.

### Phase C#1: C# – options, names, helpers, doc, base

| # | C++ file(s) | Notes / dependencies |
|---|-------------|----------------------|
| 1 | `csharp/csharp_options.h` | Port; C# generator options. |
| 2 | `csharp/names.h`, `csharp/names.cc` | descriptor.pb; C# naming. |
| 3 | `csharp/csharp_helpers.h`, `csharp/csharp_helpers.cc` | names, options, descriptor; C# codegen helpers. |
| 4 | `csharp/csharp_doc_comment.h`, `csharp/csharp_doc_comment.cc` | descriptor; doc comment extraction. |
| 5 | `csharp/csharp_source_generator_base.h`, `csharp_source_generator_base.cc` | code_generator, io/printer; SourceGeneratorBase, Options. |

### Phase C#2: C# – field base and field types

| # | C++ file(s) | Notes / dependencies |
|---|-------------|----------------------|
| 6 | `csharp/csharp_field_base.h`, `csharp_field_base.cc` | csharp_source_generator_base, descriptor, io/printer; FieldGeneratorBase. |
| 7 | `csharp/csharp_primitive_field.h`, `csharp_primitive_field.cc` | field_base, doc_comment, helpers. |
| 8 | `csharp/csharp_enum_field.h`, `csharp_enum_field.cc` | field_base, helpers. |
| 9 | `csharp/csharp_message_field.h`, `csharp_message_field.cc` | field_base, helpers. |
| 10 | `csharp/csharp_map_field.h`, `csharp_map_field.cc` | field_base, helpers. |
| 11 | `csharp/csharp_wrapper_field.h`, `csharp_wrapper_field.cc` | field_base, doc_comment, helpers, options. |
| 12 | `csharp/csharp_repeated_primitive_field.h`, `csharp_repeated_primitive_field.cc` | field_base, doc_comment, helpers. |
| 13 | `csharp/csharp_repeated_enum_field.h`, `csharp_repeated_enum_field.cc` | field_base, helpers. |
| 14 | `csharp/csharp_repeated_message_field.h`, `csharp_repeated_message_field.cc` | field_base, doc_comment, helpers. |

### Phase C#3: C# – message, enum, reflection, generator

| # | C++ file(s) | Notes / dependencies |
|---|-------------|----------------------|
| 15 | `csharp/csharp_message.h`, `csharp_message.cc` | source_generator_base, helpers; MessageGenerator. |
| 16 | `csharp/csharp_enum.h`, `csharp_enum.cc` | source_generator_base, helpers; EnumGenerator. |
| 17 | `csharp/csharp_reflection_class.h`, `csharp_reflection_class.cc` | source_generator_base, descriptor; ReflectionClassGenerator. |
| 18 | `csharp/csharp_generator.h`, `csharp_generator.cc` | code_generator, csharp_helpers, csharp_options, csharp_reflection_class, names; C# Generator. |

### Phase C#4: C# tests (optional / last)

| # | C++ file(s) | Notes |
|---|-------------|--------|
| 19 | `csharp/csharp_bootstrap_unittest.cc` | Bootstrap test. |
| 20 | `csharp/csharp_generator_unittest.cc` | Generator tests. |

---

## Phase 13: Tests and harness (optional / last)

| # | C++ file(s) | Notes |
|---|-------------|--------|
| 54 | `mock_code_generator.h`, `mock_code_generator.cc` | Test harness. |
| 55 | `command_line_interface_tester.h`, `command_line_interface_tester.cc` | Test harness. |
| 56 | `code_generator_unittest.cc` | Tests. |
| 57 | `parser_unittest.cc` | Tests. |
| 58 | `importer_unittest.cc` | Tests. |
| 59 | `command_line_interface_unittest.cc` | Tests. |
| 60 | `retention_unittest.cc` | Tests. |
| 61 | `versions_test.cc` | Tests. |
| 62 | `java/generator_unittest.cc` | Tests. |
| 63 | `java/plugin_unittest.cc` | Tests. |
| 64 | `java/doc_comment_unittest.cc` | Tests. |
| 65 | `java/name_resolver_test.cc` | Tests. |
| 66 | `java/message_serialization_unittest.cc` | Tests. |
| 67 | `test_plugin.cc`, `fake_plugin.cc` | Plugin test binaries. |

---

## Summary

- **Phases 1–4:** Shared compiler (parser, importer, code generator, plugin, CLI, main).
- **Phases 5–7:** Java support layer (options, names, helpers, context, generator factory interface).
- **Phases 8–9:** Java “full” (immutable) API generators (field types, message, builder, extension, service).
- **Phase 10:** Java “lite” API generators (same layout as full).
- **Phases 11–12:** Java file generator, shared code, main Java generator, plugin_main.
- **C++ (Phase C++1–C++5):** C++ generator: options, names → helpers, layout, parse → field, field_generators → enum, extension, service, message, file, tracker → generator, cpp_generator, main. C++6 = tests/tools.
- **C# (Phase C#1–C#3):** C# generator: options, names, helpers, doc, source_generator_base → field_base, field types (primitive, enum, message, map, wrapper, repeated*) → message, enum, reflection_class, csharp_generator. C#4 = tests.
- **Phase 13:** Shared tests and mock/harness code.

Convert each `.h` and `.cc` pair (or header-only) in the order above so that Java types map 1:1 and depend only on earlier entries. For generated protos (`plugin.pb`, `java_features.pb`, `cpp_features.pb`), use existing Java equivalents or generate from `.proto` first and treat as phase 0.

For any class that has a related unit test class (eg. parser.cc, parser.h, parser_unittest.cc) use that as the basis for a Junit test, and iterate until that test passes. If no such test for the class exists, create one and iterate until it passes.

