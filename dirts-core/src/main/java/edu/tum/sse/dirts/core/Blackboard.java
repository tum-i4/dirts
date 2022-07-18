/*
 * Copyright 2022. The ttrace authors.
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
package edu.tum.sse.dirts.core;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import edu.tum.sse.dirts.analysis.def.identifiers.nontype.InheritanceIdentifierVisitor;
import edu.tum.sse.dirts.core.strategies.DependencyStrategy;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.ModificationGraph;
import edu.tum.sse.dirts.util.Log;
import edu.tum.sse.dirts.util.naming_scheme.Names;
import edu.tum.sse.dirts.util.tuples.Pair;
import org.apache.maven.surefire.api.testset.TestFilter;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.logging.Level.*;

/**
 * Used to store all available knowledge
 */
public class Blackboard<T extends BodyDeclaration<?>> {

    /*
     * Central class of the blackboard pattern
     *
     * The main logic for selecting tests is contained in KnowledgeSources
     * Some DependencyStrategies may be used as extensions
     */

    //##################################################################################################################
    // Attributes

    private BlackboardState state;
    private final List<KnowledgeSource<T>> knowledgeSources;
    private final List<DependencyStrategy<T>> dependencyStrategies;

    // #################################################################################################################
    // ## Content of blackboard, actual knowledge

    private final Path rootPath;
    private final Path subPath;

    private TestFilter<String, String> testFilter;

    private Map<String, Integer> checksumsNodes;
    private Map<String, String> compilationUnitMapping;

    private CombinedTypeSolver typeSolver;

    private Collection<CompilationUnit> compilationUnits;

    private Map<String, Node> allNodes;
    private Map<String, Node> nodesSame;
    private Map<String, Node> nodesDifferent;
    private Map<String, Node> nodesAdded;
    private Map<String, Integer> nodesRemoved;

    private Map<String, String> nameMapperNodes;

    private Collection<String> tests;

    private Collection<TypeDeclaration<?>> impactedTypes;

    private DependencyGraph graphOldRevision;
    private DependencyGraph graphNewRevision;

    private ModificationGraph modificationGraph;

    // Only used by nontypeL
    private Map<TypeDeclaration<?>, InheritanceIdentifierVisitor> inheritanceIdentifierVisitorMap;

    //##################################################################################################################
    // Constructors

    public Blackboard(Path rootPath, Path subPath) {
        this.rootPath = rootPath;
        this.subPath = subPath;

        this.state = BlackboardState.CLEAN;
        knowledgeSources = new ArrayList<>();
        dependencyStrategies = new ArrayList<>();
    }

    //##################################################################################################################
    // Getters and Setters

    public BlackboardState getState() {
        return state;
    }

    public void setState(BlackboardState state) {
        this.state = state;
    }

    public List<KnowledgeSource<T>> getKnowledgeSources() {
        return Collections.unmodifiableList(knowledgeSources);
    }

    public void addKnowledgeSource(KnowledgeSource<T> knowledgeSource) {
        if (knowledgeSource != null)
            knowledgeSources.add(knowledgeSource);
    }

    public List<DependencyStrategy<T>> getDependencyStrategies() {
        return Collections.unmodifiableList(dependencyStrategies);
    }

    public void addDependencyStrategy(DependencyStrategy<T> dependencyStrategy) {
        dependencyStrategies.add(dependencyStrategy);
    }

    // _________________________________________________________________________________________________________________

    public Path getRootPath() {
        return rootPath;
    }

    public Path getSubPath() {
        return subPath;
    }

    // _________________________________________________________________________________________________________________

    public void setTestFilter(TestFilter<String, String> testFilter) {
        if (testFilter != null)
            Log.log(FINE, "Using test filter: " + testFilter + "\n");

        this.testFilter = testFilter;
    }

    public TestFilter<String, String> getTestFilter() {
        return testFilter;
    }

    // _________________________________________________________________________________________________________________

    public void setChecksumsNodes(Map<String, Integer> checksumsNodes) {
        if (checksumsNodes != null)
            Log.log(FINEST, "Checksums:\n" +
                    checksumsNodes.entrySet().stream()
                            .map(e -> e.getKey() + ": " + e.getValue())
                            .collect(Collectors.joining("\n")) +
                    "\n");

        this.checksumsNodes = checksumsNodes;
    }

    public Map<String, Integer> getChecksumsNodes() {
        return Collections.unmodifiableMap(checksumsNodes);
    }

    public void setCompilationUnitMapping(Map<String, String> compilationUnitMapping) {
        if (compilationUnitMapping != null)
            Log.log(ALL, "Mapping of Nodes to CompilationUnits:\n" +
                    compilationUnitMapping.entrySet().stream()
                            .map(e -> e.getKey() + " -> " + e.getValue())
                            .collect(Collectors.joining("\n")) +
                    "\n");

        this.compilationUnitMapping = compilationUnitMapping;
    }

    public Map<String, String> getCompilationUnitMapping() {
        return Collections.unmodifiableMap(compilationUnitMapping);
    }

    // _________________________________________________________________________________________________________________

    public CombinedTypeSolver getTypeSolver() {
        return typeSolver;
    }

