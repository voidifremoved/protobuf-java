# TODO: protobuf-java-compiler parity status

## ComprehensiveTestEdgeCasesMinimal Parity

### Resolved Issues
The following issues were identified and resolved during the investigation:
1.  **Compiler Version:** Updated `Versions.java` to match the expected `4.33.4` output (was `4.34.0-dev`).
2.  **Base Class & Headers:** Updated `FileGenerator` and `ImmutableMessageGenerator` to use `GeneratedMessage` (instead of `V3`), correct `internal_static_` naming, and standard generated file headers.
3.  **Builder Logic:** Fixed `MessageBuilderGenerator` to correctly implement `buildPartial` using `from_bitField0_` variables, aligned `mergeFrom`/`clone` methods, and added `builder_implements` insertion points.
4.  **Field Generation:** Updated `PrimitiveFieldGenerator`, `StringFieldGenerator`, `EnumFieldGenerator`, and `MessageFieldGenerator` to support the bitfield variable naming convention required by the Builder's `buildPartial` method.
5.  **Javadoc Debug Strings:** Implemented `DocComment.writeDebugString` to reconstruct field definitions (e.g., `optional string foo = 1;`) in generated Javadoc.
6.  **Insertion Points:** Added missing `interface_extends` and `message_implements` insertion points.

### Remaining Issues
1.  **Javadoc Source Code Comments:**
    *   **Symptom:** The test fails expecting Javadoc comments (e.g., `<pre>` blocks containing proto comments) but getting only standard class documentation.
    *   **Cause:** `com.rubberjam.protobuf.another.compiler.java.DocComment.getLocation(GenericDescriptor)` is currently a stub returning `null`. It needs to be implemented to traverse the `FileDescriptor.toProto().getSourceCodeInfo()` and find the `Location` matching the descriptor's path.
    *   **Plan:** Implement path construction logic for `Descriptor` and `FieldDescriptor` to look up the correct `SourceCodeInfo.Location`.

2.  **Potential Formatting Mismatches:**
    *   Once comments are enabled, there may be slight differences in HTML escaping or whitespace handling between the C++ implementation and the Java port's `DocComment` utility that will need fine-tuning.

### Next Steps
*   Implement `DocComment.getLocation` to support comment extraction from `SourceCodeInfo`.
*   Re-run `ComprehensiveTestEdgeCasesMinimalParityTest`.
