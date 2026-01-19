# AGENTS.md

This file contains instructions for agents working on the `protobuf-java-compiler` project.

## Code Standards

*   **Formatting**: Follow the existing code formatting standards observed in the codebase.
    *   Use Allman style braces (braces on their own lines).
    *   Maintain consistent indentation with existing files (e.g., `JavaCodeGenerator.java`).
*   **Library Usage**: Adhere to existing library usage patterns and dependencies.
*   **Package Structure**: Note that the package structure is `com.rubberjam.protobuf.compiler`. Stick to this existing package structure.

## Project Scope and Goals

*   **Multi-language Support**: Ensure that this Java-based Protobuf compiler can compile code for all languages supported by the standard Protobuf compiler (protoc).
    *   **Priority**: Start by implementing support for **C#** and **C++**.
    *   The ultimate goal is to match the language support of the standard compiler.
