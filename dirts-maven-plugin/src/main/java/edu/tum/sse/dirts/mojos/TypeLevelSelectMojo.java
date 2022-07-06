package edu.tum.sse.dirts.mojos;

import edu.tum.sse.dirts.core.control.Control;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "typeL_select")
@Execute(goal = "typeL_select", phase = LifecyclePhase.INITIALIZE, lifecycle = "dirts")
public class TypeLevelSelectMojo extends AbstractSelectMojo {

    @Override
    protected Control getControl() {
        return getTypeLevelControl(true);
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        doExecute(s -> s);
    }
}
