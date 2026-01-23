package com.rubberjam.protobuf.maven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.rubberjam.protobuf.maven.protoc.PlatformDetector;
import com.rubberjam.protobuf.maven.protoc.Protoc;
import com.rubberjam.protobuf.maven.protoc.ProtocVersion;

@Mojo(name = "protoc-maven")
public class ProtocMojo extends AbstractMojo
{
	private static final String DEFAULT_INPUT_DIR = "/src/main/protobuf/".replace('/', File.separatorChar);

	@Parameter(property = "protocVersion")
	private String protocVersion;

	@Parameter(property = "optimizeCodegen", defaultValue = "true")
	private boolean optimizeCodegen;

	@Parameter(property = "inputDirectories")
	private File[] inputDirectories;

	@Parameter(property = "includeDirectories")
	private File[] includeDirectories;

	@Parameter(property = "includeStdTypes", defaultValue = "false")
	private boolean includeStdTypes;

	@Parameter(property = "includeMavenTypes", defaultValue = "none")
	private String includeMavenTypes;

	@Parameter(property = "compileMavenTypes", defaultValue = "none")
	private String compileMavenTypes;

	@Parameter(property = "addProtoSources", defaultValue = "none")
	private String addProtoSources;

	@Parameter(property = "type", defaultValue = "java")
	private String type;

	@Parameter(property = "addSources", defaultValue = "main")
	private String addSources;

	@Parameter(property = "includeImports", defaultValue = "true")
	private boolean includeImports;

	@Parameter(property = "cleanOutputFolder", defaultValue = "false")
	private boolean cleanOutputFolder;

	@Parameter(property = "pluginPath")
	private String pluginPath;

	@Parameter(property = "pluginArtifact")
	private String pluginArtifact;

	@Parameter(property = "outputDirectory")
	private File outputDirectory;

	@Parameter(property = "outputDirectorySuffix")
	private String outputDirectorySuffix;

	@Parameter(property = "outputOptions")
	private String outputOptions;

	@Parameter(property = "outputTargets")
	private OutputTarget[] outputTargets;

	@Parameter(property = "extension", defaultValue = ".proto")
	private String extension;

	@Parameter(property = "protocCommand")
	private String protocCommand;

	@Parameter(property = "protocArtifact")
	private String protocArtifact;

	@Component
	private MavenProject project;

	@Component
	private MavenProjectHelper projectHelper;

	@Component
	private BuildContext buildContext;

	@Component
	private RepositorySystem repositorySystem;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	private RepositorySystemSession session;

	@Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
	private List<RemoteRepository> remoteRepositories;

	private File tempRoot = null;

	public void execute() throws MojoExecutionException
	{
		if ("pom".equalsIgnoreCase(project.getPackaging()))
		{
			getLog().info("Skipping 'pom' packaged project");
			return;
		}

		if (outputTargets == null || outputTargets.length == 0)
		{
			OutputTarget target = new OutputTarget();
			target.type = type;
			target.addSources = addSources;
			target.cleanOutputFolder = cleanOutputFolder;
			target.pluginPath = pluginPath;
			target.pluginArtifact = pluginArtifact;
			target.outputDirectory = outputDirectory;
			target.outputDirectorySuffix = outputDirectorySuffix;
			target.outputOptions = outputOptions;
			outputTargets = new OutputTarget[] { target };
		}

		boolean missingOutputDirectory = false;

		for (OutputTarget target : outputTargets)
		{
			target.addSources = target.addSources.toLowerCase().trim();
			if ("true".equals(target.addSources)) target.addSources = "main";

			if (target.outputDirectory == null)
			{
				String subdir = "generated-" + ("test".equals(target.addSources) ? "test-" : "") + "sources";
				target.outputDirectory = new File(project.getBuild().getDirectory() + File.separator + subdir + File.separator);
			}

			if (target.outputDirectorySuffix != null)
			{
				target.outputDirectory = new File(target.outputDirectory, target.outputDirectorySuffix);
			}

			String[] outputFiles = target.outputDirectory.list();
			if (outputFiles == null || outputFiles.length == 0)
			{
				missingOutputDirectory = true;
			}
		}

		if (!optimizeCodegen)
		{
			performProtoCompilation(true);
			return;
		}

		File successFile = new File(project.getBuild().getDirectory(), "pjmp-success.txt");
		try
		{
			long oldestOutputFileTime = minFileTime(outputTargets);
			long newestInputFileTime = maxFileTime(inputDirectories);
			if (successFile.exists() && newestInputFileTime < oldestOutputFileTime && !missingOutputDirectory)
			{
				getLog().info("Skipping code generation, proto files appear unchanged since last compilation");
				performProtoCompilation(false);
				return;
			}
			successFile.delete();
			performProtoCompilation(true);
			successFile.getParentFile().mkdirs();
			successFile.createNewFile();
		}
		catch (IOException e)
		{
			throw new MojoExecutionException("File operation failed: " + successFile, e);
		}
	}

