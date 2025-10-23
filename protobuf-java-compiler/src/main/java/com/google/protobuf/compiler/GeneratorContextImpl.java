package com.google.protobuf.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class GeneratorContextImpl implements GeneratorContext {
  @Override
  public OutputStream open(String filename) throws FileNotFoundException {
    File file = new File(filename);
    file.getParentFile().mkdirs();
    return new FileOutputStream(file);
  }
}
