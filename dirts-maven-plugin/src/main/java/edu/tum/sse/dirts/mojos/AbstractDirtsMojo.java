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
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.surefire.SurefirePlugin;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.graph.EdgeType.DI_GUICE;

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
    // Methods to initialize control objects

    /**
     * Initializes class level control object
     *
     * @return
     */
    protected TypeLevelControl getTypeLevelControl(boolean overwrite) {
        Path basePath = getBasePath();
        Path subPath = basePath.relativize(getSubPath());

        // Blackboard and Control
        Blackboard blackboard = new Blackboard(basePath, subPath);
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
        };

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

        // Blackboard and Control
        Blackboard blackboard = new Blackboard(basePath, subPath);
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

    protected void writeSelectedTests(Map<String, String> included, Set<String> excluded) throws MojoExecutionException {
        Plugin surefirePlugin = lookupPlugin(SUREFIRE_PLUGIN_KEY);
        if (surefirePlugin != null) {
            String excludesFileString = extractParamValue(surefirePlugin, "excludesFile");
            if (excludesFileString != null) {
                Path excludesFilePath = Path.of(excludesFileString);
                try {
                    Set<String> includedFormatted = included.keySet().stream()
                            .map(t -> t.replaceAll("\\.", "/") + ".java").collect(Collectors.toSet());
                    Set<String> excludedFormatted = excluded.stream()
                            .map(t -> t.replaceAll("\\.", "/") + ".java").collect(Collectors.toSet());

                    StringBuilder newExcludesFileContent = new StringBuilder();
                    if (!Files.exists(excludesFilePath)) {
                        Files.createFile(excludesFilePath);
                    }
                    String excludesFileContent = Files.readString(excludesFilePath);

                    if (excludesFileContent.strip().startsWith(DIRTS_EXCLUDES_PREFIX)
                            || excludesFileContent.strip().equals("")) {
                        // overwrite content of excludesFile with our excluded tests
                        newExcludesFileContent.append(DIRTS_EXCLUDES_PREFIX).append("\n");
                        newExcludesFileContent.append("**/*$*\n");
                        excludedFormatted.forEach(t -> newExcludesFileContent.append(t).append("\n"));
                    } else {
                        // only change the excluded tests already present
                        String[] lines = excludesFileContent.split("\n");
                        for (String line : lines) {
                            if (includedFormatted.contains(line)) {
                                newExcludesFileContent.append("# ").append(line).append("\n");
                            } else {
                                newExcludesFileContent.append(line).append("\n");
                            }
                        }

                    }

                    Files.writeString(excludesFilePath, newExcludesFileContent);
                } catch (IOException e) {
                    System.err.println("Unable to read/write excludesFile");
                }
            } else {
                System.err.println("[IMPORTANT] Surefire's excludesFile property is not set " +
                        "- test selection will not work properly");
            }
        } else {
            System.err.println("Did not find surefire plugin");
        }
        System.out.println("DIRTS excluded: " + excluded);
        System.out.println("DIRTS included: " + included);
    }


    /**
     * Locate paramName in the given (surefire) plugin. Returns value
     * of the file.
     */
    // From Ekstazi: https://github.com/gliga/ekstazi/blob/6567da0534c20eeee802d2dfb8d216cbcbf6883c/ekstazi-maven-plugin/src/main/java/org/ekstazi/maven/AbstractEkstaziMojo.java#L162
    protected String extractParamValue(Plugin plugin, String paramName) throws MojoExecutionException {
        Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
        if (configuration == null) {
            return null;
        }
        Xpp3Dom paramDom = configuration.getChild(paramName);
        return paramDom == null ? null : paramDom.getValue();
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
