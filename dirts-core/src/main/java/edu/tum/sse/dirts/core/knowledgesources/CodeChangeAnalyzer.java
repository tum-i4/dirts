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
package edu.tum.sse.dirts.core.knowledgesources;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import edu.tum.sse.dirts.analysis.FinderVisitor;
import edu.tum.sse.dirts.analysis.def.checksum.ChecksumVisitor;
import edu.tum.sse.dirts.core.Blackboard;
import edu.tum.sse.dirts.core.BlackboardState;
import edu.tum.sse.dirts.core.KnowledgeSource;
import edu.tum.sse.dirts.core.strategies.DependencyStrategy;
import edu.tum.sse.dirts.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.core.BlackboardState.NODES_CHANGES_SET;
import static edu.tum.sse.dirts.core.BlackboardState.TESTS_FOUND;
import static java.util.logging.Level.WARNING;

/**
 * Partitions code objects into four categories: same, added, removed, changed
 */
public class CodeChangeAnalyzer<T extends BodyDeclaration<?>> extends KnowledgeSource<T> {

    //##################################################################################################################
    // Attributes

    private final Blackboard<T> blackboard;
    private final ChecksumVisitor<T> checksumVisitor;
    private final FinderVisitor<Map<String, Node>, T> nameFinderVisitor;


    //##################################################################################################################
    // Constructors

    public CodeChangeAnalyzer(Blackboard<T> blackboard,
                              FinderVisitor<Map<String, Node>, T> nameFinderVisitor,
                              ChecksumVisitor<T> checksumVisitor
    ) {
        super(blackboard);
        this.blackboard = blackboard;
        this.checksumVisitor = checksumVisitor;
        this.nameFinderVisitor = nameFinderVisitor;
    }

    //##################################################################################################################
    // Methods

    @Override
    public BlackboardState updateBlackboard() {

        Map<String, Node> sameCode = new HashMap<>();
        Map<String, Node> differentCode = new HashMap<>();
        Map<String, Node> added = new HashMap<>();
        Map<String, Integer> removed = new HashMap<>();

        Map<String, String> nameMapper = new HashMap<>();

        Map<String, Node> allObjects = new HashMap<>();

        Collection<CompilationUnit> compilationUnits = blackboard.getCompilationUnits();
        compilationUnits.forEach(cu -> cu.accept(nameFinderVisitor, allObjects));

        calculateChange(
                blackboard.getChecksumsNodes(),
                checksumVisitor::hashCode,
                allObjects,
                sameCode,
                differentCode,
                added,
                removed,
                nameMapper);

        blackboard.setChangesNodes(sameCode, differentCode, added, removed);
        blackboard.setNameMapperNodes(nameMapper);

        for (DependencyStrategy<T> dependencyStrategy : blackboard.getDependencyStrategies()) {
            dependencyStrategy.doChangeAnalysis(blackboard);
        }

        return NODES_CHANGES_SET;
    }

    @Override
    public boolean executeCondition() {
        return blackboard.getState() == TESTS_FOUND;
    }

    public static <T> void calculateChange(
            Map<String, Integer> checksumsOldRevision,
            Function<T, Integer> checksumFunction,
            Map<String, T> allObjects,
            Map<String, T> objectsSame,
            Map<String, T> objectsDifferent,
            Map<String, T> objectsAdded,
            Map<String, Integer> objectsRemoved,
            Map<String, String> nameMapping) {


        objectsAdded.putAll(allObjects);

        //noinspection StatementWithEmptyBody
        if (checksumsOldRevision != null) {
            objectsRemoved.putAll(checksumsOldRevision);

            // We might have entries with no T (in this case T is MethodDeclaration) coming from extended test classes
            // See NonTypeNameFinderVisitor.visit(ClassOrInterfaceDeclaration n, ...)
            // and JUnitNonTypeDependencyCollectorVisitor.visit(ClassOrInterfaceDeclaration n, ...)

            filterNonNull(objectsAdded, objectsSame);
            filterNonNull(objectsRemoved, objectsSame);

            // check for same names
            for (Map.Entry<String, T> tNew : objectsAdded.entrySet()) {
                String name = tNew.getKey();
                Integer matchingOldT = objectsRemoved.getOrDefault(name, null);
                if (matchingOldT != null) {
                    if (checksumFunction.apply(tNew.getValue()).equals(matchingOldT)) {
                        // same Name, same Code
                        objectsSame.put(name, tNew.getValue());
                    } else {
                        // same Name, different Code
                        objectsDifferent.put(name, tNew.getValue());
                    }

                    // remove old class from active set
                    objectsRemoved.remove(name);
                }
            }

            // remove new ts from active set
            objectsSame.forEach(objectsAdded::remove);
            objectsDifferent.forEach(objectsAdded::remove);

            // same Code, different Name but able to clearly identify source
            for (Map.Entry<String, T> tNew : objectsAdded.entrySet()) {
                String newName = tNew.getKey();

            /*
            We need to ensure there is only one t with this code in tsNew and tsOld
            Only then we can identify the source without ambiguity
             */

                Set<String> matchingOldT = objectsRemoved.entrySet().stream()
                        .filter(tOld -> tOld.getValue().equals(checksumFunction.apply(tNew.getValue())))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
                Set<String> matchingNewT = objectsAdded.entrySet().stream()
                        .filter(tNewOther -> checksumFunction.apply(tNewOther.getValue()).equals(checksumFunction.apply(tNew.getValue())))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
                if (matchingOldT.size() == 1 && matchingNewT.size() == 1) {
                    objectsSame.put(newName, tNew.getValue());

                    if (nameMapping != null)
                        nameMapping.put(matchingOldT.stream().findFirst().get(), newName);

                    // remove old class from active set
                    matchingOldT.forEach(objectsRemoved::remove);
                }
            }
            // remove new ts from active set
            objectsSame.forEach(objectsAdded::remove);
        } else {
            // there are no checksums from the old revision
            // nothing to do, all objects are already in objectsAdded
        }
    }

    private static <S, T> void filterNonNull(Map<String, S> mayContainNull, Map<String, T> objectsSame) {
        Set<String> valueIsNull = mayContainNull.entrySet().stream().filter(e -> e.getValue() == null)
                .map(Map.Entry::getKey).collect(Collectors.toSet());
        valueIsNull.forEach(mayContainNull::remove);
        valueIsNull.forEach(k -> objectsSame.put(k, null));
    }
}
