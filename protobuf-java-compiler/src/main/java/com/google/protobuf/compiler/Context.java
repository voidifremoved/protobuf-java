package com.google.protobuf.compiler;

import com.google.protobuf.Descriptors.FileDescriptor;

class Context {
  private final FileDescriptor file;
  private final Options options;
  private final NameResolver nameResolver;

  Context(FileDescriptor file, Options options) {
    this.file = file;
    this.options = options;
    this.nameResolver = new NameResolver();
  }

  FileDescriptor getFile() {
    return file;
  }

  Options getOptions() {
    return options;
  }

  NameResolver getNameResolver() {
    return nameResolver;
  }
}
