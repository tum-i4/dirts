package edu.tum.sse.dirts.mojos;

import com.github.javaparser.ast.body.BodyDeclaration;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.util.Log;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.surefire.api.testset.TestFilter;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.logging.Level.*;

public abstract class AbstractSelectMojo<P extends BodyDeclaration<?>> extends AbstractDirtsMojo<P> {

    private final static String DIRTS_EXCLUDES_PREFIX = "# DIRTS excluded";

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

        Control<P> control = getControl();

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

    /**
     * @return The TestFilter object used by surefire to determine which tests to execute
     */
    protected TestFilter<String, String> getTestFilter() {
        setSurefireParameters();
        if (!standalone)
            setExcludesFile();

        TestListResolver result = null;
        try {
            Class<?> abstractSurefireMojoClass = Class.forName("org.apache.maven.plugin.surefire.AbstractSurefireMojo");

            try {
                Method getIncludedAndExcludedTestsMethod = abstractSurefireMojoClass.getDeclaredMethod("getIncludedAndExcludedTests");
                getIncludedAndExcludedTestsMethod.setAccessible(true);

                result = (TestListResolver) getIncludedAndExcludedTestsMethod.invoke(this);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (standalone)
            setExcludesFile();
        return result;
    }

    protected void setSurefireParameters() {
        Plugin surefirePlugin = lookupPlugin(SUREFIRE_PLUGIN_KEY);
        Xpp3Dom configuration = (Xpp3Dom) surefirePlugin.getConfiguration();

        if (configuration != null) {
            Xpp3Dom includesFileChild = configuration.getChild("includesFile");
            Xpp3Dom excludesChild = configuration.getChild("excludes");
            Xpp3Dom includesChild = configuration.getChild("includes");

            try {
                Class<?> abstractSurefireMojoClass = Class.forName("org.apache.maven.plugin.surefire.AbstractSurefireMojo");
                Class<?> surefirePluginClass = Class.forName("org.apache.maven.plugin.surefire.SurefirePlugin");

                if (includesFileChild != null)
                    setField(surefirePluginClass, "includesFile", Path.of(includesFileChild.getValue()).toFile());

                if (excludesChild != null) {
                    Xpp3Dom[] excludesChildren = excludesChild.getChildren("exclude");
                    List<String> excludes = Arrays.stream(excludesChildren).map(Xpp3Dom::getValue).collect(Collectors.toList());
                    setField(abstractSurefireMojoClass, "excludes", excludes);
                }

                if (includesChild != null) {
                    Xpp3Dom[] includesChildren = includesChild.getChildren("include");
                    List<String> includes = Arrays.stream(includesChildren).map(Xpp3Dom::getValue).collect(Collectors.toList());
                    setField(surefirePluginClass, "includes", includes);
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void setField(Class<?> klass, String name, Object value) {
        try {
            Field field = klass.getDeclaredField(name);
            field.setAccessible(true);
            field.set(this, value);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private void setExcludesFile() {
        Plugin surefirePlugin = lookupPlugin(SUREFIRE_PLUGIN_KEY);
        Xpp3Dom configuration = (Xpp3Dom) surefirePlugin.getConfiguration();

        if (configuration != null) {
            Xpp3Dom excludesFileChild = configuration.getChild("excludesFile");

            try {
                Class<?> surefirePluginClass = Class.forName("org.apache.maven.plugin.surefire.SurefirePlugin");
                if (excludesFileChild != null)
                    setField(surefirePluginClass, "excludesFile", Path.of(excludesFileChild.getValue()).toFile());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            clearPreviousExcludes();
        }
    }

    protected void clearPreviousExcludes() {
        File excludesFile = getExcludesFile();

        if (excludesFile != null) {
            try {
                if (!Files.exists(excludesFile.toPath())) {
                    Files.createFile(excludesFile.toPath());
                }
                String excludesFileContent = Files.readString(excludesFile.toPath());
                String[] split = excludesFileContent.split(DIRTS_EXCLUDES_PREFIX);
                Files.writeString(excludesFile.toPath(), split[0]);
            } catch (IOException e) {
                System.err.println("Unable to read/write excludesFile");
            }
        }

    }

    protected void writeSelectedTests(Map<String, String> included, Set<String> excluded) {
        File excludesFile = getExcludesFile();
        if (excludesFile != null) {
            Path excludesFilePath = excludesFile.toPath();
            try {
                Set<String> includedFormatted = included.keySet().stream()
                        .map(t -> t.replaceAll("\\.", "/") + ".java").collect(Collectors.toSet());
                Set<String> excludedFormatted = excluded.stream()
                        .map(t -> t.replaceAll("\\.", "/") + ".java").collect(Collectors.toSet());

                if (!Files.exists(excludesFilePath)) {
                    Files.createDirectories(excludesFilePath.getParent());
                    Files.createFile(excludesFilePath);
                }
                String excludesFileContent = Files.readString(excludesFilePath);
                StringBuilder newExcludesFileContent = new StringBuilder();

                if (standalone) {
                    // only change the excluded tests already present
                    String[] lines = excludesFileContent.split("\n");
                    for (String line : lines) {
                        if (includedFormatted.contains(line)) {
                            newExcludesFileContent.append("# ").append(line).append("\n");
                        } else {
                            newExcludesFileContent.append(line).append("\n");
                        }
                    }
                } else {
                    // exclude all tests that have not been selected
                    newExcludesFileContent.append(excludesFileContent);
                    newExcludesFileContent.append(DIRTS_EXCLUDES_PREFIX).append("\n");
                    newExcludesFileContent.append("**/*$*\n");
                    excludedFormatted.forEach(t -> newExcludesFileContent.append(t).append("\n"));
                }


                Files.writeString(excludesFilePath, newExcludesFileContent);
            } catch (IOException e) {
                System.err.println("Unable to read/write excludesFile");
            }
        } else {
            Log.errLog(WARNING, "Surefire's excludesFile property is not set " +
                    "- test selection will not work properly");
        }


        try {
            Path basePath = getBasePath();
            Path subPath = getSubPath();
            Path affectedModulesPath = basePath.resolve(".dirts").resolve("affected_modules");

            if (!Files.exists(affectedModulesPath)) {
                Files.createDirectories(affectedModulesPath.getParent());
                Files.createFile(affectedModulesPath);
            }

            if (!included.isEmpty()) {
                Files.writeString(affectedModulesPath,
                        basePath.relativize(subPath.resolve("pom.xml")) + ", ",
                        StandardOpenOption.APPEND);
            }

        } catch (IOException e) {
            Log.errLog(SEVERE, "Unable to read/write affectedModules");
            e.printStackTrace();
        }


        System.out.println("DIRTS excluded: [" +
                String.join("\n", excluded) +
                "]");
        System.out.println("DIRTS included: [" +
                included.entrySet().stream()
                        .map(e -> e.getKey() + " <-- " + e.getValue())
                        .collect(Collectors.joining("\n")) +
                "]");
    }
}
