package com.rubberjam.protobuf.maven;

import com.rubberjam.protobuf.compiler.Compiler;
import com.rubberjam.protobuf.compiler.CompilationException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mojo(
		name = "compile",
		defaultPhase = LifecyclePhase.GENERATE_SOURCES,
		requiresDependencyResolution = ResolutionScope.COMPILE,
		threadSafe = true)
public class ProtobufCompileMojo extends AbstractMojo
{

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	@Parameter(defaultValue = "${project.basedir}/src/main/proto")
	private File sourceDirectory;

	@Parameter
	private File[] additionalSourceDirectories;

	@Parameter(defaultValue = "${project.build.directory}/generated-sources/protobuf")
	private File outputDirectory;

	@Parameter
	private String[] includes;

	@Parameter
	private String[] excludes;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		if (!outputDirectory.exists())
		{
			outputDirectory.mkdirs();
		}
		project.addCompileSourceRoot(outputDirectory.getAbsolutePath());

		List<File> sourceDirs = new ArrayList<>();
		if (sourceDirectory.exists())
		{
			sourceDirs.add(sourceDirectory);
		}
		if (additionalSourceDirectories != null)
		{
			for (File dir : additionalSourceDirectories)
			{
				if (dir.exists())
				{
					sourceDirs.add(dir);
				}
			}
		}

		if (sourceDirs.isEmpty())
		{
			getLog().info("No proto source directories found.");
			return;
		}

		Map<String, String> protoFiles = new HashMap<>();
		for (File dir : sourceDirs)
		{
			DirectoryScanner scanner = new DirectoryScanner();
			scanner.setBasedir(dir);
			if (includes != null && includes.length > 0)
			{
				scanner.setIncludes(includes);
			}
			else
			{
				scanner.setIncludes(new String[] { "**/*.proto" });
			}
			if (excludes != null)
			{
				scanner.setExcludes(excludes);
			}
			scanner.scan();
			for (String includedFile : scanner.getIncludedFiles())
			{
				try
				{
					File f = new File(dir, includedFile);
					String content = readFile(f);
					// Use relative path as key (Linux style separators)
					protoFiles.put(includedFile.replace(File.separatorChar, '/'), content);
				}
				catch (IOException e)
				{
					throw new MojoExecutionException("Error reading proto file: " + includedFile, e);
				}
			}
		}

		if (protoFiles.isEmpty())
		{
			getLog().info("No proto files found.");
			return;
		}

		ClassLoader projectClassLoader = getProjectClassLoader();

		Compiler.ProtoImportResolver resolver = new Compiler.ProtoImportResolver()
		{
			@Override
			public String resolve(String path)
			{
				// 1. Check source directories
				for (File dir : sourceDirs)
				{
					File f = new File(dir, path);
					if (f.exists())
					{
						try
						{
							return readFile(f);
						}
						catch (IOException e)
						{
							getLog().warn("Failed to read dependency from file: " + f, e);
						}
					}
				}

				// 2. Check classpath
				try (InputStream is = projectClassLoader.getResourceAsStream(path))
				{
					if (is != null)
					{
						return IOUtil.toString(is, StandardCharsets.UTF_8.name());
					}
				}
				catch (IOException e)
				{
					getLog().warn("Failed to read dependency from classpath: " + path, e);
				}

				return null;
			}
		};

		Compiler compiler = new Compiler();
		try
		{
			Map<String, String> generatedFiles = compiler.compile(protoFiles, Collections.singletonList("java"), resolver);

			for (Map.Entry<String, String> entry : generatedFiles.entrySet())
			{
				File outputFile = new File(outputDirectory, entry.getKey());
				outputFile.getParentFile().mkdirs();
				try (FileOutputStream fos = new FileOutputStream(outputFile))
				{
					fos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
				}
			}
			getLog().info("Generated " + generatedFiles.size() + " Java files.");

		}
		catch (CompilationException | IOException e)
		{
			throw new MojoFailureException("Protobuf compilation failed", e);
		}
	}

	private String readFile(File f) throws IOException
	{
		try (FileInputStream fis = new FileInputStream(f))
		{
			return IOUtil.toString(fis, StandardCharsets.UTF_8.name());
		}
	}

	private ClassLoader getProjectClassLoader() throws MojoExecutionException
	{
		try
		{
			List<String> classpathElements = project.getCompileClasspathElements();
			URL[] urls = new URL[classpathElements.size()];
			for (int i = 0; i < classpathElements.size(); ++i)
			{
				urls[i] = new File(classpathElements.get(i)).toURI().toURL();
			}
			return new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
		}
		catch (Exception e)
		{
			throw new MojoExecutionException("Couldn't create project classloader", e);
		}
	}
}
