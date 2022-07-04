package edu.tum.sse.dirts.mojos;

import edu.tum.sse.dirts.core.control.Control;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mojo(name = "nontypeL_graph")
@Execute(goal = "nontypeL_graph", phase = LifecyclePhase.INITIALIZE, lifecycle = "dirts")
public class NonTypeLevelGraphMojo extends AbstractDirtsMojo {

    private static final String OUTPUT_FILE = "nontypeL";

    @Parameter(property = "toFile", defaultValue = "false")
    protected boolean toFile;

    @Parameter(property = "outputFile", defaultValue = OUTPUT_FILE)
    protected String outputFile;

    @Override
    public void execute() throws MojoExecutionException {
        if (getProject().getPackaging().equals("pom")) {
            System.out.println("There are no tests that could be selected, " +
                    "since this project has packaging \"pom\".");
            try {
                Files.delete(getSubPath().resolve(".dirts_dependencies"));
            } catch (IOException e) {
                System.err.println("Failed to delete file containing dependencies: " + e.getMessage());
            }
            return;
        }

        Control control = super.getNonTypeLevelControl(false);

        String fileName = outputFile;
        if (fileName.equals(OUTPUT_FILE))
            outputFile += "_" + getProject().getArtifactId();
        outputFile += ".dot";

        if (toFile) {
            try {
                Path outputPath = Path.of(outputFile);
                Files.writeString(outputPath, control.visualizeDependencyGraph());
            } catch (IOException ignored) {
                throw new MojoExecutionException("Unable to write dependency graph to file.\n");
            }
        } else {
            System.out.println(control.visualizeDependencyGraph());
        }
    }
}