	private void performProtoCompilation(boolean doCodegen) throws MojoExecutionException
	{
		if (doCodegen)
		{
			prepareProtoc();
		}

		File protoBuildDir = new File(project.getBuild().getDirectory(), "proto-sources");
		File mergedProtosDir = new File(protoBuildDir, "merged-protos");
		try
		{
			if (mergedProtosDir.exists())
			{
				FileUtils.cleanDirectory(mergedProtosDir);
			}
		}
		catch (IOException e)
		{
			throw new MojoExecutionException("Failed to clean merged-protos directory", e);
		}
		mergedProtosDir.mkdirs();

		File includeDir = new File(protoBuildDir, "includes");
		includeDir.mkdirs();

		// Get original project input directories
		File[] originalInputDirs = this.inputDirectories;
		if (originalInputDirs == null || originalInputDirs.length == 0)
		{
			File inputDir = new File(project.getBasedir().getAbsolutePath() + DEFAULT_INPUT_DIR);
			originalInputDirs = new File[] { inputDir };
		}

		// Store the names of the project's own proto files
		Set<String> projectProtoNames = new HashSet<>();

		// Copy local protos to the merged-protos directory
		for (File inputDir : originalInputDirs)
		{
			if (inputDir.exists() && inputDir.isDirectory())
			{
				getLog().info("Scanning for local protos in: " + inputDir);
				try
				{
					Collection<File> projectFiles = FileUtils.listFiles(inputDir, new FileFilter(extension),
							TrueFileFilter.INSTANCE);
					for (File projectFile : projectFiles)
					{
						projectProtoNames.add(projectFile.getName());
					}
					FileUtils.copyDirectory(inputDir, mergedProtosDir);
				}
				catch (IOException e)
				{
					throw new MojoExecutionException("Error copying local proto files", e);
				}
			}
		}

		// Extract dependency protos to the merged-protos directory
		getLog().info("Extracting protos from dependencies...");
		try
		{
			extractProtosFromDependencies(mergedProtosDir, true);
		}
		catch (IOException e)
		{
			throw new MojoExecutionException("Error extracting files from Maven dependencies", e);
		}

		// Extract standard types to the includes directory
		File stdTypesIncludeDir = null;
		if (includeStdTypes)
		{
			try
			{
				Protoc.extractStdTypes(ProtocVersion.getVersion("-v" + protocVersion), includeDir);
				// extractStdTypes creates includeDir/include/google/protobuf/, so we need to use includeDir/include as the include path
				stdTypesIncludeDir = new File(includeDir, "include");
				File googleProtobufDir = new File(stdTypesIncludeDir, "google/protobuf");
				
				// If extraction from JAR failed (directory doesn't exist or is empty), try extracting from protobuf-java dependency
				if (!stdTypesIncludeDir.exists() || !stdTypesIncludeDir.isDirectory() || 
				    !googleProtobufDir.exists() || !googleProtobufDir.isDirectory() ||
				    (googleProtobufDir.listFiles() == null || googleProtobufDir.listFiles().length == 0))
				{
					getLog().info("Standard types not found in JAR, extracting from protobuf-java dependency");
					extractStdTypesFromDependency(includeDir);
					stdTypesIncludeDir = new File(includeDir, "include");
					googleProtobufDir = new File(stdTypesIncludeDir, "google/protobuf");
					if (!stdTypesIncludeDir.exists() || !stdTypesIncludeDir.isDirectory() ||
					    !googleProtobufDir.exists() || googleProtobufDir.listFiles() == null || googleProtobufDir.listFiles().length == 0)
					{
						getLog().warn("Standard types directory not found or empty at " + googleProtobufDir + ", standard types may not be available");
						stdTypesIncludeDir = null;
					}
					else
					{
						getLog().info("Successfully extracted " + googleProtobufDir.listFiles().length + " standard type proto files");
					}
				}
			}
			catch (IOException e)
			{
				throw new MojoExecutionException("Error extracting standard types", e);
			}
		}

		// Process each output target
		for (OutputTarget target : outputTargets)
		{
			try
			{
				// Create a target-specific directory for this build, containing
				// all protos
				File targetBuildDir = new File(new File(protoBuildDir, "build"), target.type);
				if (targetBuildDir.exists())
				{
					FileUtils.cleanDirectory(targetBuildDir);
				}
				targetBuildDir.mkdirs();
				FileUtils.copyDirectory(mergedProtosDir, targetBuildDir);

				// Set the include paths for the compiler
				List<File> includeDirsList = new ArrayList<>();
				includeDirsList.add(targetBuildDir);
				if (stdTypesIncludeDir != null)
				{
					includeDirsList.add(stdTypesIncludeDir);
				}
				File[] currentIncludeDirs = includeDirsList.toArray(new File[0]);

				// Identify only the project's own proto files for compilation
				Collection<File> filesToCompile = new ArrayList<>();
				if (projectProtoNames.isEmpty())
				{
					getLog().debug("No project-specific proto files found to compile for target: " + target.type);
				}
				else
				{
					Collection<File> allFiles = FileUtils.listFiles(targetBuildDir, new FileFilter(extension),
							TrueFileFilter.INSTANCE);
					for (File f : allFiles)
					{
						if (projectProtoNames.contains(f.getName()))
						{
							filesToCompile.add(f);
						}
					}
				}

				if (doCodegen)
				{
					getLog().info("Processing target: " + target);
					preprocessTarget(target);

					boolean shaded = false;
					String targetType = target.type;
					if (targetType.equals("java-shaded") || targetType.equals("java_shaded"))
					{
						targetType = "java";
						shaded = true;
					}

					// Temporarily set include directories for buildCommand to
					// use
					File[] oldIncludeDirs = this.includeDirectories;
					this.includeDirectories = currentIncludeDirs;

					// Process only the project's own proto files
					for (File protoFile : filesToCompile)
					{
						processFile(protoFile, protocVersion, targetType, target.pluginPath, target.outputDirectory,
								target.outputOptions);
					}

					if (shaded)
					{
						try
						{
							getLog().info("    Shading (version " + protocVersion + "): " + target.outputDirectory);
							Protoc.doShading(target.outputDirectory, protocVersion);
						}
						catch (IOException e)
						{
							throw new MojoExecutionException("Error occurred during shading", e);
						}
					}

					// Restore old include directories
					this.includeDirectories = oldIncludeDirs;
				}
				addGeneratedSources(target);

			}
			catch (IOException e)
			{
				throw new MojoExecutionException("Error processing target " + target.type, e);
			}
		}
	}

