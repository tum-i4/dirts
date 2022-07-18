package edu.tum.sse.dirts.mojos;

import com.github.javaparser.ast.body.TypeDeclaration;
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

import static java.util.logging.Level.FINE;

@Mojo(name = "typeL_select")
@Execute(goal = "typeL_select", phase = LifecyclePhase.INITIALIZE, lifecycle = "dirts")
public class TypeLevelSelectMojo extends AbstractSelectMojo<TypeDeclaration<?>> {

    @Override
    protected Control<TypeDeclaration<?>> getControl() {
        Blackboard<TypeDeclaration<?>> typeLevelBlackboard = getTypeLevelBlackboard();
        typeLevelBlackboard.setTestFilter(getTestFilter());
        return new TypeLevelControl(typeLevelBlackboard, true);
    }

    @Override
    public void execute() {
        doExecute(s -> s);
    }
}