    public void setTypeSolver(CombinedTypeSolver typeSolver) {
        this.typeSolver = typeSolver;
    }

    // _________________________________________________________________________________________________________________

    public void setCompilationUnits(Collection<CompilationUnit> compilationUnits) {
        if (compilationUnits != null)
            Log.log(ALL, "CompilationUnits in new Revision:\n" +
                    compilationUnits.stream()
                            .map(CompilationUnit::getPrimaryTypeName)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.joining("\n")) +
                    "\n");

        this.compilationUnits = compilationUnits;
    }

    public Collection<CompilationUnit> getCompilationUnits() {
        return Collections.unmodifiableCollection(compilationUnits);
    }

    // _________________________________________________________________________________________________________________

    public void setChangesNodes(Map<String, Node> nodesSame,
                                Map<String, Node> nodesDifferent,
                                Map<String, Node> nodesAdded,
                                Map<String, Integer> nodesRemoved) {
        if (nodesSame != null && nodesDifferent != null && nodesRemoved != null) {
            Log.log(FINEST, "Nodes that have not been modified:\n" +
                    String.join("\n", nodesSame.keySet()));
            Log.log(FINE, "Nodes that have been modified:\n" +
                    String.join("\n", nodesDifferent.keySet()));
            Log.log(FINE, "Nodes that have been added:\n" +
                    String.join("\n", nodesAdded.keySet()));
            Log.log(FINE, "Nodes that have been removed:\n" +
                    String.join("\n", nodesRemoved.keySet()));
        }

        this.nodesSame = nodesSame;
        this.nodesDifferent = nodesDifferent;
        this.nodesAdded = nodesAdded;
        this.nodesRemoved = nodesRemoved;

        this.allNodes = new HashMap<>();
        allNodes.putAll(nodesSame);
        allNodes.putAll(nodesAdded);
        allNodes.putAll(nodesDifferent);
    }

    public Map<String, Node> getNodesSame() {
        return Collections.unmodifiableMap(nodesSame);
    }

    public Map<String, Node> getNodesDifferent() {
        return Collections.unmodifiableMap(nodesDifferent);
    }

    public Map<String, Node> getNodesAdded() {
        return Collections.unmodifiableMap(nodesAdded);
    }

    public Map<String, Integer> getNodesRemoved() {
        return Collections.unmodifiableMap(nodesRemoved);
    }

    public Map<String, Node> getAllNodes() {
        return Collections.unmodifiableMap(allNodes);
    }

    // _________________________________________________________________________________________________________________

    public void setNameMapperNodes(Map<String, String> nameMapperNodes) {
        if (nameMapperNodes != null)
            Log.log(FINEST, "Identified mappings:\n" +
                    nameMapperNodes.entrySet().stream()
                            .map(e -> e.getKey() + "->" + e.getValue())
                            .collect(Collectors.joining("\n")));

        this.nameMapperNodes = nameMapperNodes;
    }

    public Map<String, String> getNameMapperNodes() {
        return Collections.unmodifiableMap(nameMapperNodes);
    }

    // _________________________________________________________________________________________________________________

    public void setTests(Collection<String> tests) {
        if (nameMapperNodes != null)
            Log.log(FINE, "Identified tests:\n" + String.join("\n", tests));

        this.tests = tests;
    }

    public Collection<String> getTests() {
        return tests;
    }

    // _________________________________________________________________________________________________________________

    public void setImpactedTypes(Collection<TypeDeclaration<?>> impactedTypes) {
        Log.log(INFO, "Recalculating primary dependencies of:\n" +
                impactedTypes.stream()
                        .map(Names::lookup)
                        .map(Pair::getFirst)
                        .collect(Collectors.joining("\n")) +
                "\n");

        this.impactedTypes = impactedTypes;
    }

    public Collection<TypeDeclaration<?>> getImpactedTypes() {
        return impactedTypes;
    }

    // _________________________________________________________________________________________________________________

    public void setGraphOldRevision(DependencyGraph graphOldRevision) {
        this.graphOldRevision = graphOldRevision;
    }

    public DependencyGraph getDependencyGraphOldRevision() {
        return this.graphOldRevision;
    }

    public void setGraphNewRevision(DependencyGraph graphNewRevision) {
        this.graphNewRevision = graphNewRevision;
    }

    public DependencyGraph getDependencyGraphNewRevision() {
        return this.graphNewRevision;
    }

    // _________________________________________________________________________________________________________________

    public void setCombinedGraph(ModificationGraph modificationGraph) {
        this.modificationGraph = modificationGraph;
    }

    public ModificationGraph getCombinedGraph() {
        return modificationGraph;
    }

    // _________________________________________________________________________________________________________________


    public void setInheritanceIdentifierVisitorMap(
            Map<TypeDeclaration<?>, InheritanceIdentifierVisitor> inheritanceIdentifierVisitorMap) {
        this.inheritanceIdentifierVisitorMap = inheritanceIdentifierVisitorMap;
    }

    public Map<TypeDeclaration<?>, InheritanceIdentifierVisitor> getInheritanceIdentifierVisitorMap() {
        return inheritanceIdentifierVisitorMap;
    }
}
