package edu.tum.sse.dirts.mojos;

import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.util.Log;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.logging.Level.INFO;

public abstract class AbstractSelectMojo extends AbstractDirtsMojo{

    public void doExecute(Function<String, String> mapper) throws MojoExecutionException {
        if (getProject().getPackaging().equals("pom")) {
            Log.log(INFO, "There are no tests that could be selected, " +
                    "since this project has packaging \"pom\".");
            try {
                Files.delete(getSubPath().resolve(".dirts_dependencies"));
            } catch (IOException e) {
                System.err.println("Failed to delete file containing dependencies: " + e.getMessage());
            }
            return;
        }

        if (standalone) {
            Log.log(INFO, "Running in standalone mode");
        }

        Control control = getControl();

        Set<EdgeType> edgeTypes = new HashSet<>();

        if (!standalone) {
            edgeTypes.add(EdgeType.DI_SPRING);
            edgeTypes.add(EdgeType.DI_GUICE);
            edgeTypes.add(EdgeType.DI_CDI);
        }

        Map<String, Set<String>> tests = control.getSelectedTests(edgeTypes);

        if (tests != null) {
            Set<String> excluded = tests.get(null).stream().map(mapper).collect(Collectors.toSet());

            Map<String, String> included = new HashMap<>();
            tests.forEach((affectingNode, affectedTests) -> {
                for (String affectedTest : affectedTests) {
                    if (affectingNode != null)
                        included.put(mapper.apply(affectedTest), affectingNode);
                }
            });

            // In nontypeL, it is possible that some test methods of a class are excluded while others are included.
            // In this case, we want the class not to be excluded
            excluded.removeAll(included.keySet());

            writeSelectedTests(included, excluded);
        }
    }
}
