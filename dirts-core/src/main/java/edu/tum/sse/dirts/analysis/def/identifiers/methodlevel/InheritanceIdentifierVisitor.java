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
package edu.tum.sse.dirts.analysis.def.identifiers.methodlevel;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.util.JavaParserUtils;
import edu.tum.sse.dirts.util.Log;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.logging.Level.FINE;

/**
 * Identifies inherited methods
 * Considers only MethodDeclaration (since Constructors are not inherited)
 */
public class InheritanceIdentifierVisitor extends AbstractIdentifierVisitor<
        Collection<ResolvedMethodLikeDeclaration>
        > {

    /*
    Since the constructor of this class has a parameter, it cannot implement the singleton pattern
     */

    //##################################################################################################################
    // Attributes

    private final Map<String, Set<ResolvedMethodDeclaration>> inheritedMethods;

    //##################################################################################################################
    // Constructors

    public InheritanceIdentifierVisitor(TypeDeclaration<?> typeDeclaration) {

        inheritedMethods = new HashMap<>();
        try {
            ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = typeDeclaration.resolve();
            resolvedReferenceTypeDeclaration.getAllAncestors(JavaParserUtils.depthFirstFuncAcceptIncompleteList)
                    .stream()
                    .filter(t -> !t.isJavaLangObject())
                    .flatMap(resolvedReferenceType -> resolvedReferenceType.getAllMethodsVisibleToInheritors().stream())
                    .filter(m -> !m.declaringType().isJavaLangObject())
                    .forEach(m -> {
                        try {
                            String signature = m.getSignature();
                            if (!inheritedMethods.containsKey(signature))
                                inheritedMethods.put(signature, new HashSet<>());
                            Set<ResolvedMethodDeclaration> resolvedMethodLikeDeclarations =
                                    inheritedMethods.get(signature);
                            resolvedMethodLikeDeclarations.add(m);
                        } catch (UnsolvedSymbolException ignored) {
                        }
                    });
        } catch (RuntimeException e) {
            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    //##################################################################################################################
    // Visitor Pattern

    @Override
    public void visit(MethodDeclaration n, Collection<ResolvedMethodLikeDeclaration> arg) {
        try {
            ResolvedMethodLikeDeclaration resolvedMethodDeclaration = n.resolve();
            String thisSignature = resolvedMethodDeclaration.getSignature();

            List<ResolvedMethodLikeDeclaration> overriddenMethods = inheritedMethods.entrySet()
                    .stream()
                    .filter(e -> {
                        try {
                            String iSignature = e.getKey();
                            return thisSignature.equals(iSignature);
                        } catch (RuntimeException ignored) {
                            return false;
                        }
                    })
                    .map(Map.Entry::getValue)
                    .flatMap(Set::stream)
                    .collect(Collectors.toList());
            arg.addAll(overriddenMethods);
            inheritedMethods.remove(thisSignature);
        } catch (RuntimeException e) {
            Log.log(FINE, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public Map<String, Set<ResolvedMethodDeclaration>> getInheritedMethods() {
        return inheritedMethods;
    }
}