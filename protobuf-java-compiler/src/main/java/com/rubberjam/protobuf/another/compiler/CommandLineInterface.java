package com.rubberjam.protobuf.another.compiler;

import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.rubberjam.protobuf.another.compiler.CodeGenerator.GenerationException;
import com.rubberjam.protobuf.another.compiler.Importer.DiskSourceTree;
import com.rubberjam.protobuf.another.compiler.Importer.MultiFileErrorCollector;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command-line interface for the protocol buffer compiler.
 * Mirrors google/protobuf/compiler/command_line_interface.cc
 */
public class CommandLineInterface {

    private final Map<String, CodeGenerator> generators = new HashMap<>();
    private final Map<String, String> plugins = new HashMap<>();
    private final List<String> protoPath = new ArrayList<>();
    private final List<String> inputFiles = new ArrayList<>();

    // Output directives: generator name -> output location
    // We store the raw flag value to handle options (e.g. "lite:outdir")
    private final List<Map.Entry<String, String>> outputDirectives = new ArrayList<>();

    public CommandLineInterface() {
    }

    public void registerGenerator(String flagName, CodeGenerator generator, String help) {
        generators.put(flagName, generator);
    }

    public void allowPlugins(String flagName) {
        // In C++, this enables --plugin executable lookup.
        // Here we can just store supported plugin flags if needed,
        // but typically --plugin=protoc-gen-NAME=PATH covers it.
    }

    public int run(String[] args) {
        if (args.length == 0) {
            printHelp();
            return 0;
        }

        // Check for help/version first
        for (String arg : args) {
            if (arg.equals("--help") || arg.equals("-h")) {
                printHelp();
                return 0;
            }
            if (arg.equals("--version")) {
                System.out.println("libprotoc " + Versions.PROTOBUF_JAVA_VERSION_STRING);
                return 0;
            }
        }

        if (!parseArguments(args)) {
            return 1;
        }

        if (inputFiles.isEmpty()) {
            System.err.println("Missing input file.");
            return 1;
        }

        if (outputDirectives.isEmpty()) {
            System.err.println("Missing output directive.");
            return 1;
        }

        DiskSourceTree sourceTree = new DiskSourceTree();
        // If no proto_path is specified, default to current directory.
        if (protoPath.isEmpty()) {
            sourceTree.mapPath("", ".");
        } else {
            for (String path : protoPath) {
                // Handle "virtual=physical" syntax
                int equals = path.indexOf('=');
                if (equals > 0) {
                    sourceTree.mapPath(path.substring(0, equals), path.substring(equals + 1));
                } else {
                    sourceTree.mapPath("", path);
                }
            }
        }

        ErrorCollector errorCollector = new ErrorCollector();
        Importer importer = new Importer(sourceTree, errorCollector);

        List<FileDescriptor> parsedFiles = new ArrayList<>();
        for (String inputFile : inputFiles) {
            FileDescriptor fd = importer.importFile(inputFile);
            if (fd == null) {
                return 1; // Error already reported
            }
            parsedFiles.add(fd);
        }

        for (Map.Entry<String, String> directive : outputDirectives) {
            String generatorName = directive.getKey();
            String outputLocation = directive.getValue();
            String parameter = "";

            // Check if outputLocation contains options: "options:path"
            // Note: This logic depends on implementation.
            // Standard protoc splits by colon if the generator supports it or generically?
            // Actually --java_out=lite:outdir passes "lite" as parameter.
            // But we need to separate the path.
            // A heuristic is usually to find the last colon? Or first?
            // C++ CLI splits at the *first* colon.
            // But on Windows, paths have colons (C:\...).
            // C++ CLI handles this by checking if the part before colon is a valid parameter?
            // Actually, C++ CLI logic:
            // if output_location starts with "options:", parse options.
            // But usually the syntax is --name_out=param1,param2:output_directory.
            // We assume the generator parameter is separated by the LAST colon,
            // unless it looks like a windows drive letter?

            // Simplified logic: split at first colon, unless it looks like a drive letter (1 char).
            // Better: C++ impl uses `ParseGeneratorParameter` logic implicitly?
            // No, the splitting happens here.

            // Re-reading C++:
            // It searches for the *first* colon.
            // If the output directory is needed, and we split early, we might mess up Windows paths?
            // Actually, usually output directory is the LAST part.
            // "param1,param2:C:\path\to\out"
            // So we should find the *first* colon?
            // "C:\path" -> "C" is param? No.

            // Let's implement a safe split.
            // If there is a colon, and the part before it is not a drive letter.
            // Or assume unix paths for now or use careful logic.

            int colon = outputLocation.indexOf(':');
            if (colon != -1) {
                 // Check for Windows drive letter: "C:\..."
                 // If colon is at index 1, and char at 0 is letter...
                 boolean isDriveLetter = (colon == 1 && Character.isLetter(outputLocation.charAt(0)));
                 if (!isDriveLetter) {
                     parameter = outputLocation.substring(0, colon);
                     outputLocation = outputLocation.substring(colon + 1);
                 }
            }

            if (!generateOutput(generatorName, parameter, outputLocation, parsedFiles)) {
                return 1;
            }
        }

        return 0;
    }

