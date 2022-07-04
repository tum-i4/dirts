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
package edu.tum.sse.dirts.analysis.def.identifiers.type;

import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.core.control.Control;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

public class NewIdentifierVisitor extends AbstractIdentifierVisitor<
        Collection<ResolvedReferenceTypeDeclaration>
        > {

    /*
     * This class is part of the algorithm from AutoRTS
     * (J. Öqvist, G. Hedin, and B. Magnusson, “Extraction-based regression test selection,”
     * ACM Int. Conf. Proceeding Ser., vol. Part F1284, 2016, doi: 10.1145/2972206.2972224)
     */

    //##################################################################################################################
    // Singleton pattern

    private static final NewIdentifierVisitor singleton = new NewIdentifierVisitor();

    private NewIdentifierVisitor() {
    }

    public static Collection<ResolvedReferenceTypeDeclaration> identifyDependencies(TypeDeclaration<?> n) {
        Collection<ResolvedReferenceTypeDeclaration> arg = new HashSet<>();
        n.getMembers().forEach(m -> m.accept(singleton, arg));
        return arg;
    }

    //##################################################################################################################
    // Visitor pattern

    @Override
    public void visit(ObjectCreationExpr n, Collection<ResolvedReferenceTypeDeclaration> arg) {
        super.visit(n, arg);

        try {
            ResolvedReferenceType resolvedReferenceType = n.getType().resolve().asReferenceType();
            Optional<ResolvedReferenceTypeDeclaration> typeDeclaration = resolvedReferenceType.getTypeDeclaration();
            typeDeclaration.ifPresent(arg::add);
        } catch (RuntimeException e) {
            if (Control.DEBUG)
                System.out.println("Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
