/*
 * Copyright 2022. The dirts authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package edu.tum.sse.dirts.mojos;

import com.github.javaparser.ast.body.BodyDeclaration;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.util.DirtsUtil;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.surefire.api.testset.ResolvedTest;
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
import java.util.logging.Level;
import java.util.stream.Collectors;

import static java.util.logging.Level.*;

public abstract class AbstractSelectMojo<P extends BodyDeclaration<?>> extends AbstractDirtsMojo<P> {

    private final static String DIRTS_EXCLUDES_PREFIX = "# DIRTS excluded";

    @Parameter(property = "standalone", defaultValue = "false")
    protected boolean standalone;

    @Parameter(property = "overrideExtension", defaultValue = "false")
    protected boolean overrideExtension;

    public void doExecute(Function<String, String> mapper) {
        Path subPath = getSubPath();
        Path rootPath = getRootPath();

        Log.setLogLevel(Level.parse(logging));
        JavaParserUtils.RESTRICTIVE = restrictive;

        Log.log(CONFIG, "Root path: " + rootPath);
        Log.log(CONFIG, "Sub path: " + subPath);

        if (subPath.toString().isEmpty()) {
            Log.log(INFO, "Plugin is executed on the outermost module. " +
                    "Clearing caches for affected modules and changed nodes.");

            Path affectedModulesPath = DirtsUtil.getAffectedModulesPath(rootPath);
            Path changedNodesPath = DirtsUtil.getChangedNodesPath(rootPath);

            if (!Files.exists(affectedModulesPath)) {
                try {
                    Files.createDirectories(affectedModulesPath.getParent());
                    Files.createFile(affectedModulesPath);
                } catch (IOException e) {
                    Log.errLog(INFO, "Failed to create file containing affected modules: " + e.getMessage());
                }
            }

            try {
                Files.writeString(affectedModulesPath, "pom.xml", StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                Log.errLog(INFO, "Failed to reset file containing affected modules: " + e.getMessage());
            }

            if (!Files.exists(changedNodesPath)) {
                try {
                    Files.createDirectories(changedNodesPath.getParent());
                    Files.createFile(changedNodesPath);
                } catch (IOException e) {
                    Log.errLog(INFO, "Failed to create file containing changed nodes: " + e.getMessage());
                }
            }

            try {
                Files.writeString(changedNodesPath, "", StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                Log.errLog(INFO, "Failed to reset file containing changed nodes: " + e.getMessage());
            }

        }

        if (getProject().getPackaging().equals("pom")) {
            Log.log(INFO, "There are no tests that could be selected, " +
                    "since this project has packaging \"pom\".");
            try {
                Files.delete(DirtsUtil.getLibrariesPath(rootPath, subPath));
            } catch (IOException e) {
                Log.errLog(INFO, "Failed to delete file containing dependencies: " + e.getMessage());
            }
            return;
        }

        if (standalone) {
            Log.log(INFO, "Running in standalone mode");
        } else {
            Log.log(INFO, "Running in non-standalone mode");
            if (!overrideExtension) {
                Log.log(INFO, "We expect that another RTS-tool has already excluded some tests in the excludesFile. " +
                        "Every test that has not been excluded this way is considered as included by this other tool and will not be excluded.");
            }
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

            Map<String, Set<String>> included = new HashMap<>();
            tests.forEach((affectingNode, affectedTests) -> {
                for (String affectedTest : affectedTests) {
                    if (affectingNode != null) {
                        Set<String> strings = included.computeIfAbsent(mapper.apply(affectedTest), k -> new HashSet<>());
                        strings.add(affectingNode);
                    }
                }
            });

            // In method level RTS, it is possible that some test methods of a class are excluded while others are included.
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
        if (standalone)
            setExcludesFile();

        TestListResolver result = null;
        try {
            Class<?> abstractSurefireMojoClass = Class.forName("org.apache.maven.plugin.surefire.AbstractSurefireMojo");

            try {
                Method getIncludedAndExcludedTestsMethod =
                        abstractSurefireMojoClass.getDeclaredMethod("getIncludedAndExcludedTests");
                getIncludedAndExcludedTestsMethod.setAccessible(true);

                result = (TestListResolver) getIncludedAndExcludedTestsMethod.invoke(this);

                Set<ResolvedTest> toRemove = new HashSet<>();
                for (ResolvedTest excludedPattern : result.getExcludedPatterns()) {
                    if (excludedPattern.toString().equals("**/*.java")) {
                        toRemove.add(excludedPattern);
                        Log.log(SEVERE, "Found pattern !" + excludedPattern + " - removing it!");
                    }
                }
                if (!toRemove.isEmpty()) {
                    Set<ResolvedTest> newExcludesPatterns = new HashSet<>(result.getExcludedPatterns());
                    newExcludesPatterns.removeAll(toRemove);

                    Class<?> testListResolverClass = TestListResolver.class;
                    try {
                        Field excludedPatterns = testListResolverClass.getDeclaredField("excludedPatterns");
                        excludedPatterns.setAccessible(true);
                        excludedPatterns.set(result, Collections.unmodifiableSet(newExcludesPatterns));

                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                }

                Set<ResolvedTest> includedPatterns = result.getIncludedPatterns();
                Class<?> resolvedTestClass = ResolvedTest.class;
                try {
                    Field methodPattern = resolvedTestClass.getDeclaredField("methodPattern");
                    for (ResolvedTest includedPattern : includedPatterns) {
                        methodPattern.setAccessible(true);
                        methodPattern.set(includedPattern, null);
                    }
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }


            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (!standalone)
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

    protected void writeSelectedTests(Map<String, Set<String>> included, Set<String> excluded) {
        File excludesFile = getExcludesFile();

        if (excludesFile == null) {
            excludesFile = getRootPath().resolve(getSubPath()).resolve("Excludes").toFile();
            Log.log(WARNING, "Surefire's excludesFile property is not set " +
                    "- excluded tests will be written to " + excludesFile.getAbsolutePath());
        }

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

            if (!standalone && !overrideExtension) {
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
        } catch (
                IOException e) {
            System.err.println("Unable to read/write excludesFile");
        }


        try {
            Path rootPath = getRootPath();
            Path subPath = getSubPath();
            Path affectedModulesPath = DirtsUtil.getAffectedModulesPath(rootPath);

            if (!Files.exists(affectedModulesPath)) {
                Files.createDirectories(affectedModulesPath.getParent());
                Files.createFile(affectedModulesPath);
            }

            Set<String> affectedModules = new HashSet<>(Set.of(Files.readString(affectedModulesPath).split(", ")));
            String currentModule = DirtsUtil.getSubPomPathRelative(subPath).toString();

            if (included.isEmpty()) {
                affectedModules.remove(currentModule);
            } else {
                affectedModules.add(currentModule);
            }

            Files.writeString(affectedModulesPath,
                    String.join(", ", affectedModules),
                    StandardOpenOption.TRUNCATE_EXISTING);

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
