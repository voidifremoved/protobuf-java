# TODO: Remaining Differences from C++ Protobuf Compiler Output

This document tracks the remaining differences between our Java protobuf compiler output and the official C++ protobuf compiler's Java output.

## 1. Enum Indentation Issues

### 1.1 Enum JavaDoc Comment Indentation
- **Location**: `FileGenerator.java` - enum generation
- **Issue**: Enum JavaDoc comment should be indented with 2 spaces (at enum level), but currently has 0 spaces
- **Expected**: `  /**` (2 spaces)
- **Actual**: `/**` (0 spaces)
- **Fix**: Ensure enum JavaDoc is indented with 2 spaces in `FileGenerator.java`

### 1.2 Enum Method Indentation
- **Location**: `ImmutableEnumGenerator.java` - all enum methods
- **Issue**: All enum methods should be indented with 4 spaces (inside enum), but currently have 2 spaces
- **Affected Methods**:
  - `getNumber()`
  - `valueOf(int value)`
  - `forNumber(int value)`
  - `internalGetValueMap()`
  - `getValueDescriptor()`
  - `getDescriptorForType()`
  - `getDescriptor()`
  - `valueOf(EnumValueDescriptor desc)`
- **Expected**: `    public final int getNumber() {` (4 spaces)
- **Actual**: `  public final int getNumber() {` (2 spaces)
- **Fix**: Change indentation from 2 spaces to 4 spaces for all enum methods in `ImmutableEnumGenerator.java`

### 1.3 Enum Field Indentation
- **Location**: `ImmutableEnumGenerator.java` - `VALUES` field and private fields
- **Issue**: Fields should be indented with 4 spaces (inside enum)
- **Affected Fields**:
  - `private static final TestEnum[] VALUES = values();`
  - `private final int value;`
  - `private TestEnum(int value)` constructor
- **Expected**: `    private static final TestEnum[] VALUES = values();` (4 spaces)
- **Actual**: Currently has 4 spaces (may be correct, verify)
- **Fix**: Ensure all enum fields and constructors use 4-space indentation

## 2. Interface JavaDoc Comment Indentation

### 2.1 Field Accessor JavaDoc Indentation
- **Location**: `DocComment.java` or `ImmutableMessageGenerator.java` - interface field accessor JavaDoc
- **Issue**: JavaDoc comments for fields in the interface have incorrect indentation for the `*` lines
- **Expected**: `     * <pre>` (5 spaces: 4 for interface level + 1 for `*`)
- **Actual**: `      * <pre>` (6 spaces: 5 for interface level + 1 for `*`)
- **Affected**: All field accessor JavaDoc comments in the `OrBuilder` interface
- **Fix**: Adjust indentation in `DocComment.writeFieldAccessorDocComment` or the calling code to use correct indent prefix

## 3. Whitespace and Blank Lines

### 3.1 Blank Lines Between Enum Methods
- **Location**: `ImmutableEnumGenerator.java`
- **Issue**: Verify blank lines between enum methods match expected output
- **Fix**: Review and adjust blank line generation between enum methods

### 3.2 Blank Lines in Interface
- **Location**: `ImmutableMessageGenerator.java` - interface generation
- **Issue**: Verify blank lines in interface match expected output
- **Fix**: Review blank line generation in interface

## 4. Code Structure Ordering

### 4.1 Enum Method Order
- **Location**: `ImmutableEnumGenerator.java`
- **Issue**: Verify the order of enum methods matches expected output:
  1. `getNumber()`
  2. `valueOf(int)` (deprecated)
  3. `forNumber(int)`
  4. `internalGetValueMap()`
  5. `internalValueMap` field
  6. `getValueDescriptor()`
  7. `getDescriptorForType()`
  8. `getDescriptor()`
  9. `VALUES` field
  10. `valueOf(EnumValueDescriptor)`
  11. `value` field
  12. Constructor
- **Fix**: Verify and reorder if necessary

## 5. Missing Package Names in Class References

### 5.1 Enum Descriptor References
- **Location**: `ImmutableEnumGenerator.java` - `getDescriptor()` method
- **Issue**: Verify all class references include full package names
- **Status**: Appears correct in diff (uses `com.rubberjam.protobuf.compiler.test.edge.ComprehensiveTestEdgeCases`)
- **Fix**: Verify all references are fully qualified

## 6. Missing Fields and Methods

### 6.1 Verify All Required Methods Present
- **Location**: All generators
- **Issue**: Ensure all required methods are generated:
  - Enum methods: ✓ Present
  - Message methods: ✓ Present (based on previous fixes)
  - Builder methods: ✓ Present (based on previous fixes)
- **Fix**: Run comprehensive test to verify

## Implementation Priority

1. **High Priority**: Fix enum method indentation (affects readability and matches C++ output)
2. **High Priority**: Fix enum JavaDoc indentation (affects formatting)
3. **Medium Priority**: Fix interface JavaDoc indentation (affects formatting)
4. **Low Priority**: Verify and fix any remaining whitespace issues
5. **Low Priority**: Verify method ordering matches exactly

## Testing

After each fix, run:
```bash
mvn test -Dtest=ComprehensiveProtoParityTest
```

Compare output using:
```bash
diff -u src/test/resources/expected/java/ComprehensiveTestEdgeCases.java <generated_output>
```

## Notes

- All indentation should match the C++ protobuf compiler output exactly
- The C++ compiler uses 2-space indentation for class-level members
- Enum members inside an enum should use 4-space indentation
- Interface members should use 4-space indentation
- JavaDoc comments should align with their containing element's indentation

