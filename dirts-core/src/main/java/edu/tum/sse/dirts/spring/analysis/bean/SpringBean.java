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
package edu.tum.sse.dirts.spring.analysis.bean;

import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import edu.tum.sse.dirts.analysis.di.Bean;
import edu.tum.sse.dirts.spring.util.SpringNames;
import edu.tum.sse.dirts.util.alternatives.TriAlternative;
import edu.tum.sse.dirts.util.alternatives.TriFirstOption;
import edu.tum.sse.dirts.util.alternatives.TriSecondOption;
import edu.tum.sse.dirts.util.alternatives.TriThirdOption;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Represents a bean in Spring
 * Can be either a method, a type or a xml bean definition
 */
public class SpringBean implements Bean {

    //##################################################################################################################
    // Attributes

    private final TriAlternative<XMLBeanDefinition,
            ResolvedMethodDeclaration,
            ResolvedReferenceTypeDeclaration> definition;

    //##################################################################################################################
    // Constructors

    public SpringBean(XMLBeanDefinition XMLBeanDefinition) {
        this.definition = new TriFirstOption<>(XMLBeanDefinition);
    }

    public SpringBean(ResolvedMethodDeclaration declaringMethod) {
        this.definition = new TriSecondOption<>(declaringMethod);
    }

    public SpringBean(ResolvedReferenceTypeDeclaration referenceType) {
        this.definition = new TriThirdOption<>(referenceType);
    }

    //##################################################################################################################
    // Getters

    public TriAlternative<XMLBeanDefinition, ResolvedMethodDeclaration, ResolvedReferenceTypeDeclaration> getDefinition() {
        return definition;
    }

    @Override
    public String toString() {
        if (definition.isFirstOption()) {
            return SpringNames.lookup(definition.getAsFirstOption());
        } else if (definition.isSecondOption()) {
            return lookup(definition.getAsSecondOption());
        } else {
            return lookup(definition.getAsThirdOption());
        }
    }
}