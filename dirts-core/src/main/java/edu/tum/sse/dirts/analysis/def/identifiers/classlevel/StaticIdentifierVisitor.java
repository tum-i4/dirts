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
package edu.tum.sse.dirts.analysis.def.identifiers.classlevel;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import edu.tum.sse.dirts.analysis.AbstractIdentifierVisitor;
import edu.tum.sse.dirts.util.Log;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static java.util.logging.Level.FINEST;

public class StaticIdentifierVisitor extends AbstractIdentifierVisitor<
        Collection<ResolvedTypeDeclaration>
        > {

    /*
     * This class is part of the algorithm from AutoRTS
     * (J. Öqvist, G. Hedin, and B. Magnusson, “Extraction-based regression test selection,”
     * ACM Int. Conf. Proceeding Ser., vol. Part F1284, 2016, doi: 10.1145/2972206.2972224)
     */

    //##################################################################################################################
    // Singleton pattern

    private static final StaticIdentifierVisitor singleton = new StaticIdentifierVisitor();

    private StaticIdentifierVisitor() {
    }

    public static Collection<ResolvedTypeDeclaration> identifyDependencies(TypeDeclaration<?> n) {
        Collection<ResolvedTypeDeclaration> arg = new HashSet<>();
        n.getMembers().forEach(m -> m.accept(singleton, arg));
        return arg;
    }

    //##################################################################################################################
    // Visitor pattern

    @Override
    public void visit(MethodCallExpr n, Collection<ResolvedTypeDeclaration> arg) {
        super.visit(n, arg);

        try {
            ResolvedMethodDeclaration resolvedMethodDeclaration = n.resolve();
            if (resolvedMethodDeclaration.isStatic()) {
                arg.add(resolvedMethodDeclaration.declaringType());
            }
        } catch (Throwable e) {
            Log.log(FINEST, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Override
    public void visit(FieldAccessExpr n, Collection<ResolvedTypeDeclaration> arg) {
        super.visit(n, arg);

        try {
            ResolvedValueDeclaration resolvedValueDeclaration = n.resolve();

            if (resolvedValueDeclaration.isField()) {
                ResolvedFieldDeclaration resolvedFieldDeclaration = resolvedValueDeclaration.asField();
                if (resolvedFieldDeclaration.isStatic()) {
                    arg.add(resolvedFieldDeclaration.declaringType());
                }
            }
        } catch (Throwable e) {
            Log.log(FINEST, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Override
    public void visit(NameExpr n, Collection<ResolvedTypeDeclaration> arg) {
        super.visit(n, arg);

        try {
            ResolvedValueDeclaration resolvedValueDeclaration = n.resolve();
            if (resolvedValueDeclaration.isField()) {
                ResolvedFieldDeclaration resolvedFieldDeclaration = resolvedValueDeclaration.asField();
                if (resolvedFieldDeclaration.isStatic()) {
                    arg.add(resolvedFieldDeclaration.declaringType());
                }
            }
            if (resolvedValueDeclaration.isEnumConstant()) {
                ResolvedEnumConstantDeclaration resolvedEnumConstantDeclaration = resolvedValueDeclaration.asEnumConstant();
                ResolvedReferenceType resolvedEnumType = resolvedEnumConstantDeclaration.getType().asReferenceType();
                Optional<ResolvedReferenceTypeDeclaration> enumTypeDeclaration = resolvedEnumType.getTypeDeclaration();
                enumTypeDeclaration.ifPresent(arg::add);
            }
        } catch (Throwable e) {
            Log.log(FINEST, "Exception in " + this.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