	private void prepareProtoc() throws MojoExecutionException
	{
		if (protocCommand != null)
		{
			try
			{
				Protoc.runProtoc(protocCommand, new String[] { "--version" });
			}
			catch (Exception e)
			{
				protocCommand = null;
			}
		}

		if (protocCommand == null && protocArtifact == null)
		{
			if (isEmpty(protocVersion)) protocVersion = ProtocVersion.PROTOC_VERSION.version;
			getLog().info("Protoc version: " + protocVersion);

			try
			{
				if (protocCommand == null && protocArtifact == null)
				{
					File protocFile = Protoc.extractProtoc(ProtocVersion.getVersion("-v" + protocVersion), false);
					protocCommand = protocFile.getAbsolutePath();
					try
					{
						Protoc.runProtoc(protocCommand, new String[] { "--version" });
					}
					catch (Exception e)
					{
						tempRoot = new File(System.getProperty("user.home"));
						protocFile = Protoc.extractProtoc(ProtocVersion.getVersion("-v" + protocVersion), false, tempRoot);
						protocCommand = protocFile.getAbsolutePath();
					}
				}
			}
			catch (IOException e)
			{
				throw new MojoExecutionException("Error extracting protoc for version " + protocVersion, e);
			}
		}

		if (protocCommand == null && protocArtifact != null)
		{
			protocVersion = ProtocVersion.getVersion("-v:" + protocArtifact).version;
			protocCommand = resolveArtifact(protocArtifact, null).getAbsolutePath();
			try
			{
				Protoc.runProtoc(protocCommand, new String[] { "--version" });
			}
			catch (Exception e)
			{
				tempRoot = new File(System.getProperty("user.home"));
				protocCommand = resolveArtifact(protocArtifact, tempRoot).getAbsolutePath();
			}
		}
		getLog().info("Protoc command: " + protocCommand);
	}

