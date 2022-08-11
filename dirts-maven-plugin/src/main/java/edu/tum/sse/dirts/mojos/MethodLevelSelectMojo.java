package edu.tum.sse.dirts.mojos;

import com.github.javaparser.ast.body.BodyDeclaration;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.core.control.MethodLevelControl;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.function.Function;

@Mojo(name = "method_level_select")
@Execute(goal = "method_level_select", phase = LifecyclePhase.INITIALIZE, lifecycle = "dirts")
public class MethodLevelSelectMojo extends AbstractSelectMojo<BodyDeclaration<?>> {

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
        Blackboard<BodyDeclaration<?>> methodLevelBlackboard = getMethodLevelBlackboard();
        methodLevelBlackboard.setTestFilter( getTestFilter());
        return new MethodLevelControl(methodLevelBlackboard, true);
    }

    @Override
    public void execute() {
        doExecute(toContainingClass);
    }
}

