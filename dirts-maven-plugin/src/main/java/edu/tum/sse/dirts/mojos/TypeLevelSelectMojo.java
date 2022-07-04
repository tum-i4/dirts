package edu.tum.sse.dirts.mojos;

import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.graph.EdgeType;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mojo(name = "typeL_select")
@Execute(goal = "typeL_select", phase = LifecyclePhase.INITIALIZE, lifecycle = "dirts")
public class TypeLevelSelectMojo extends AbstractDirtsMojo {

    @Override
    public void execute() throws MojoExecutionException {
        if (getProject().getPackaging().equals("pom")) {
            System.out.println("There are no tests that could be selected, since this project has packaging \"pom\".");
            try {
                Files.delete(getSubPath().resolve(".dirts_dependencies"));
            } catch (IOException e) {
                System.err.println("Failed to delete file containing dependencies: " + e.getMessage());
            }
            return;
        }

        if (standalone) {
            System.out.println("Running in standalone mode");
        }

        Control control = super.getTypeLevelControl(true);

        Set<EdgeType> edgeTypes = new HashSet<>();

        if (!standalone) {
            edgeTypes.add(EdgeType.DI_SPRING);
            edgeTypes.add(EdgeType.DI_GUICE);
            edgeTypes.add(EdgeType.DI_CDI);
        }

        Map<String, Set<String>> tests = control.getSelectedTests(edgeTypes);

        if (tests != null) {
            Set<String> excluded = tests.get(null);

            Map<String, String> included = new HashMap<>();
            tests.forEach((affectingNode, affectedTests) -> {
                for (String affectedTest : affectedTests) {
                    if (affectingNode != null)
                        included.put(affectedTest, affectingNode);
                }
            });

            writeSelectedTests(included, excluded);
        }
    }
}