	private void addIncludeDir(File dir)
	{
		includeDirectories = addDir(includeDirectories, dir);
	}

	private void addInputDir(File dir)
	{
		inputDirectories = addDir(inputDirectories, dir);
	}

	private boolean hasIncludeMavenTypes()
	{
		return includeMavenTypes.equalsIgnoreCase("direct") || includeMavenTypes.equalsIgnoreCase("transitive");
	}

	private boolean hasCompileMavenTypes()
	{
		return compileMavenTypes.equalsIgnoreCase("direct") || compileMavenTypes.equalsIgnoreCase("transitive");
	}

	private void extractProtosFromDependencies(File dir, boolean transitive) throws IOException
	{
		int extractedCount = 0;
		int skippedNullFile = 0;
		int scannedJars = 0;

		for (org.apache.maven.artifact.Artifact artifact : getArtifactsForProtoExtraction(transitive))
		{
			if (artifact.getFile() == null)
			{
				getLog().warn("  Skipping artifact with null file: " + artifact);
				skippedNullFile++;
				continue;
			}
			scannedJars++;
			getLog().debug("  Scanning artifact: " + artifact + " at " + artifact.getFile().getAbsolutePath());
			InputStream is = null;
			try
			{
				if (artifact.getFile().isDirectory())
				{
					for (File f : listFilesRecursively(artifact.getFile(), extension, new ArrayList<File>()))
					{
						// Extract proto files from 'protobuf' directory
						// (anywhere in the path)
						String path = f.getAbsolutePath().replace('\\', '/');
						if (!path.contains("/protobuf/"))
						{
							getLog().debug("    Skipping (not in protobuf dir): " + f.getName());
							continue;
						}
						is = new FileInputStream(f);

						// Use only the filename to place all protos in the same
						// root directory
						String filenameOnly = f.getName();
						writeProtoFile(dir, is, filenameOnly);
						is.close();
						extractedCount++;
					}
				}
				else
				{
					getLog().debug("  Scanning JAR: " + artifact.getFile().getName());
					ZipInputStream zis = new ZipInputStream(new FileInputStream(artifact.getFile()));
					is = zis;
					ZipEntry ze;
					int jarProtoCount = 0;
					while ((ze = zis.getNextEntry()) != null)
					{
						String entryName = ze.getName();

						// Extract proto files from 'protobuf' directory
						// (anywhere in the path)
						if (ze.isDirectory() ||
								!entryName.toLowerCase().endsWith(extension) ||
								!entryName.contains("protobuf/"))
						{
							continue;
						}

						// Use only the filename to place all protos in the same
						// root directory
						String filenameOnly = new File(entryName).getName();
						writeProtoFile(dir, zis, filenameOnly);
						zis.closeEntry();
						extractedCount++;
						jarProtoCount++;
					}
					if (jarProtoCount > 0)
					{
						getLog().info("  Found " + jarProtoCount + " proto files in " + artifact);
					}
				}
			}
			catch (IOException e)
			{
				getLog().error("  Error scanning artifact: " + artifact.getFile() + ": " + e);
			}
			finally
			{
				if (is != null) is.close();
			}
		}
		getLog().info("Extracted " + extractedCount + " proto files from dependencies");
		getLog().info("  Scanned " + scannedJars + " JARs, skipped " + skippedNullFile + " artifacts with null files");
	}

