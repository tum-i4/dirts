package edu.tum.sse.dirts.mojos;

import edu.tum.sse.dirts.core.control.Control;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "typeL_graph")
@Execute(goal = "typeL_graph", phase = LifecyclePhase.INITIALIZE, lifecycle = "dirts")
public class TypeLevelGraphMojo extends AbstractGraphMojo {

    private static final String OUTPUT_FILE = "typeL";

    @Parameter(property = "outputFile", defaultValue = OUTPUT_FILE)
    protected String outputFile;

    @Override
    protected String getOutputFilePath() {
        return outputFile;
    }

    @Override
    protected String getDefaultOutputFilePath() {
        return OUTPUT_FILE;
    }

    @Override
    protected Control getControl() {
        return getTypeLevelControl(false);
    }
}