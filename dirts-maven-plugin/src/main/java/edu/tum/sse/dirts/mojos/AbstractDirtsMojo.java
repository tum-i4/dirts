package edu.tum.sse.dirts.mojos;

import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.cdi.analysis.CDINonTypeDependencyCollectorVisitor;
import edu.tum.sse.dirts.cdi.analysis.CDITypeDependencyCollectorVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.core.control.NonTypeLevelControl;
import edu.tum.sse.dirts.core.control.TypeLevelControl;
import edu.tum.sse.dirts.core.strategies.CDIDependencyStrategy;
import edu.tum.sse.dirts.core.strategies.RecomputeAllDependencyStrategy;
import edu.tum.sse.dirts.core.strategies.SpringDependencyStrategy;
import edu.tum.sse.dirts.guice.analysis.GuiceNonTypeDependencyCollectorVisitor;
import edu.tum.sse.dirts.guice.analysis.GuiceTypeDependencyCollectorVisitor;
import edu.tum.sse.dirts.spring.analysis.SpringBeanNonTypeDependencyCollector;
import edu.tum.sse.dirts.spring.analysis.SpringBeanTypeDependencyCollector;
import edu.tum.sse.dirts.spring.analysis.SpringNonTypeDependencyCollectorVisitor;
import edu.tum.sse.dirts.spring.analysis.SpringTypeDependencyCollectorVisitor;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.surefire.SurefirePlugin;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.graph.EdgeType.DI_GUICE;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

/**
 * Abstract parent class of all Mojos related to DIRTS
 */
public abstract class AbstractDirtsMojo extends SurefirePlugin {

    //##################################################################################################################
    // Static constants

    /**
     * Name of surefire plugin
     */
    // From ekstazi: https://github.com/gliga/ekstazi/blob/6567da0534c20eeee802d2dfb8d216cbcbf6883c/ekstazi-maven-plugin/src/main/java/org/ekstazi/maven/AbstractEkstaziMojo.java#L50
    protected static final String SUREFIRE_PLUGIN_KEY = "org.apache.maven.plugins:maven-surefire-plugin";

    private final static String DIRTS_EXCLUDES_PREFIX = "# DIRTS excluded";

    //##################################################################################################################
    // Parameters all subclasses use

    @Parameter(property = "logging", defaultValue = "INFO")
    protected String logging;

    @Parameter(property = "standalone", defaultValue = "false")
    protected boolean standalone;

    @Parameter(property = "restrictive", defaultValue = "false")
    protected boolean restrictive;

    @Parameter(property = "useSpringExtension", defaultValue = "false")
    protected boolean useSpringExtension;

    @Parameter(property = "useGuiceExtension", defaultValue = "false")
    protected boolean useGuiceExtension;

    @Parameter(property = "useCDIExtension", defaultValue = "false")
    protected boolean useCDIExtension;

    @Parameter(property = "printBeans", defaultValue = "false")
    protected boolean printBeans;

    //##################################################################################################################
    // Abstract mehtods implemented by all subclasses

    protected abstract Control getControl();

    //##################################################################################################################
    // Methods to initialize control objects

    /**
     * Initializes class level control object
     *
     * @return
     */
    protected TypeLevelControl getTypeLevelControl(boolean overwrite) {
        Path basePath = getBasePath();
        Path subPath = basePath.relativize(getSubPath());
        TestFilter<String, String> testFilter = getTestFilter();

        Log.setLogLevel(Level.parse(logging));

        if (testFilter != null)
            Log.log(FINE, "Using test filter: " + testFilter);

        // Blackboard and Control
        Blackboard blackboard = new Blackboard(basePath, subPath, testFilter);
        TypeLevelControl control = new TypeLevelControl(blackboard, overwrite);

        Control.PRINT_BEANS = printBeans;
        JavaParserUtils.RESTRICTIVE = restrictive;

        // Spring
        if (useSpringExtension) {
            blackboard.addDependencyStrategy(new SpringDependencyStrategy(
                    new SpringTypeDependencyCollectorVisitor(),
                    new SpringBeanTypeDependencyCollector()));
        }

        // Guice
        if (useGuiceExtension) {
            blackboard.addDependencyStrategy(new RecomputeAllDependencyStrategy(
                    Set.of(new GuiceTypeDependencyCollectorVisitor(new BeanStorage<>())),
                    Set.of(DI_GUICE)));
        }

        // CDI
        if (useCDIExtension) {
            blackboard.addDependencyStrategy(
                    new CDIDependencyStrategy(new CDITypeDependencyCollectorVisitor()));
        }

        return control;
    }

