package edu.tum.sse.dirts.mojos;

import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.core.control.ClassLevelControl;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "class_level_select")
@Execute(goal = "class_level_select", phase = LifecyclePhase.INITIALIZE, lifecycle = "dirts")
public class ClassLevelSelectMojo extends AbstractSelectMojo<TypeDeclaration<?>> {

    @Override
    protected Control<TypeDeclaration<?>> getControl() {
        Blackboard<TypeDeclaration<?>> typeLevelBlackboard = getClassLevelBlackboard();
        typeLevelBlackboard.setTestFilter(getTestFilter());
        return new ClassLevelControl(typeLevelBlackboard, true);
    }

    @Override
    public void execute() {
        doExecute(s -> s);
    }
}