	private void extractStdTypesFromDependency(File includeDir) throws IOException
	{
		getLog().info("Attempting to extract standard types from protobuf-java dependency");
		// Look for protobuf-java dependency
		Set<org.apache.maven.artifact.Artifact> artifacts = getArtifactsForProtoExtraction(true);
		getLog().debug("Scanning " + artifacts.size() + " artifacts for protobuf-java");
		
		for (org.apache.maven.artifact.Artifact artifact : artifacts)
		{
			if (artifact.getFile() == null)
			{
				getLog().debug("  Skipping artifact with null file: " + artifact);
				continue;
			}
			
			getLog().debug("  Checking artifact: " + artifact.getGroupId() + ":" + artifact.getArtifactId());
			
			// Check if this is the protobuf-java artifact
			if (!"protobuf-java".equals(artifact.getArtifactId()) || 
			    !"com.google.protobuf".equals(artifact.getGroupId()))
			{
				continue;
			}
			
			getLog().info("Found protobuf-java dependency, extracting standard types from: " + artifact.getFile());
			File stdTypesDir = new File(includeDir, "include/google/protobuf");
			stdTypesDir.mkdirs();
			
			InputStream is = null;
			try
			{
				if (artifact.getFile().isDirectory())
				{
					// Extract from directory (for testing)
					for (File f : listFilesRecursively(artifact.getFile(), extension, new ArrayList<File>()))
					{
						String path = f.getAbsolutePath().replace('\\', '/');
						if (path.contains("/google/protobuf/") && path.endsWith(".proto"))
						{
							String relativePath = path.substring(path.indexOf("/google/protobuf/") + 1);
							File destFile = new File(includeDir, "include/" + relativePath);
							destFile.getParentFile().mkdirs();
							FileUtils.copyFile(f, destFile);
							getLog().debug("  Extracted: " + relativePath);
						}
					}
				}
				else
				{
					// Extract from JAR
					ZipInputStream zis = new ZipInputStream(new FileInputStream(artifact.getFile()));
					is = zis;
					ZipEntry ze;
					int extractedCount = 0;
					while ((ze = zis.getNextEntry()) != null)
					{
						String entryName = ze.getName();
						// Look for google/protobuf/*.proto files
						if (!ze.isDirectory() && 
						    entryName.startsWith("google/protobuf/") && 
						    entryName.endsWith(".proto"))
						{
							File destFile = new File(includeDir, "include/" + entryName);
							destFile.getParentFile().mkdirs();
							FileOutputStream fos = new FileOutputStream(destFile);
							streamCopy(zis, fos);
							fos.close();
							zis.closeEntry();
							extractedCount++;
							getLog().debug("  Extracted: " + entryName);
						}
						else
						{
							zis.closeEntry();
						}
					}
					getLog().info("Extracted " + extractedCount + " standard type proto files from protobuf-java");
				}
				return; // Found and extracted, we're done
			}
			catch (IOException e)
			{
				getLog().warn("Error extracting standard types from protobuf-java: " + e);
			}
			finally
			{
				if (is != null) is.close();
			}
		}
		getLog().warn("protobuf-java dependency not found, standard types may not be available");
	}

	private Set<org.apache.maven.artifact.Artifact> getArtifactsForProtoExtraction(boolean transitive)
	{
		Set<org.apache.maven.artifact.Artifact> artifacts = new HashSet<>();

		int projectArtifacts = 0;
		int dependencyArtifacts = 0;

		if (project.getArtifacts() != null)
		{
			projectArtifacts = project.getArtifacts().size();
			artifacts.addAll(project.getArtifacts());
			getLog().debug("  Added " + projectArtifacts + " from project.getArtifacts()");
		}
		if (project.getDependencyArtifacts() != null)
		{
			dependencyArtifacts = project.getDependencyArtifacts().size();
			artifacts.addAll(project.getDependencyArtifacts());
			getLog().debug("  Added " + dependencyArtifacts + " from project.getDependencyArtifacts()");
		}

		getLog().info("Found " + artifacts.size() + " dependency artifacts to scan for proto files");
		getLog().info(
				"  (" + projectArtifacts + " from resolved artifacts, " + dependencyArtifacts + " from dependency artifacts)");

		// Log which specific artifacts we're looking at
		if (getLog().isDebugEnabled())
		{
			for (org.apache.maven.artifact.Artifact artifact : artifacts)
			{
				getLog().debug("    Artifact: " + artifact + " -> file: " +
						(artifact.getFile() != null ? artifact.getFile().getAbsolutePath() : "NULL"));
			}
		}

		return artifacts;
	}

