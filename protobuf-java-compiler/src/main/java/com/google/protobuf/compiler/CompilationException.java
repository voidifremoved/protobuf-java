package com.google.protobuf.compiler;

public class CompilationException extends Exception {
  public CompilationException(String message) {
    super(message);
  }

  public CompilationException(String message, Throwable cause) {
    super(message, cause);
  }
}
