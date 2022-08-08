package edu.tum.sse.dirts.core.strategies;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.di.*;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.util.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.util.DirtsUtil.getBeansPath;
import static edu.tum.sse.dirts.util.DirtsUtil.getInjectionPointsPath;
import static java.util.logging.Level.*;

public abstract class DIDependencyStrategy<T extends BodyDeclaration<?>, B extends Bean> implements DependencyStrategy<T> {

    private final static ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<BeanStorage<Set<String>>> typeReferenceBeanStorage = new TypeReference<>() {
    };
    private static final TypeReference<InjectionPointStorage> typeReferenceInjectionPointStorage = new TypeReference<>() {
    };

    private final String prefix;

    protected BeanStorage<Set<String>> beans;
    private InjectionPointStorage injectionPoints;

    private final InjectionPointCollector<T> injectionPointCollector;
    protected final EdgeType edgeType;

    protected final NameMapper<B> nameMapper;

    protected DIDependencyStrategy(String prefix,
                                   InjectionPointCollector<T> injectionPointCollector,
                                   EdgeType edgeType,
                                   NameMapper<B> nameMapper) {
        this.prefix = prefix;
        this.injectionPointCollector = injectionPointCollector;
        this.edgeType = edgeType;
        this.nameMapper = nameMapper;
    }

    @Override
    public void doImport(Path tmpPath, Blackboard<T> blackboard, String suffix) {
        Path rootPath = blackboard.getRootPath();
        Path subPath = blackboard.getSubPath();

        Path beansPath = getBeansPath(rootPath, subPath, prefix, suffix);
        Path injectionPointsPath = getInjectionPointsPath(rootPath, subPath, prefix, suffix);

        try {
            String beanStorageString = Files.readString(beansPath);
            beans = objectMapper.readValue(beanStorageString, typeReferenceBeanStorage);
        } catch (IOException e) {
            beans = new BeanStorage<>();
        }

        try {
            String injectionPointsString = Files.readString(injectionPointsPath);
            injectionPoints = objectMapper.readValue(injectionPointsString, typeReferenceInjectionPointStorage);
        } catch (IOException e) {
            injectionPoints = new InjectionPointStorage();
        }
    }

