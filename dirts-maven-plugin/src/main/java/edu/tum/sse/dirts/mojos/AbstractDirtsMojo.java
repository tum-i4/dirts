package edu.tum.sse.dirts.mojos;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.cdi.analysis.*;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.control.Control;
import edu.tum.sse.dirts.core.strategies.CDIDependencyStrategy;
import edu.tum.sse.dirts.core.strategies.GuiceDependencyStrategy;
import edu.tum.sse.dirts.core.strategies.SpringDependencyStrategy;
import edu.tum.sse.dirts.guice.analysis.GuiceMethodLevelInjectionPointCollectorVisitor;
import edu.tum.sse.dirts.guice.analysis.GuiceMethodLevelMapper;
import edu.tum.sse.dirts.guice.analysis.GuiceClassLevelInjectionPointCollectorVisitor;
import edu.tum.sse.dirts.guice.analysis.GuiceClassLevelMapper;
import edu.tum.sse.dirts.spring.analysis.*;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.surefire.SurefirePlugin;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

/**
 * Abstract parent class of all Mojos related to DIRTS
 */
public abstract class AbstractDirtsMojo<T extends BodyDeclaration<?>> extends SurefirePlugin {

    //##################################################################################################################
    // Static constants

    /**
     * Name of surefire plugin
     */
    // From ekstazi: https://github.com/gliga/ekstazi/blob/6567da0534c20eeee802d2dfb8d216cbcbf6883c/ekstazi-maven-plugin/src/main/java/org/ekstazi/maven/AbstractEkstaziMojo.java#L50
    protected static final String SUREFIRE_PLUGIN_KEY = "org.apache.maven.plugins:maven-surefire-plugin";


    //##################################################################################################################
    // Parameters all subclasses use

    @Parameter(property = "logging", defaultValue = "INFO")
    protected String logging;

    @Parameter(property = "restrictive", defaultValue = "false")
    protected boolean restrictive;

    @Parameter(property = "useSpringExtension", defaultValue = "false")
    protected boolean useSpringExtension;

    @Parameter(property = "useGuiceExtension", defaultValue = "false")
    protected boolean useGuiceExtension;

    @Parameter(property = "useCDIExtension", defaultValue = "false")
    protected boolean useCDIExtension;

    @Parameter(property = "considerAnnotationsAsDependencies", defaultValue = "false")
    protected boolean annotations;

    //##################################################################################################################
    // Abstract methods implemented by all subclasses

    protected abstract Control<T> getControl();

    //##################################################################################################################
    // Methods to initialize control objects

    /**
     * Initializes class level blackboard
     *
     * @return blackboard
     */
    protected Blackboard<TypeDeclaration<?>> getClassLevelBlackboard() {
        Path rootPath = getRootPath();
        Path subPath = getSubPath();

        // Blackboard
        Blackboard<TypeDeclaration<?>> blackboard = new Blackboard<>(rootPath, subPath, "class_level");
        Blackboard.considerAnnotationsAsDependencies = annotations;

        // Spring
        if (useSpringExtension) {
            blackboard.addDependencyStrategy(new SpringDependencyStrategy<>(
                    new SpringClassLevelInjectionPointCollectorVisitor(),
                    new SpringBeanClassLevelDependencyCollector(),
                    new SpringClassLevelMapper()));
        }

        // Guice
        if (useGuiceExtension) {
            blackboard.addDependencyStrategy(new GuiceDependencyStrategy<>(
                    new GuiceClassLevelInjectionPointCollectorVisitor(),
                    new GuiceClassLevelMapper()));
        }

        // CDI
        if (useCDIExtension) {
            blackboard.addDependencyStrategy(new CDIDependencyStrategy<>(
                    new CDIClassLevelInjectionPointCollectorVisitor(),
                    new CDIClassLevelMapper(), new CDIClassLevelAlternativeDependencyCollector()));
        }

        return blackboard;
    }

    /**
     * Initializes method level blackboard
     *
     * @return blackboard
     */
    protected Blackboard<BodyDeclaration<?>> getMethodLevelBlackboard() {
        Path rootPath = getRootPath();
        Path subPath = getSubPath();

        Log.setLogLevel(Level.parse(logging));
        JavaParserUtils.RESTRICTIVE = restrictive;

        // Blackboard
        Blackboard<BodyDeclaration<?>> blackboard = new Blackboard<>(rootPath, subPath, "method_level");
        Blackboard.considerAnnotationsAsDependencies = annotations;

        // Spring
        if (useSpringExtension) {
            blackboard.addDependencyStrategy(new SpringDependencyStrategy<>(
                    new SpringMethodLevelInjectionPointCollectorVisitor(),
                    new SpringBeanMethodLevelDependencyCollector(),
                    new SpringMethodLevelMapper()));
        }

        // Guice
        if (useGuiceExtension) {
            blackboard.addDependencyStrategy(new GuiceDependencyStrategy<>(
                    new GuiceMethodLevelInjectionPointCollectorVisitor(),
                    new GuiceMethodLevelMapper()));
        }

        // CDI
        if (useCDIExtension) {
            blackboard.addDependencyStrategy(new CDIDependencyStrategy<>(
                    new CDIMethodLevelInjectionPointCollectorVisitor(),
                    new CDIMethodLevelMapper(), new CDIMethodLevelAlternativeDependencyCollector()));
        }

        return blackboard;
    }


    //##################################################################################################################
    // Auxiliary methods

    /**
     * @return the path of the outermost maven project
     */
    protected Path getRootPath() {
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
        return getRootPath().relativize(project.getBasedir().toPath());
    }


    /**
     * Find plugin based on the plugin key. Returns null if plugin
     * cannot be located.
     */
    // From ekstazi: https://github.com/gliga/ekstazi/blob/6567da0534c20eeee802d2dfb8d216cbcbf6883c/ekstazi-maven-plugin/src/main/java/org/ekstazi/maven/AbstractEkstaziMojo.java#L128
    // with slight modifications
    @SuppressWarnings({"SameParameterValue", "unchecked"})
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