    /**
     * Initializes method level control object
     *
     * @return
     */
    protected NonTypeLevelControl getNonTypeLevelControl(boolean overwrite) {
        Path basePath = getBasePath();
        Path subPath = basePath.relativize(getSubPath());
        TestFilter<String, String> testFilter = getTestFilter();

        Log.setLogLevel(Level.parse(logging));

        if (testFilter != null)
            Log.log(FINE, "Using test filter: " + testFilter);

        // Blackboard and Control
        Blackboard blackboard = new Blackboard(basePath, subPath, testFilter);
        NonTypeLevelControl control = new NonTypeLevelControl(blackboard, overwrite);

        Control.PRINT_BEANS = printBeans;
        JavaParserUtils.RESTRICTIVE = restrictive;

        // Spring
        if (useSpringExtension) {
            blackboard.addDependencyStrategy(new SpringDependencyStrategy(
                    new SpringNonTypeDependencyCollectorVisitor(),
                    new SpringBeanNonTypeDependencyCollector()));
        }

        // Guice
        if (useGuiceExtension) {
            blackboard.addDependencyStrategy(new RecomputeAllDependencyStrategy(
                    Set.of(new GuiceNonTypeDependencyCollectorVisitor(new BeanStorage<>())),
                    Set.of(DI_GUICE)));
        }

        // CDI
        if (useCDIExtension) {
            blackboard.addDependencyStrategy(
                    new CDIDependencyStrategy(new CDINonTypeDependencyCollectorVisitor()));
        }
        return control;
    }


    //##################################################################################################################
    // Auxiliary methods

    /**
     * @return the path of the outermost maven project
     */
    protected Path getBasePath() {
        MavenProject project = getProject();
        while (project.hasParent() && project.getParent().getBasedir() != null) {
            project = project.getParent();
        }
        return project.getBasedir().toPath();
    }

    /**
     * @return the path of the current maven project, can be an inner maven project
     */
    protected Path getSubPath() {
        MavenProject project = getProject();
        return project.getBasedir().toPath();
    }

    /**
     * @return The TestFilter object used by surefire to determine which tests to execute
     */
    private TestFilter<String, String> getTestFilter() {
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

    private void setExcludesFile() {
        Plugin surefirePlugin = lookupPlugin(SUREFIRE_PLUGIN_KEY);
        Xpp3Dom configuration = (Xpp3Dom) surefirePlugin.getConfiguration();

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

    private void setField(Class<?> klass, String name, Object value) {
        try {
            Field field = klass.getDeclaredField(name);
            field.setAccessible(true);
            field.set(this, value);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
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
            try {
                Set<String> includedFormatted = included.keySet().stream()
                        .map(t -> t.replaceAll("\\.", "/") + ".java").collect(Collectors.toSet());
                Set<String> excludedFormatted = excluded.stream()
                        .map(t -> t.replaceAll("\\.", "/") + ".java").collect(Collectors.toSet());


                if (!Files.exists(excludesFile.toPath())) {
                    Files.createFile(excludesFile.toPath());
                }
                String excludesFileContent = Files.readString(excludesFile.toPath());
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


                Files.writeString(excludesFile.toPath(), newExcludesFileContent);
            } catch (IOException e) {
                System.err.println("Unable to read/write excludesFile");
            }
        } else {
            Log.errLog(WARNING, "Surefire's excludesFile property is not set " +
                    "- test selection will not work properly");
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


    /**
     * Find plugin based on the plugin key. Returns null if plugin
     * cannot be located.
     */
    // From ekstazi: https://github.com/gliga/ekstazi/blob/6567da0534c20eeee802d2dfb8d216cbcbf6883c/ekstazi-maven-plugin/src/main/java/org/ekstazi/maven/AbstractEkstaziMojo.java#L128
    // with slight modifications
    protected Plugin lookupPlugin(String key) {
        List<Plugin> plugins = this.getProject().getBuildPlugins();

        for (Plugin plugin : plugins) {
            if (key.equalsIgnoreCase(plugin.getKey())) {
                return plugin;
            }
        }
        return null;
    }
}
