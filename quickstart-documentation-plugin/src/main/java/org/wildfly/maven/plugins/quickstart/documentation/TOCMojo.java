package org.wildfly.maven.plugins.quickstart.documentation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
@Mojo(name = "table-of-contents", threadSafe = true, defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class TOCMojo extends AbstractMojo {


    /**
     * The project
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * The target directory the application to be deployed is located.
     */
    @Parameter(defaultValue = "${project.basedir}", required = true, property = "rootDirectory")
    private File rootDirectory;

    @Parameter(defaultValue = "[TOC-quickstart]", required = true)
    protected String replaceMarker;

    @Parameter(defaultValue = "target/docs/README.md", required = true)
    protected String targetDocument;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        TOCGenerator generator = new TOCGenerator(Arrays.asList("target", "dist", "template", "guide"));
        Path root = rootDirectory.toPath();
        getLog().info("root directory: " + root);
        try {
            generator.generate(root, replaceMarker, Paths.get(targetDocument));
        } catch (IOException e) {
            throw new MojoFailureException("Could not generate TOC", e);
        }
    }
}
