# Protobuf Java Compiler

A pure Java implementation of the Protocol Buffers compiler (`protoc`).

## Overview

This project aims to provide a dependency-free, pure Java version of the Protocol Buffers compiler. It is designed to be a faithful port of the original C++ `protoc` implementation, maintaining the same internal structure and logic to ensure **bit-wise parity** in generated code.

This allows Java developers to integrate Protocol Buffers compilation directly into their build process without requiring a local installation of the C++ `protoc` binary.

## Project Structure

The project is organized into three main modules:

*   **`protobuf-java-compiler`**: The core compiler implementation. This code is a direct port of the C++ source files found in the [google/protobuf](https://github.com/protocolbuffers/protobuf) repository. It targets the package `com.rubberjam.protobuf.another.compiler`.
*   **`protobuf-maven-plugin`**: A Maven plugin that wraps the `protobuf-java-compiler`, allowing users to compile `.proto` files as part of their Maven build lifecycle.
*   **`protoc-maven-plugin`**: A utility plugin that wraps the native `protoc` executable. This is primarily used within this project's build process to generate "gold master" baselines for testing and verification purposes.

## Status

The project is currently a work in progress. The porting process follows a strict file-by-file conversion strategy to ensure accuracy and maintainability.

Please refer to [CONVERSION_ORDER.md](CONVERSION_ORDER.md) for a detailed list of converted files and the current status of the project.

## Usage

To use the pure Java compiler in your Maven project, add the `protobuf-maven-plugin` to your `pom.xml`.

```xml
<plugin>
    <groupId>com.rubberjam.protobuf</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <!-- Optional: Defaults to src/main/proto -->
        <sourceDirectory>src/main/proto</sourceDirectory>
        <!-- Optional: Defaults to target/generated-sources/protobuf -->
        <outputDirectory>target/generated-sources/protobuf</outputDirectory>
    </configuration>
</plugin>
```

The plugin attaches to the `generate-sources` phase by default.

## Development

For developers contributing to this project:
*   Follow the [CONVERSION_ORDER.md](CONVERSION_ORDER.md) to pick up tasks.
*   See [PROTOC.md](PROTOC.md) for detailed notes on the internal architecture and porting guidelines.
