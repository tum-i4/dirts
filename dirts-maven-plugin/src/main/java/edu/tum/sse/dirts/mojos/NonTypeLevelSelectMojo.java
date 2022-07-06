package edu.tum.sse.dirts.mojos;

import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.core.control.TypeLevelControl;
import edu.tum.sse.dirts.util.Log;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.surefire.api.testset.TestFilter;

import java.util.function.Function;

import static java.util.logging.Level.FINE;

@Mojo(name = "nontypeL_select")
@Execute(goal = "nontypeL_select", phase = LifecyclePhase.INITIALIZE, lifecycle = "dirts")
public class NonTypeLevelSelectMojo extends AbstractSelectMojo {

    private final Function<String, String> toContainingClass = t -> {
        String ret = t;
        if (ret.contains("(")) {
            ret = ret.substring(0, ret.indexOf("("));
        }
        if (ret.contains(".")) {
            ret = ret.substring(0, ret.lastIndexOf("."));
        }
        return ret;
    };


    @Override
    protected Control getControl() {
        TestFilter<String, String> testFilter = getTestFilter();
        if (testFilter != null)
            Log.log(FINE, "Using test filter: " + testFilter);

        Blackboard typeLevelBlackboard = getTypeLevelBlackboard();
        typeLevelBlackboard.setTestFilter(testFilter);
        return new TypeLevelControl(typeLevelBlackboard, true);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        doExecute(toContainingClass);
    }
}

