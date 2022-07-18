package edu.tum.sse.dirts.mojos;

import com.github.javaparser.ast.body.BodyDeclaration;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.core.control.NonTypeLevelControl;
import edu.tum.sse.dirts.util.Log;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.surefire.api.testset.TestFilter;

import java.util.function.Function;

import static java.util.logging.Level.FINE;

@Mojo(name = "nontypeL_select")
@Execute(goal = "nontypeL_select", phase = LifecyclePhase.INITIALIZE, lifecycle = "dirts")
public class NonTypeLevelSelectMojo extends AbstractSelectMojo<BodyDeclaration<?>> {

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
    protected Control<BodyDeclaration<?>> getControl() {
        Blackboard<BodyDeclaration<?>> nontypeLevelBlackboard = getNonTypeLevelBlackboard();
        nontypeLevelBlackboard.setTestFilter( getTestFilter());
        return new NonTypeLevelControl(nontypeLevelBlackboard, true);
    }

    @Override
    public void execute() {
        doExecute(toContainingClass);
    }
}

