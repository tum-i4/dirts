package edu.tum.sse.dirts.mojos;

import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.core.control.NonTypeLevelControl;
import edu.tum.sse.dirts.util.Log;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.surefire.api.testset.TestFilter;

import static java.util.logging.Level.FINE;

@Mojo(name = "typeL_select")
@Execute(goal = "typeL_select", phase = LifecyclePhase.INITIALIZE, lifecycle = "dirts")
public class TypeLevelSelectMojo extends AbstractSelectMojo {

    @Override
    protected Control getControl() {
        TestFilter<String, String> testFilter = getTestFilter();
        if (testFilter != null)
            Log.log(FINE, "Using test filter: " + testFilter);

        Blackboard nontypeLevelBlackboard = getNonTypeLevelBlackboard();
        nontypeLevelBlackboard.setTestFilter(testFilter);
        return new NonTypeLevelControl(nontypeLevelBlackboard, true);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        doExecute(s -> s);
    }
}
