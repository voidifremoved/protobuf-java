# TODO: Remaining Parity Issues

## ComprehensiveTestEdgeCasesMinimal

1.  **Indentation Mismatch at End of File:**
    -   The `// @@protoc_insertion_point(outer_class_scope)` and the closing brace `}` of the outer class seem to be indented deeper (6 spaces vs 2 spaces) in the generated output compared to the expected output.
    -   Investigation into `SharedCodeGenerator` and `FileGenerator` indentation logic is needed. The `Printer` class might have a different default indentation width or accumulation logic than expected.

2.  **`FileDescriptor.getMessageType(int)` vs `getMessageTypes().get(int)`:**
    -   The expected output uses `getMessageType(int)`, which is not a standard method in `com.google.protobuf.Descriptors.FileDescriptor` (usually `getMessageTypes().get(int)`).
    -   The generator was updated to emit `getMessageType(int)` to match parity, but this might cause compilation errors if the runtime environment does not support it. This needs verification against the specific Protobuf Java runtime version used in the project (4.33.4).

3.  **SourceCodeInfo and Syntax in Embedded Descriptors:**
    -   The C++ generator strips `SourceCodeInfo` and default `syntax="proto2"` from the embedded `FileDescriptorProto` bytes.
    -   `SharedCodeGenerator.java` has been updated to replicate this behavior by manually clearing these fields before serialization.

4.  **String Literal Formatting:**
    -   The C++ generator uses `+` concatenation for split string literals in the descriptor array, while the initial Java port used `,`.
    -   `SharedCodeGenerator.java` was updated to use `+`.

5.  **Empty Line before Outer Class Scope Insertion Point:**
    -   The expected output includes an empty line before `// @@protoc_insertion_point(outer_class_scope)`, which was missing in the Java port.
    -   `FileGenerator.java` was updated to add this newline.
