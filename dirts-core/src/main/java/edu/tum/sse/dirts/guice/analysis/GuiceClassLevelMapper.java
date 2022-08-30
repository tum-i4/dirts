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
package edu.tum.sse.dirts.guice.analysis;

import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import edu.tum.sse.dirts.guice.util.GuiceBinding;
import edu.tum.sse.dirts.util.alternatives.TriAlternative;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.HashSet;
import java.util.Set;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

/**
 * Used to map Guice beans to the names of corresponding class level nodes
 */
public class GuiceClassLevelMapper extends GuiceMapper<TypeDeclaration<?>> {
    @Override
    public Set<String> mapToString(GuiceBinding guiceBinding) {
        TriAlternative<ResolvedMethodLikeDeclaration,
                ResolvedReferenceTypeDeclaration,
                Pair<ResolvedMethodDeclaration, Set<ResolvedReferenceTypeDeclaration>>> source = guiceBinding.getSource();


        Set<String> ret = new HashSet<>();

        String toNode;
        if (source.isFirstOption()) {
            ResolvedMethodLikeDeclaration resolvedMethodLikeDeclaration = source.getAsFirstOption();
            toNode = lookup(resolvedMethodLikeDeclaration.declaringType());
            ret.add(toNode);
        } else if (source.isSecondOption()) {
            ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = source.getAsSecondOption();
            toNode = lookup(resolvedReferenceTypeDeclaration);
            ret.add(toNode);
        } else {
            Pair<ResolvedMethodDeclaration, Set<ResolvedReferenceTypeDeclaration>> pair = source.getAsThirdOption();
            ResolvedMethodDeclaration methodWhereBound = pair.getFirst();
            Set<ResolvedReferenceTypeDeclaration> resolvedReferenceTypeDeclarations = pair.getSecond();

            // Add edge to the class containing the method where the binding was created
            if (methodWhereBound != null) {
                String toMethodNode;
                toMethodNode = lookup(methodWhereBound.declaringType());
                ret.add(toMethodNode);
            }
            for (ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration : resolvedReferenceTypeDeclarations) {
                toNode = lookup(resolvedReferenceTypeDeclaration);
                ret.add(toNode);
            }
        }
        return ret;
    }
}
