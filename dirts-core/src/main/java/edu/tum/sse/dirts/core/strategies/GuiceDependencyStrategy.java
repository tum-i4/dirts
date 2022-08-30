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
package edu.tum.sse.dirts.core.strategies;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.analysis.DependencyCollector;
import edu.tum.sse.dirts.analysis.di.Bean;
import edu.tum.sse.dirts.analysis.di.BeanStorage;
import edu.tum.sse.dirts.analysis.di.InjectionPointCollector;
import edu.tum.sse.dirts.graph.EdgeType;
import edu.tum.sse.dirts.guice.analysis.GuiceInjectionPointCollectorVisitor;
import edu.tum.sse.dirts.guice.analysis.GuiceMapper;
import edu.tum.sse.dirts.guice.analysis.identifiers.*;
import edu.tum.sse.dirts.guice.util.GuiceBinding;

import java.util.Collection;
import java.util.Set;

/**
 * Contains tasks required by the dependency-analyzing extension for Guice
 */
public class GuiceDependencyStrategy<T extends BodyDeclaration<?>>
        extends DIDependencyStrategy<T, GuiceBinding> {

    private static final String PREFIX = "guice";

    public GuiceDependencyStrategy(GuiceInjectionPointCollectorVisitor<T> injectionPointCollector,
                                   GuiceMapper<T> nameMapper) {
        super(PREFIX, injectionPointCollector, EdgeType.DI_GUICE, nameMapper);
    }

    @Override
    protected BeanStorage<GuiceBinding> collectBeans(Collection<TypeDeclaration<?>> ts) {

        BeanStorage<GuiceBinding> bindingsStorage = new BeanStorage<>();

        ProvidesIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        BindToIdentifier.identifyDependencies(ts, bindingsStorage);
        AutoBindSingletonIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        ProviderIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        JustInTimeIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        ImplementedByIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        ProvidedByIdentifierVisitor.identifyDependencies(ts, bindingsStorage);
        return bindingsStorage;
    }
}
