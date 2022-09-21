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
package edu.tum.sse.dirts.core.knowledgesources.graph_cropper;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import edu.tum.sse.dirts.analysis.AbstractTruncatedVisitor;
import edu.tum.sse.dirts.analysis.FinderVisitor;
import edu.tum.sse.dirts.analysis.def.finders.TypeFinderVisitor;
import edu.tum.sse.dirts.analysis.def.identifiers.methodlevel.InheritanceIdentifierVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.graph.EdgeType;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.core.BlackboardState.NODES_CHANGES_SET;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Prepares the graph before analyzing dependencies
 * Calculates which dependencies need to be reanalyzed
 * <p>
 * The concept used in this class can be found in J. Öqvist et al., Extraction-Based Regression Test Selection, 2016,
 * but has been adapted and extended towards method level nodes
 */
public class MethodLevelGraphCropper extends AbstractGraphCropper<BodyDeclaration<?>> {

    public MethodLevelGraphCropper(Blackboard<BodyDeclaration<?>> blackboard,
                                   FinderVisitor<Map<String, Node>, BodyDeclaration<?>> finderVisitor,
                                   Set<EdgeType> affectedEdges,
                                   Predicate<Node> nodesFilter) {
        super(blackboard, finderVisitor, affectedEdges, nodesFilter);
    }

    @Override
    public Collection<TypeDeclaration<?>> calculateImpactedTypeDeclarations(
            DependencyGraph dependencyGraph,
            Collection<CompilationUnit> compilationUnits,
            Map<String, Node> nodesAdded,
            Map<String, Integer> nodesRemoved,
            Map<String, Node> nodesDifferent,
            Map<String, Node> nodesSame
    ) {

        Map<String, Node> allNodes = blackboard.getAllNodes();
        Map<String, String> compilationUnitMapping = blackboard.getCompilationUnitMapping();

        // Compute the set of CompilationUnits that require a recalculation of dependencies
        HashSet<CompilationUnit> impactedCompilationUnits = new HashSet<>();

        // In method level RTS it may be that removed or renamed nodes are pointed to by other nodes that need not change
        // However, the dependencies of these nodes may change and need to be recalculated
        nodesRemoved.keySet().forEach(name -> {
            Set<String> affectedNodesNames = dependencyGraph.removeAllEdgesTo(name, affectedEdges);
            Set<Node> affectedNodes = allNodes.entrySet().stream()
                    .filter(e -> affectedNodesNames.contains(e.getKey()))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toSet());
            impactedCompilationUnits.addAll(affectedNodes.stream()
                    .map(Node::findCompilationUnit)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet()));
        });

        // Add all CompilationUnits that contained removed nodes (if they are not removed entirely)
        impactedCompilationUnits.addAll(nodesRemoved.keySet()
                .stream().map(compilationUnitMapping::get)
                .filter(Objects::nonNull)
                .map(allNodes::get)
                .filter(Objects::nonNull)
                .map(Node::findCompilationUnit)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet()));

        // Add all CompilationUnits from modified nodes
        impactedCompilationUnits.addAll(nodesDifferent.values().stream()
                .map(Node::findCompilationUnit)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet()));

        // Add all CompilationUnits from added nodes
        impactedCompilationUnits.addAll(nodesAdded.values().stream()
                .map(Node::findCompilationUnit)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet()));

        // Add all CompilationUnits from packages that have at least one added or renamed CompilationUnit
        // theory by Öqvist et al.
        {
            // Add all added nodes that correspond to a CompilationUnit
            Set<String> affectedPackagesNodes = new HashSet<>();
            affectedPackagesNodes.addAll(nodesAdded.values().stream()
                    .filter(n -> n instanceof CompilationUnit)
                    .map(n -> ((CompilationUnit) n).getPackageDeclaration())
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(NodeWithName::getNameAsString)
                    .collect(Collectors.toSet()));

            impactedCompilationUnits.addAll(compilationUnits.stream()
                    .filter(cu -> cu.getPackageDeclaration()
                            .filter(declaration -> affectedPackagesNodes.contains(declaration.getNameAsString()))
                            .isPresent())
                    .collect(Collectors.toSet()));
        }

        List<TypeDeclaration<?>> typeDeclarations = new ArrayList<>();
        TypeFinderVisitor typeFinderVisitor = new TypeFinderVisitor();
        impactedCompilationUnits.forEach(cu -> cu.accept(typeFinderVisitor, typeDeclarations));

        IdentityHashMap<TypeDeclaration<?>, InheritanceIdentifierVisitor> inheritanceIdentifierVisitorMap = new IdentityHashMap<>();
        Set<CompilationUnit> impactedCompilationUnits3 = typeDeclarations.stream()
                .flatMap(t -> {
                    InheritanceIdentifierVisitor inheritanceIdentifierVisitor = new InheritanceIdentifierVisitor(t);
                    inheritanceIdentifierVisitorMap.put(t, inheritanceIdentifierVisitor);

                    // find all methods that could possibly be overridden
                    return inheritanceIdentifierVisitor.getInheritedMethods().values().stream();
                })
                .flatMap(Set::stream)
                // find all nodes that delegate to potentially overridden methods
                .flatMap(m -> dependencyGraph.removeAllEdgesTo(lookup(m), Set.of(EdgeType.DELEGATION)).stream()
                        .map(allNodes::get)
                        .filter(Objects::nonNull))
                .map(Node::findCompilationUnit)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(cu -> !impactedCompilationUnits.contains(cu))
                .collect(Collectors.toSet());

        impactedCompilationUnits3.forEach(cu -> cu.accept(typeFinderVisitor, typeDeclarations));


        blackboard.setInheritanceIdentifierVisitorMap(inheritanceIdentifierVisitorMap);

        AbstractTruncatedVisitor<Set<String>> methodOrFieldFinder = new AbstractTruncatedVisitor<>() {
            @Override
            public void visit(MethodDeclaration n, Set<String> arg) {
                //super.visit(n, arg);
                arg.add(lookup(n).getFirst());
            }

            @Override
            public void visit(FieldDeclaration n, Set<String> arg) {
                //super.visit(n, arg);
                arg.add(lookup(n).getFirst());
            }
        };

        Set<String> methodNodes = new HashSet<>();
        typeDeclarations.forEach(t -> t.accept(methodOrFieldFinder, methodNodes));

        Set<CompilationUnit> impactedCompilationUnits2 = methodNodes.stream()
                .flatMap(methodNode -> dependencyGraph.removeAllEdgesFrom(methodNode, Set.of(EdgeType.INHERITANCE, EdgeType.FIELD_ASSIGNMENT)).stream())
                .map(allNodes::get)
                .filter(Objects::nonNull)
                .map(Node::findCompilationUnit)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(unit -> !impactedCompilationUnits.contains(unit))
                .collect(Collectors.toSet());
        impactedCompilationUnits2.forEach(cu -> cu.accept(typeFinderVisitor, typeDeclarations));

        typeDeclarations.forEach(t -> {
            inheritanceIdentifierVisitorMap.computeIfAbsent(t, InheritanceIdentifierVisitor::new);
        });

        return typeDeclarations;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == NODES_CHANGES_SET;
    }
}
