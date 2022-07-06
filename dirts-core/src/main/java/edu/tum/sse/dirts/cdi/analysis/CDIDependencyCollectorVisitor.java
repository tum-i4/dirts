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
package edu.tum.sse.dirts.cdi.analysis;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.resolution.types.ResolvedType;
import edu.tum.sse.dirts.analysis.AbstractTruncatedVisitor;
import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.analysis.di.NameIdentifierVisitor;
import edu.tum.sse.dirts.cdi.analysis.identifiers.ManagedBeanIdentifierVisitor;
import edu.tum.sse.dirts.cdi.analysis.identifiers.ProducerFieldIdentifierVisitor;
import edu.tum.sse.dirts.cdi.analysis.identifiers.ProducerMethodIdentifierVisitor;
import edu.tum.sse.dirts.cdi.analysis.identifiers.SelectIdentifierVisitor;
import edu.tum.sse.dirts.cdi.util.CDIBean;
import edu.tum.sse.dirts.cdi.util.CDIUtil;
import edu.tum.sse.dirts.graph.DependencyGraph;
import edu.tum.sse.dirts.util.Log;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.Collection;
import java.util.Set;

import static java.util.logging.Level.FINER;

public abstract class CDIDependencyCollectorVisitor extends AbstractTruncatedVisitor<DependencyGraph>
        implements DependencyCollector {


    //##################################################################################################################
    // Attributes

    protected BeanStorage<CDIBean> beanStorage;
    protected Set<String> alternatives;

    //##################################################################################################################
    // Setters

    public void setBeanStorage(BeanStorage<CDIBean> beanStorage) {
        this.beanStorage = beanStorage;
    }

    public void setAlternatives(Set<String> alternatives) {
        this.alternatives = alternatives;
    }

    //##################################################################################################################
    // Methods specified by DependencyCollector

    public void calculateDependencies(Collection<TypeDeclaration<?>> ts, DependencyGraph dependencyGraph) {
        ManagedBeanIdentifierVisitor.identifyDependencies(ts, beanStorage);
        ProducerFieldIdentifierVisitor.identifyDependencies(ts, beanStorage);
        ProducerMethodIdentifierVisitor.identifyDependencies(ts, beanStorage);

        for (TypeDeclaration<?> t : ts) {
            t.accept(this, dependencyGraph);
        }

        Log.log(FINER, "BEANS", "\n" + beanStorage.toString());
    }


    //##################################################################################################################
    // Methods required in subclasses

    protected abstract void processBeans(DependencyGraph arg,
                                         BodyDeclaration<?> node,
                                         Collection<CDIBean> candidateBeanMethods);

    //##################################################################################################################
    // Methods used by subclasses

    protected void getCandidates(Collection<CDIBean> candidates, NodeWithAnnotations<?> node, ResolvedType type) {
        // determine qualifiers
        Set<String> qualifiers = CDIUtil.findQualifiers(node);

        // determine name
        String name = NameIdentifierVisitor.getNameFromQualifier(node, "Named");

        candidates.addAll(beanStorage.getBeansForTypeAndNameAndQualifiers(type, name, qualifiers));
    }

    protected void handleSelect(Collection<CDIBean> candidates, BodyDeclaration<?> n) {
        // Injection through Instance
        Set<Pair<Set<String>, ResolvedType>> pairs = SelectIdentifierVisitor.collectSelectMethodCalls(n);
        for (Pair<Set<String>, ResolvedType> pair : pairs) {
            Set<String> qualifiers = pair.getFirst();
            ResolvedType type = pair.getSecond();
            candidates.addAll(beanStorage.getBeansForTypeAndQualifiers(type, qualifiers));
        }
    }
}
