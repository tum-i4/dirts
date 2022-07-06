package edu.tum.sse.dirts.mojos;

import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.core.control.NonTypeLevelControl;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "nontypeL_graph")
@Execute(goal = "nontypeL_graph", phase = LifecyclePhase.INITIALIZE, lifecycle = "dirts")
public class NonTypeLevelGraphMojo extends AbstractGraphMojo {

    private static final String OUTPUT_FILE = "nontypeL";

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
        Blackboard nontypeLevelBlackboard = getNonTypeLevelBlackboard();
        return new NonTypeLevelControl(nontypeLevelBlackboard, false);
    }
}
