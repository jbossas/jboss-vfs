package org.wildfly.quickstart.documentation.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
@Mojo(name = "documentation", threadSafe = true, defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class DocumentationMojo extends AbstractMojo {



    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        System.out.println("hello from the plugin");
    }
}
