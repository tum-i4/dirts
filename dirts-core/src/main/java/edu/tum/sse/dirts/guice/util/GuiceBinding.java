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
package edu.tum.sse.dirts.guice.util;

import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import edu.tum.sse.dirts.analysis.di.Bean;
import edu.tum.sse.dirts.util.alternatives.TriAlternative;
import edu.tum.sse.dirts.util.alternatives.TriFirstOption;
import edu.tum.sse.dirts.util.alternatives.TriSecondOption;
import edu.tum.sse.dirts.util.alternatives.TriThirdOption;
import edu.tum.sse.dirts.util.naming_scheme.Names;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.Set;
import java.util.stream.Collectors;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Represents a binding in Guice
 * Can be either a method, a type or a combination of a method and several types
 */
public class GuiceBinding implements Bean {

    private final TriAlternative<ResolvedMethodLikeDeclaration,
            ResolvedReferenceTypeDeclaration,
            Pair<ResolvedMethodDeclaration, Set<ResolvedReferenceTypeDeclaration>>> source;

    public GuiceBinding(ResolvedMethodLikeDeclaration resolvedMethodLikeDeclaration) {
        source = new TriFirstOption<>(resolvedMethodLikeDeclaration);
    }

    public GuiceBinding(ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration) {
        source = new TriSecondOption<>(resolvedReferenceTypeDeclaration);
    }

    public GuiceBinding(Pair<ResolvedMethodDeclaration, Set<ResolvedReferenceTypeDeclaration>> pair) {
        source = new TriThirdOption<>(pair);
    }

    public TriAlternative<ResolvedMethodLikeDeclaration, ResolvedReferenceTypeDeclaration, Pair<ResolvedMethodDeclaration, Set<ResolvedReferenceTypeDeclaration>>> getSource() {
        return source;
    }

    @Override
    public String toString() {
        if (source.isFirstOption()){
            return lookup(source.getAsFirstOption());
        } else if (source.isSecondOption()) {
            return lookup(source.getAsSecondOption());
        } else {
            Pair<ResolvedMethodDeclaration, Set<ResolvedReferenceTypeDeclaration>> pair = source.getAsThirdOption();
            return "(" + lookup(pair.getFirst()) + " [" + pair.getSecond().stream().map(Names::lookup).collect(Collectors.joining(", ")) + "])";
        }
    }
}
