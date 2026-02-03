package com.rubberjam.protobuf.compiler;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.io.Tokenizer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Importer {

    public interface MultiFileErrorCollector {
        void recordError(String filename, int line, int column, String message);
        void recordWarning(String filename, int line, int column, String message);
    }

    public interface SourceTree {
        /**
         * Open the given file and return a stream that reads it, or NULL if not
         * found. The caller takes ownership of the returned object. The filename
         * must be a path relative to the root of the source tree and must not
         * contain "." or ".." components.
         */
        InputStream open(String filename);

        String getLastErrorMessage();
    }

    public static class DiskSourceTree implements SourceTree {
        private final List<Mapping> mappings = new ArrayList<>();
        private String lastErrorMessage = "";

        private static class Mapping {
            String virtualPath;
            String diskPath;

            Mapping(String virtualPath, String diskPath) {
                this.virtualPath = virtualPath;
                this.diskPath = diskPath;
            }
        }

        public void mapPath(String virtualPath, String diskPath) {
            mappings.add(new Mapping(virtualPath, diskPath));
        }

        @Override
        public InputStream open(String filename) {
            if (filename.contains("..")) {
                lastErrorMessage = "Invalid filename: " + filename;
                return null;
            }

            for (Mapping mapping : mappings) {
                // If virtualPath is empty, it maps to root.
                // Otherwise check if filename starts with virtualPath.
                String diskPath = null;
                if (mapping.virtualPath.isEmpty()) {
                    diskPath = mapping.diskPath + File.separator + filename;
                } else if (filename.startsWith(mapping.virtualPath + "/")) {
                    String suffix = filename.substring(mapping.virtualPath.length() + 1);
                    diskPath = mapping.diskPath + File.separator + suffix;
                }

                if (diskPath != null) {
                    File file = new File(diskPath);
                    if (file.exists() && file.isFile()) {
                        try {
                            return new FileInputStream(file);
                        } catch (IOException e) {
                            lastErrorMessage = "Error opening file: " + diskPath + " (" + e.getMessage() + ")";
                            return null;
                        }
                    }
                }
            }
            lastErrorMessage = "File not found: " + filename;
            return null;
        }

        @Override
        public String getLastErrorMessage() {
            return lastErrorMessage;
        }
    }

    private final SourceTree sourceTree;
    private final MultiFileErrorCollector errorCollector;
    private final Map<String, FileDescriptor> descriptorCache = new HashMap<>();
    private final Set<String> currentlyLoading = new HashSet<>();

    public Importer(SourceTree sourceTree, MultiFileErrorCollector errorCollector) {
        this.sourceTree = sourceTree;
        this.errorCollector = errorCollector;
    }

    public FileDescriptor importFile(String filename) {
        if (descriptorCache.containsKey(filename)) {
            return descriptorCache.get(filename);
        }

        if (currentlyLoading.contains(filename)) {
            if (errorCollector != null) {
                errorCollector.recordError(filename, -1, -1, "File recursively imports itself: " + filename);
            }
            return null;
        }

        InputStream stream = sourceTree.open(filename);
        if (stream == null) {
            if (errorCollector != null) {
                errorCollector.recordError(filename, -1, -1, "File not found: " + filename);
            }
            return null;
        }

        currentlyLoading.add(filename);
        try {
            SingleFileErrorCollector simpleCollector = new SingleFileErrorCollector(filename, errorCollector);
            Tokenizer tokenizer = new Tokenizer(new InputStreamReader(stream, StandardCharsets.UTF_8), simpleCollector);
            Parser parser = new Parser();
            FileDescriptorProto.Builder protoBuilder = FileDescriptorProto.newBuilder();

            // Set name to filename as expected by DescriptorPool
            protoBuilder.setName(filename);

            if (!parser.parse(tokenizer, protoBuilder)) {
                // Parser already reported errors
                return null;
            }

            FileDescriptorProto proto = protoBuilder.build();
            List<FileDescriptor> dependencies = new ArrayList<>();
            for (String dependencyName : proto.getDependencyList()) {
                FileDescriptor dependency = importFile(dependencyName);
                if (dependency == null) {
                    // Error already reported
                     // We might still want to continue to report more errors?
                     // But typically buildFrom requires valid dependencies.
                     // C++ returns a placeholder? Java throws.
                     // We abort here.
                     return null;
                }
                dependencies.add(dependency);
            }

            // Handle public dependencies?
            // FileDescriptor.buildFrom handles them if passed correctly.

            try {
                FileDescriptor result = FileDescriptor.buildFrom(proto, dependencies.toArray(new FileDescriptor[0]));
                descriptorCache.put(filename, result);
                return result;
            } catch (DescriptorValidationException e) {
                if (errorCollector != null) {
                    errorCollector.recordError(filename, -1, -1, "Validation failed: " + e.getMessage());
                }
                return null;
            }

        } finally {
            currentlyLoading.remove(filename);
            try {
                stream.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private static class SingleFileErrorCollector implements ErrorCollector {
        private final String filename;
        private final MultiFileErrorCollector delegate;

        SingleFileErrorCollector(String filename, MultiFileErrorCollector delegate) {
            this.filename = filename;
            this.delegate = delegate;
        }

        @Override
        public void recordError(int line, int column, String message) {
            if (delegate != null) {
                delegate.recordError(filename, line, column, message);
            }
        }
    }
}