    @Override
    public void doExport(Path tmpPath, Blackboard<T> blackboard, String suffix) {
        Path rootPath = blackboard.getRootPath();
        Path subPath = blackboard.getSubPath();

        Path beansPath = getBeansPath(rootPath, subPath, prefix, suffix);
        Path injectionPointsPath = getInjectionPointsPath(rootPath, subPath, prefix, suffix);

        try {
            Files.createDirectories(beansPath.getParent());

            Files.writeString(beansPath,
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(beans),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException ignored) {
            Log.log(SEVERE, "Failed to export " + prefix + " beans");
        }

        try {
            Files.createDirectories(injectionPointsPath.getParent());

            Files.writeString(injectionPointsPath,
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(injectionPoints),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException ignored) {
            Log.log(SEVERE, "Failed to export " + prefix + " injection points");
        }
    }

    @Override
    public void doChangeAnalysis(Blackboard<T> blackboard) {
    }

    @Override
    public void doGraphCropping(Blackboard<T> blackboard) {
        DependencyGraph dependencyGraph = blackboard.getDependencyGraphNewRevision();

        // remove impacted injection points, those are going to be rediscovered
        Collection<String> impactedNodes = blackboard.getImpactedNodes();
        Set<String> nodesRemoved = blackboard.getNodesRemoved().keySet();

        impactedNodes.forEach(injectionPoints::removeInjectionPoint);
        nodesRemoved.forEach(injectionPoints::removeInjectionPoint);

        // remove edges from impacted injection points
        impactedNodes.forEach(b -> dependencyGraph.removeAllEdgesFrom(b, Set.of(edgeType)));

        Set<Set<String>> impactedBeans = beans.getAllBeans().stream()
                .filter(p -> p.stream().anyMatch(impactedNodes::contains)
                        || p.stream().anyMatch(nodesRemoved::contains))
                .collect(Collectors.toSet());

        impactedBeans.addAll(calculateImpactedBeans());

        // remove impacted beans, those are going to be rediscovered
        impactedBeans.forEach(beans::removeBean);

        if (!impactedBeans.isEmpty())
            Log.log(FINER, "BEANS", "Impacted beans:\n" + impactedBeans);

        // remove edges to impacted beans
        impactedBeans.stream()
                .flatMap(Collection::stream)
                .forEach(b -> dependencyGraph.removeAllEdgesTo(b, Set.of(edgeType)));
    }

    protected Set<Set<String>> calculateImpactedBeans() {
        return Set.of();
    }

    ;

    protected abstract BeanStorage<B> collectBeans(Collection<TypeDeclaration<?>> ts);

    @Override
    public void doDependencyAnalysis(Blackboard<T> blackboard) {
        DependencyGraph dependencyGraph = blackboard.getDependencyGraphNewRevision();

        Collection<TypeDeclaration<?>> impactedTypes = blackboard.getImpactedTypes();

        BeanStorage<B> newBeans = collectBeans(impactedTypes);

        if (!newBeans.isEmpty())
            Log.log(FINER, "BEANS", "Beans that have been discovered or reevaluated:\n" + newBeans);

        InjectionPointStorage newInjectionPoints = new InjectionPointStorage();
        injectionPointCollector.collectInjectionPoints(impactedTypes, newInjectionPoints);

        if (!newInjectionPoints.isEmpty())
            Log.log(FINER,
                    "INJECTION_POINTS",
                    "InjectionPoints that have been discovered or reevaluated:\n" + newInjectionPoints);

        for (B bean : newBeans.getAllBeans()) {
            for (String s : nameMapper.mapToString(bean)) {
                dependencyGraph.removeAllEdgesTo(s, Set.of(edgeType));
            }
        }
        for (String injectionPoint : newInjectionPoints.getInjectionPoints().keySet()) {
            dependencyGraph.removeAllEdgesFrom(injectionPoint, Set.of(edgeType));
        }

        // consider join between newBeans and newInjectionPoints
        join(dependencyGraph, newInjectionPoints, newBeans, nameMapper::mapToString);

        // consider join between newBeans and (old) injectionPoints
        join(dependencyGraph, injectionPoints, newBeans, nameMapper::mapToString);

        // consider join between (old) beans and newInjectionPoints
        join(dependencyGraph, newInjectionPoints, beans, b -> b);

        newBeans.getBeansByName().forEach((k, v) -> v.forEach(b -> beans.addBeanByName(k, nameMapper.mapToString(b))));
        newBeans.getBeansByType().forEach((k, v) -> v.forEach(b -> beans.addBeanByType(k, nameMapper.mapToString(b))));
        newBeans.getBeansByQualifier().forEach((k, v) -> v.forEach(b -> beans.addBeanByName(k, nameMapper.mapToString(b))));

        injectionPoints.addAll(newInjectionPoints);
    }

    @Override
    public void combineGraphs(Blackboard<T> blackboard) {

    }


    private <T1, B1> void join(DependencyGraph dependencyGraph,
                               InjectionPointStorage injectionPoints,
                               BeanStorage<B1> beanStorage,
                               Function<B1, Set<String>> mapper) {
        injectionPoints.getInjectionPoints().forEach((fromNode, keys) -> keys.forEach(key -> {
            // fetch possible beans
            Set<B1> beans = beanStorage.getBeans(key.getFirst(), key.getSecond(), key.getThird());
            for (B1 bean : beans) {
                Set<String> beansNodes = mapper.apply(bean);

                // add edges
                for (String toNode : beansNodes) {
                    Log.log(FINEST, "Connected injectionPoint " + fromNode + " to bean " + toNode);
                    dependencyGraph.addEdge(fromNode, toNode, edgeType);
                }
            }
        }));
    }
}
