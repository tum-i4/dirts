package edu.tum.sse.dirts.mojos;

import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.core.control.TypeLevelControl;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.lang.reflect.Type;

@Mojo(name = "typeL_graph")
@Execute(goal = "typeL_graph", phase = LifecyclePhase.INITIALIZE, lifecycle = "dirts")
public class TypeLevelGraphMojo extends AbstractGraphMojo<TypeDeclaration<?>> {

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
    protected Control<TypeDeclaration<?>> getControl() {
        Blackboard<TypeDeclaration<?>> typeLevelBlackboard = getTypeLevelBlackboard();
        return new TypeLevelControl(typeLevelBlackboard, false);
    }
}