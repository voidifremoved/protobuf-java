# TODO: ComprehensiveTestEdgeCasesMinimal Differences

## 1. Header Generation
- [ ] **Missing Header Comments**: Expected "NO CHECKED-IN PROTOBUF GENCODE" and "Protobuf Java Version: 4.33.4".
- [ ] **Missing `@Generated` annotation**: The outer class should be annotated with `@com.google.protobuf.Generated`.
- [ ] **Missing Base Class**: The outer class should extend `com.google.protobuf.GeneratedFile`.

## 2. Runtime Version Validation
- [ ] **Missing Static Initialization**: The outer class and message classes should include `com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(...)` in a static block.

## 3. Message Class Generation
- [ ] **Incorrect Base Class**: Generated messages are extending `com.google.protobuf.GeneratedMessageV3` instead of `com.google.protobuf.GeneratedMessage`.
- [ ] **Missing UTF-8 Validation in Accessors**: `get*` methods for string fields are missing `if (bs.isValidUtf8())` check before caching the string (setting `field_ = s`).

## 4. Descriptor Generation
- [ ] **String Escaping**: Descriptor strings use octal escapes `\012` instead of standard escapes `\n` in some places, or vice-versa. The splitting logic might also differ.
- [ ] **Internal Field Naming**: Descriptor fields are named `internal_ComprehensiveTest_...` instead of `internal_static_ComprehensiveTest_...`.

## 5. Insertion Points
- [ ] **Missing or Misplaced Insertion Points**: Some `@@protoc_insertion_point` comments are missing or in different locations.

## 6. Miscellaneous
- [ ] **Annotations on Methods**: Some `@Override` or `@SuppressWarnings` annotations might be missing or different.
- [ ] **Field Ordering**: The order of generated methods and fields might differ (e.g. `registerAllExtensions`).
