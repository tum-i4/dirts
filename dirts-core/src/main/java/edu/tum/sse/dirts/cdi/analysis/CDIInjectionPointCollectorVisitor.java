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
package edu.tum.sse.dirts.cdi.analysis;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractTruncatedVisitor;
import edu.tum.sse.dirts.analysis.di.InjectionPointCollector;
import edu.tum.sse.dirts.analysis.di.InjectionPointStorage;
import edu.tum.sse.dirts.cdi.util.CDIUtil;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.util.Log;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import static edu.tum.sse.dirts.analysis.di.NameIdentifierVisitor.getNameFromQualifier;
import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;

/**
 * Collects injectionPoints induced by CDI
 */
public abstract class CDIInjectionPointCollectorVisitor<P extends BodyDeclaration<?>>
        extends AbstractTruncatedVisitor<InjectionPointStorage>
        implements InjectionPointCollector<P> {

    //##################################################################################################################
    // Methods specified by InjectionPointCollector

    public void collectInjectionPoints(Collection<TypeDeclaration<?>> ts, InjectionPointStorage injectionPoints) {
        for (TypeDeclaration<?> t : ts) {
            t.accept(this, injectionPoints);
        }
    }

    //##################################################################################################################
    // Methods used by subclasses

    protected <T extends Node & NodeWithAnnotations<?>, R> void handleInjectionVariable(
            InjectionPointStorage injectionPoints,
            R node,
            Function<R, String> getterNodeName,
            T injectionVariable,
            Function<T, ResolvedType> getterType) {

        String nodeName = null;
        ResolvedType type = null;

        // Name
        String name = getNameFromQualifier(injectionVariable, "Named");

        // Qualifiers
        Set<String> qualifiers = CDIUtil.findQualifiers(injectionVariable);

        try {
            // Type
            type = getterType.apply(injectionVariable);

            nodeName = getterNodeName.apply(node);
        } catch (RuntimeException e) {
            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }

        if (nodeName != null) {
            // add injectionPoints
            injectionPoints.addInjectionPoint(nodeName, type, name, qualifiers);
        }
    }

    protected <R> void handleSelect(InjectionPointStorage injectionPoints,
                                    R node,
                                    Function<R, String> getterNodeName,
                                    Set<Pair<Set<String>, ResolvedType>> pairs) {
        // Injection through Instance
        if (!pairs.isEmpty()) {
            String nodeName = getterNodeName.apply(node);
            for (Pair<Set<String>, ResolvedType> pair : pairs) {
                Set<String> qualifiers = pair.getFirst();
                ResolvedType type = pair.getSecond();

                injectionPoints.addInjectionPoint(nodeName, type, null, qualifiers);
            }
        }
    }
}
