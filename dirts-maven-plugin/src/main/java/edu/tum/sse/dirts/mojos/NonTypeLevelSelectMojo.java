package edu.tum.sse.dirts.mojos;

import edu.tum.sse.dirts.core.control.Control;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.function.Function;

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
        return getNonTypeLevelControl(true);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        doExecute(toContainingClass);
    }
}