	private List<File> listFilesRecursively(File directory, String ext, List<File> list)
	{
		File[] files = directory.listFiles();
		if (files == null) return list;
		for (File f : files)
		{
			if (f.isFile() && f.canRead() && f.getName().toLowerCase().endsWith(ext))
				list.add(f);
			else if (f.isDirectory() && f.canExecute()) listFilesRecursively(f, ext, list);
		}
		return list;
	}

	private void writeProtoFile(File dir, InputStream zis, String name) throws IOException
	{
		getLog().info("    " + name);
		File protoOut = new File(dir, name);
		protoOut.getParentFile().mkdirs();
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(protoOut);
			streamCopy(zis, fos);
		}
		finally
		{
			if (fos != null) fos.close();
		}
	}

	private void preprocessTarget(OutputTarget target) throws MojoExecutionException
	{
		if (!isEmpty(target.pluginArtifact))
		{
			target.pluginPath = resolveArtifact(target.pluginArtifact, tempRoot).getAbsolutePath();
		}

		File f = target.outputDirectory;
		if (!f.exists())
		{
			getLog().info(f + " does not exist. Creating...");
			f.mkdirs();
		}

		if (target.cleanOutputFolder)
		{
			try
			{
				getLog().info("Cleaning " + f);
				FileUtils.cleanDirectory(f);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void processTarget(OutputTarget target) throws MojoExecutionException
	{
		boolean shaded = false;
		String targetType = target.type;
		if (targetType.equals("java-shaded") || targetType.equals("java_shaded"))
		{
			targetType = "java";
			shaded = true;
		}

		FileFilter fileFilter = new FileFilter(extension);
		for (File input : inputDirectories)
		{
			if (input == null) continue;

			if (input.exists() && input.isDirectory())
			{
				Collection<File> protoFiles = FileUtils.listFiles(input, fileFilter, TrueFileFilter.INSTANCE);
				for (File protoFile : protoFiles)
				{

					if (target.cleanOutputFolder || buildContext.hasDelta(protoFile.getPath()))
					{
						processFile(protoFile, protocVersion, targetType, target.pluginPath, target.outputDirectory,
								target.outputOptions);
					}
					else
					{
						getLog().info("Not changed " + protoFile);
					}
				}
			}
			else
			{
				if (input.exists())
					getLog().warn(input + " is not a directory");
				else getLog().warn(input + " does not exist");
			}
		}

		if (shaded)
		{
			try
			{
				getLog().info("    Shading (version " + protocVersion + "): " + target.outputDirectory);
				Protoc.doShading(target.outputDirectory, protocVersion);
			}
			catch (IOException e)
			{
				throw new MojoExecutionException("Error occurred during shading", e);
			}
		}
	}

	private void addGeneratedSources(OutputTarget target) throws MojoExecutionException
	{
		boolean mainAddSources = "main".endsWith(target.addSources);
		boolean testAddSources = "test".endsWith(target.addSources);

		if (mainAddSources)
		{
			getLog().info("Adding generated sources (" + target.type + "): " + target.outputDirectory);
			project.addCompileSourceRoot(target.outputDirectory.getAbsolutePath());
		}
		if (testAddSources)
		{
			getLog().info("Adding generated test sources (" + target.type + "): " + target.outputDirectory);
			project.addTestCompileSourceRoot(target.outputDirectory.getAbsolutePath());
		}
		if (mainAddSources || testAddSources)
		{
			buildContext.refresh(target.outputDirectory);
		}
	}

	private void processFile(File file, String version, String type, String pluginPath, File outputDir, String outputOptions)
			throws MojoExecutionException
	{
		getLog().info("    Processing (" + type + "): " + file.getName());

		try
		{
			buildContext.removeMessages(file);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ByteArrayOutputStream err = new ByteArrayOutputStream();
			TeeOutputStream outTee = new TeeOutputStream(System.out, out);
			TeeOutputStream errTee = new TeeOutputStream(System.err, err);

			int ret = 0;
			Collection<String> cmd = buildCommand(file, version, type, pluginPath, outputDir, outputOptions);
			if (protocCommand == null)
				ret = Protoc.runProtoc(cmd.toArray(new String[0]), outTee, errTee);
			else ret = Protoc.runProtoc(protocCommand, Arrays.asList(cmd.toArray(new String[0])), outTee, errTee);

			String errStr = err.toString();
			if (!isEmpty(errStr))
			{
				int severity = (ret != 0) ? BuildContext.SEVERITY_ERROR : BuildContext.SEVERITY_WARNING;
				String[] lines = errStr.split("\\n", -1);
				for (String line : lines)
				{
					int lineNum = 0;
					int colNum = 0;
					String msg = line;
					if (line.contains(file.getName()))
					{
						String[] parts = line.split(":", 4);
						if (parts.length == 4)
						{
							try
							{
								lineNum = Integer.parseInt(parts[1]);
								colNum = Integer.parseInt(parts[2]);
								msg = parts[3];
							}
							catch (Exception e)
							{
								getLog().warn("Failed to parse protoc warning/error for " + file);
							}
						}
					}
					buildContext.addMessage(file, lineNum, colNum, msg, severity, null);
				}
			}

			if (ret != 0) throw new MojoExecutionException("protoc-jar failed for " + file + ". Exit code " + ret);
		}
		catch (InterruptedException e)
		{
			throw new MojoExecutionException("Interrupted", e);
		}
		catch (IOException e)
		{
			throw new MojoExecutionException("Unable to execute protoc-jar for " + file, e);
		}
	}

	private Collection<String> buildCommand(File file, String version, String type, String pluginPath, File outputDir,
			String outputOptions) throws MojoExecutionException
	{
		Collection<String> cmd = new ArrayList<>();
		populateIncludes(cmd);
		cmd.add("-I" + file.getParentFile().getAbsolutePath());
		if ("descriptor".equals(type))
		{
			File outFile = new File(outputDir, file.getName());
			cmd.add("--descriptor_set_out=" + FilenameUtils.removeExtension(outFile.toString()) + ".desc");

			// Handle include_imports from either includeImports field or
			// outputOptions
			boolean hasIncludeImports = includeImports;
			if (outputOptions != null && outputOptions.contains("include_imports"))
			{
				hasIncludeImports = true;
			}

			if (hasIncludeImports)
			{
				cmd.add("--include_imports");
			}

			// Add other output options (excluding include_imports which we
			// handled above)
			if (outputOptions != null)
			{
				for (String arg : outputOptions.split("\\s+"))
				{
					if (!arg.isEmpty() && !arg.equals("include_imports") && !arg.equals("--include_imports"))
					{
						if (!arg.startsWith("-"))
						{
							cmd.add("--" + arg);
						}
						else
						{
							cmd.add(arg);
						}
					}
				}
			}
		}
		else
		{
			if (outputOptions != null)
			{
				cmd.add("--" + type + "_out=" + outputOptions + ":" + outputDir);
			}
			else
			{
				cmd.add("--" + type + "_out=" + outputDir);
			}

			if (pluginPath != null)
			{
				getLog().info("    Plugin path: " + pluginPath);
				cmd.add("--plugin=protoc-gen-" + type + "=" + pluginPath);
			}
		}
		cmd.add(file.toString());
		if (version != null) cmd.add("-v" + version);
		return cmd;
	}

	private void populateIncludes(Collection<String> args) throws MojoExecutionException
	{
		if (includeDirectories == null)
		{
			return;
		}
		for (File include : includeDirectories)
		{
			if (!include.exists()) throw new MojoExecutionException("Include path '" + include.getPath() + "' does not exist");
			if (!include.isDirectory())
				throw new MojoExecutionException("Include path '" + include.getPath() + "' is not a directory");
			args.add("-I" + include.getPath());
		}
	}

	private File resolveArtifact(String artifactSpec, File dir) throws MojoExecutionException
	{
		try
		{
			Properties detectorProps = new Properties();
			new PlatformDetector().detect(detectorProps, null);
			String platform = detectorProps.getProperty("os.detected.classifier");

			getLog().info("Resolving artifact: " + artifactSpec + ", platform: " + platform);
			String[] as = parseArtifactSpec(artifactSpec, platform);
			// Create the artifact
			Artifact artifact = new DefaultArtifact(as[0] + ":" + as[1] + ":" + as[3] + ":" + as[4] + ":" + as[2]);

			// Setup request for resolving the artifact
			ArtifactRequest request = new ArtifactRequest();
			request.setArtifact(artifact);
			request.setRepositories(remoteRepositories);

			ArtifactResult result = repositorySystem.resolveArtifact(session, request);
			File artifactFile = result.getArtifact().getFile(); // Use this to
																// get the file
																// location

			File tempFile = File.createTempFile(as[1], "." + as[3], dir);
			copyFile(artifactFile, tempFile);
			tempFile.setExecutable(true);
			tempFile.deleteOnExit();
			return tempFile;
		}
		catch (Exception e)
		{
			throw new MojoExecutionException("Error resolving artifact: " + artifactSpec, e);
		}
	}

	static String[] parseArtifactSpec(String artifactSpec, String platform)
	{
		String[] as = artifactSpec.split(":");
		String[] ret = Arrays.copyOf(as, 5);
		if (ret[3] == null) ret[3] = "exe";
		if (ret[4] == null) ret[4] = platform;
		return ret;
	}

	static long minFileTime(OutputTarget[] outputTargets)
	{
		if (outputTargets == null || outputTargets.length == 0) return Long.MAX_VALUE;
		long minTime = Long.MAX_VALUE;
		for (OutputTarget target : outputTargets)
		{
			if (target != null && target.outputDirectory != null)
			{
				minTime = Math.min(minTime, minFileTime(target.outputDirectory));
			}
		}
		return minTime;
	}

	static long maxFileTime(File[] dirs)
	{
		if (dirs == null || dirs.length == 0) return Long.MIN_VALUE;
		long maxTime = Long.MIN_VALUE;
		for (File dir : dirs)
		{
			if (dir != null && dir.exists())
			{
				maxTime = Math.max(maxTime, maxFileTime(dir));
			}
		}
		return maxTime;
	}

	static long minFileTime(File current)
	{
		if (current == null || !current.exists()) return Long.MAX_VALUE;
		if (!current.isDirectory()) return current.lastModified();
		File[] files = current.listFiles();
		if (files == null) return current.lastModified();
		long minTime = Long.MAX_VALUE;
		for (File entry : files)
			minTime = Math.min(minTime, minFileTime(entry));
		return minTime;
	}

	static long maxFileTime(File current)
	{
		if (current == null || !current.exists()) return Long.MIN_VALUE;
		if (!current.isDirectory()) return current.lastModified();
		File[] files = current.listFiles();
		if (files == null) return current.lastModified();
		long maxTime = Long.MIN_VALUE;
		for (File entry : files)
			maxTime = Math.max(maxTime, maxFileTime(entry));
		return maxTime;
	}

	static File createTempDir(String name) throws MojoExecutionException
	{
		try
		{
			File tmpDir = File.createTempFile(name, "");
			tmpDir.delete();
			tmpDir.mkdirs();
			tmpDir.deleteOnExit();
			return tmpDir;
		}
		catch (IOException e)
		{
			throw new MojoExecutionException("Error creating temporary directory: " + name, e);
		}
	}

	static File[] addDir(File[] dirs, File dir)
	{
		if (dirs == null)
		{
			dirs = new File[] { dir };
		}
		else
		{
			dirs = Arrays.copyOf(dirs, dirs.length + 1);
			dirs[dirs.length - 1] = dir;
		}
		return dirs;
	}

	static void deleteOnExitRecursive(File dir)
	{
		dir.deleteOnExit();
		File[] files = dir.listFiles();
		if (files == null) return;
		for (File f : files)
		{
			f.deleteOnExit();
			if (f.isDirectory()) deleteOnExitRecursive(f);
		}
	}

	static File copyFile(File srcFile, File destFile) throws IOException
	{
		FileInputStream is = null;
		FileOutputStream os = null;
		try
		{
			is = new FileInputStream(srcFile);
			os = new FileOutputStream(destFile);
			streamCopy(is, os);
		}
		finally
		{
			if (is != null) is.close();
			if (os != null) os.close();
		}
		return destFile;
	}

	static void streamCopy(InputStream in, OutputStream out) throws IOException
	{
		int read = 0;
		byte[] buf = new byte[4096];
		while ((read = in.read(buf)) > 0)
			out.write(buf, 0, read);
	}

	static boolean isEmpty(String s)
	{
		return s == null || s.length() == 0;
	}

	static class FileFilter implements IOFileFilter
	{
		String extension;

		public FileFilter(String extension)
		{
			this.extension = extension;
		}

		public boolean accept(File dir, String name)
		{
			return name.endsWith(extension);
		}

		public boolean accept(File file)
		{
			return file.getName().endsWith(extension);
		}
	}
}
