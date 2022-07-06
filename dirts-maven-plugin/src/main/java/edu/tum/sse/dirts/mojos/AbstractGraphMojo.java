package edu.tum.sse.dirts.mojos;

import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.util.Log;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.logging.Level.INFO;

public abstract class AbstractGraphMojo extends AbstractDirtsMojo{

    @Parameter(property = "toFile", defaultValue = "false")
    protected boolean toFile;

    protected abstract String getOutputFilePath();
    protected abstract String getDefaultOutputFilePath();

    @Override
    public void execute() throws MojoExecutionException {
        if (getProject().getPackaging().equals("pom")) {
            Log.log(INFO, "There are no tests that could be selected, " +
                    "since this project has packaging \"pom\".");
            try {
                Files.delete(getSubPath().resolve(".dirts_dependencies"));
            } catch (IOException e) {
                System.err.println("Failed to delete file containing dependencies: " + e.getMessage());
            }
            return;
        }

        Control control = getControl();

        String fileName = getOutputFilePath();
        if (fileName.equals(getDefaultOutputFilePath()))
            fileName += "_" + getProject().getArtifactId();
        fileName += ".dot";

        if (toFile) {
            try {
                Path outputPath = Path.of(fileName);
                Files.writeString(outputPath, control.visualizeDependencyGraph());
            } catch (IOException ignored) {
                throw new MojoExecutionException("Unable to write dependency graph to file.\n");
            }
        } else {
            System.out.println(control.visualizeDependencyGraph());
        }
    }

}