    private boolean parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-I")) {
                String path = arg.substring(2);
                if (path.isEmpty()) {
                    if (i + 1 < args.length) {
                        protoPath.add(args[++i]);
                    } else {
                        System.err.println("Missing value for flag: " + arg);
                        return false;
                    }
                } else {
                    protoPath.add(path);
                }
            } else if (arg.startsWith("--proto_path=")) {
                protoPath.add(arg.substring("--proto_path=".length()));
            } else if (arg.equals("--proto_path")) {
                 if (i + 1 < args.length) {
                    protoPath.add(args[++i]);
                } else {
                    System.err.println("Missing value for flag: " + arg);
                    return false;
                }
            } else if (arg.startsWith("--plugin=")) {
                String val = arg.substring("--plugin=".length());
                int equals = val.indexOf('=');
                if (equals != -1) {
                    plugins.put(val.substring(0, equals), val.substring(equals + 1));
                } else {
                    String name = new File(val).getName();
                    if (name.startsWith("protoc-gen-")) {
                         name = name.substring("protoc-gen-".length());
                    }
                    plugins.put(name, val);
                }
            } else if (arg.startsWith("--") && arg.endsWith("_out")) {
                // --java_out etc (space separated)
                if (i + 1 < args.length) {
                    String generatorName = arg.substring(2, arg.length() - 4);
                    String value = args[++i];
                    outputDirectives.add(new AbstractMap.SimpleImmutableEntry<>(generatorName, value));
                } else {
                    System.err.println("Missing value for flag: " + arg);
                    return false;
                }
            } else if (arg.startsWith("--") && arg.contains("_out=")) {
                 int equals = arg.indexOf('=');
                 String flag = arg.substring(2, equals);
                 if (flag.endsWith("_out")) {
                      String generatorName = flag.substring(0, flag.length() - 4);
                      String value = arg.substring(equals + 1);
                      outputDirectives.add(new AbstractMap.SimpleImmutableEntry<>(generatorName, value));
                 } else {
                     System.err.println("Unknown flag: " + arg);
                     return false;
                 }
            } else if (arg.startsWith("-")) {
                 System.err.println("Unknown flag: " + arg);
                 return false;
            } else {
                inputFiles.add(arg);
            }
        }
        return true;
    }

    private boolean generateOutput(String generatorName, String parameter, String outputLocation, List<FileDescriptor> files) {
        CodeGenerator generator = generators.get(generatorName);

        // If not built-in, try plugin
        if (generator == null) {
            String pluginPath = plugins.get(generatorName);
            if (pluginPath == null) {
                // Try to find on path?
                pluginPath = "protoc-gen-" + generatorName;
                // We rely on Subprocess to find it?
                // Subprocess has SEARCH_PATH mode.
            }
            return generatePluginOutput(pluginPath, parameter, outputLocation, files);
        }

        try {
            GeneratorContext context;
            if (outputLocation.endsWith(".zip") || outputLocation.endsWith(".jar")) {
                ZipWriter zipWriter = new ZipWriter(new FileOutputStream(outputLocation));
                context = new ZipGeneratorContext(zipWriter);
                generator.generateAll(files, parameter, context);
                zipWriter.writeDirectory(); // finish
            } else {
                context = new DirectoryGeneratorContext(new File(outputLocation));
                generator.generateAll(files, parameter, context);
            }
            return true;
        } catch (IOException | GenerationException e) {
            System.err.println("--" + generatorName + "_out: " + e.getMessage());
            return false;
        }
    }

    private boolean generatePluginOutput(String pluginPath, String parameter, String outputLocation, List<FileDescriptor> files) {
        // TODO: Implement plugin execution using Subprocess
        // For now, we only support built-in generators in this Phase.
        // Or if required, I can implement it.
        // Given Phase 3 Subprocess is done, I should probably use it.
        // But implementing full plugin protocol (CodeGeneratorRequest/Response) requires more code.
        System.err.println("Plugin support not fully implemented yet for: " + pluginPath);
        return false;
    }

    private void printHelp() {
        System.out.println("Usage: protoc [OPTION] PROTO_FILES");
        // Add more help text
    }

    public static void main(String[] args) {
        CommandLineInterface cli = new CommandLineInterface();
        // Register default generators if any (e.g. Java, C++)
        // cli.registerGenerator("java", new JavaGenerator(), "Generate Java source file.");
        System.exit(cli.run(args));
    }

    private static class ErrorCollector implements MultiFileErrorCollector {
        @Override
        public void recordError(String filename, int line, int column, String message) {
            System.err.println(filename + ":" + (line != -1 ? line + ":" : "") + (column != -1 ? column + ":" : "") + " " + message);
        }

        @Override
        public void recordWarning(String filename, int line, int column, String message) {
            System.err.println(filename + ":" + (line != -1 ? line + ":" : "") + (column != -1 ? column + ":" : "") + " warning: " + message);
        }
    }

    private static class DirectoryGeneratorContext implements GeneratorContext {
        private final File outputDir;

        DirectoryGeneratorContext(File outputDir) {
            this.outputDir = outputDir;
        }

        @Override
        public OutputStream open(String filename) throws IOException {
            File file = new File(outputDir, filename);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            return new FileOutputStream(file);
        }
    }

    private static class ZipGeneratorContext implements GeneratorContext {
        private final ZipWriter zipWriter;

        ZipGeneratorContext(ZipWriter zipWriter) {
            this.zipWriter = zipWriter;
        }

        @Override
        public OutputStream open(final String filename) throws IOException {
            return new ByteArrayOutputStream() {
                @Override
                public void close() throws IOException {
                    zipWriter.write(filename, this.toByteArray());
                    super.close();
                }
            };
        }
    }
}
