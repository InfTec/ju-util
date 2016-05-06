package ch.inftec.ju.maven;

import ch.inftec.ju.devops.FilesystemCleanup;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Created by rotscher on 2/27/14.
 */

@Mojo(name="filesystem-cleanup")
public class FilesystemCleanupMojo extends AbstractMojo {

    @Parameter( alias = "directory",
            property = "directory",
            required = true )
    private File directory;

    @Parameter( alias = "dryRun",
            property = "dryRun",
            defaultValue = "true",
            required = false )
    private boolean dryRun = true;

    @Parameter( alias = "olderThan",
            property = "olderThan",
            required = false )
    private int olderThan = FilesystemCleanup.OLDER_THAN;

    @Parameter( alias = "keepMaxFiles",
            property = "keepMax",
            required = false )
    private int keepMaxFiles = FilesystemCleanup.MAX_RELEASES;

    @Parameter( alias = "keepMinFiles",
            property = "keepMin",
            required = false )
    private int keepMinFiles = FilesystemCleanup.MIN_RELEASES;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        FilesystemCleanup cleanup = new FilesystemCleanup(dryRun);
        getLog().info(String.format("start cleaning from %s", directory.getPath()));
        int result = cleanup.cleanup(directory, olderThan, keepMaxFiles, keepMinFiles);
        getLog().info(String.format("cleaned %d directories from %s", result, directory.getPath()));
    }
}
