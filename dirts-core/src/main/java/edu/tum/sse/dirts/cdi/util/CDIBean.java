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
package edu.tum.sse.dirts.cdi.util;

import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import edu.tum.sse.dirts.analysis.di.Bean;
import edu.tum.sse.dirts.util.alternatives.*;
import edu.tum.sse.dirts.util.naming_scheme.Names;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Represents a bean in CDI
 * Can be either a method, a field (in form of a ResolvedFieldDeclaration or a ResolvedValueDeclaration) or a type
 */
public class CDIBean implements Bean {

    //##################################################################################################################
    // Attributes

    private final QuadAlternative<ResolvedReferenceTypeDeclaration,
                Set<ResolvedMethodDeclaration>,
                ResolvedFieldDeclaration,
                ResolvedValueDeclaration> source;

    //##################################################################################################################
    // Constructors

    public CDIBean(ResolvedReferenceTypeDeclaration beanDeclaration) {
        this.source = new QuadFirstOption<>(beanDeclaration);
    }

    public CDIBean(ResolvedMethodDeclaration beanMethod, Set<ResolvedMethodDeclaration> disposerMethods) {
        Set<ResolvedMethodDeclaration> set = new HashSet<>(disposerMethods);
        set.add(beanMethod);
        this.source = new QuadSecondOption<>(set);
    }

    public CDIBean(ResolvedFieldDeclaration beanField) {
        this.source = new QuadThirdOption<>(beanField);
    }

    public CDIBean(ResolvedValueDeclaration beanField) {
        this.source = new QuadFourthOption<>(beanField);
    }

    //##################################################################################################################
    // Getters

    public QuadAlternative<ResolvedReferenceTypeDeclaration,
            Set<ResolvedMethodDeclaration>,
            ResolvedFieldDeclaration,
            ResolvedValueDeclaration> getSource() {
        return source;
    }

    @Override
    public String toString() {
        if (source.isFirstOption()) {
            return lookup(source.getAsFirstOption());
        } else if (source.isSecondOption()) {
            return "[" + source.getAsSecondOption().stream()
                    .map(Names::lookup)
                    .collect(Collectors.joining(", ")) + "]";
        } else if (source.isThirdOption()) {
            ResolvedFieldDeclaration field = source.getAsThirdOption();
            return lookup(field.declaringType(), field);
        } else {
            ResolvedValueDeclaration field = source.getAsFourthOption();
            return lookup(field.asField().declaringType(), field.asField());
        }
    }
}
