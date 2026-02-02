# TODO

## Outstanding Issues

### 1. FileGenerator / Extension Registry
- **Current Implementation:** `FileGenerator.generateDescriptorInitializationCodeForImmutable` registers *all* top-level extensions defined in the file into the `ExtensionRegistry` used for `internalUpdateFileDescriptor`.
- **Desired Implementation:** Match C++ `CollectExtensions` logic to only register extensions that are actually *used* in the file's options (FileOptions, MessageOptions, etc.). This requires parsing the descriptor options to find unknown fields that correspond to extensions.

### 2. SharedCodeGenerator Refactoring
- **Current State:** `SharedCodeGenerator` now only contains `generateDescriptors`.
- **Cleanup:** verify if `SharedCodeGenerator` should be merged into `FileGenerator` or if it should be kept to mirror C++ structure (where it handles more in non-OSS builds).

### 3. Any Methods
- **Current State:** `generateAnyMethods` in `ImmutableMessageGenerator` is a stub/partial implementation.
- **Action:** Fully port `GenerateAnyMethods` from `message.cc` if `Any` proto support is required.

### 4. Lite Runtime
- **Current State:** `FileGenerator` uses `ImmutableGeneratorFactory` hardcoded.
- **Action:** Implement switching to `LiteGeneratorFactory` based on `options.enforceLite` or `file.getOptions().getOptimizeFor()`.
