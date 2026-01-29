package com.rubberjam.protobuf.another.compiler;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A table mapping (descriptor, ErrorLocation) pairs -- as reported by DescriptorPool when validating
 * descriptors -- to line and column numbers within the original source code.
 */
public final class SourceLocationTable {

  // Using a custom key class because Pair is not standard Java
  private static final class LocationKey {
    final Message descriptor;
    final ErrorLocation location;

    LocationKey(Message descriptor, ErrorLocation location) {
      this.descriptor = descriptor;
      this.location = location;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      LocationKey that = (LocationKey) o;
      return Objects.equals(descriptor, that.descriptor) && location == that.location;
    }

    @Override
    public int hashCode() {
      return Objects.hash(descriptor, location);
    }
  }

  private static final class ImportKey {
    final Message descriptor;
    final String name;

    ImportKey(Message descriptor, String name) {
      this.descriptor = descriptor;
      this.name = name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ImportKey importKey = (ImportKey) o;
      return Objects.equals(descriptor, importKey.descriptor) && Objects.equals(name, importKey.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(descriptor, name);
    }
  }

  private static final class Coordinates {
    final int line;
    final int column;

    Coordinates(int line, int column) {
      this.line = line;
      this.column = column;
    }
  }

  // Enum mirroring DescriptorPool::ErrorCollector::ErrorLocation
  // Since Java's DescriptorPool doesn't expose ErrorLocation directly in the same way C++ does (it uses callbacks with string paths usually),
  // we might need to define our own or see if we can use something close.
  // C++ defines: NAME, NUMBER, TYPE, EXTENDEE, DEFAULT_VALUE, OPTION_NAME, OPTION_VALUE, INPUT_TYPE, OUTPUT_TYPE, IMPORT, OTHER

  public enum ErrorLocation {
    NAME,
    NUMBER,
    TYPE,
    EXTENDEE,
    DEFAULT_VALUE,
    OPTION_NAME,
    OPTION_VALUE,
    INPUT_TYPE,
    OUTPUT_TYPE,
    IMPORT,
    OTHER
  }

  private final Map<LocationKey, Coordinates> locationMap = new HashMap<>();
  private final Map<ImportKey, Coordinates> importLocationMap = new HashMap<>();

  public SourceLocationTable() {}

  /**
   * Finds the precise location of the given error and returns coordinates.
   * Returns null if not found.
   */
  public Coordinates find(Message descriptor, ErrorLocation location) {
    return locationMap.get(new LocationKey(descriptor, location));
  }

  public Coordinates findImport(Message descriptor, String name) {
    return importLocationMap.get(new ImportKey(descriptor, name));
  }

  public void add(Message descriptor, ErrorLocation location, int line, int column) {
    locationMap.put(new LocationKey(descriptor, location), new Coordinates(line, column));
  }

  public void addImport(Message descriptor, String name, int line, int column) {
    importLocationMap.put(new ImportKey(descriptor, name), new Coordinates(line, column));
  }

  public void clear() {
    locationMap.clear();
    importLocationMap.clear();
  }
}
