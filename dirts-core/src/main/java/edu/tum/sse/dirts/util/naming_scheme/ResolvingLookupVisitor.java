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
package edu.tum.sse.dirts.util.naming_scheme;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.VoidVisitorWithDefaults;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.*;
import edu.tum.sse.dirts.util.Container;
import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import static edu.tum.sse.dirts.util.naming_scheme.Names.lookup;

class ResolvingLookupVisitor extends VoidVisitorWithDefaults<Pair<Container<String>, Container<Optional<String>>>> {

    //##################################################################################################################
    // Attributes

    private final Map<FieldDeclaration, Integer> fieldDeclarationMap = new IdentityHashMap<>();

    private final Map<InitializerDeclaration, Integer> initializerDeclarationMap = new IdentityHashMap<>();

    private final UnresolvedLookupVisitor unresolvedLookupVisitor;

    //##################################################################################################################
    // Constructors

    public ResolvingLookupVisitor(UnresolvedLookupVisitor unresolvedLookupVisitor) {
        this.unresolvedLookupVisitor = unresolvedLookupVisitor;
    }

    //##################################################################################################################
    // Methods

    private static <N> String getCustomName(Map<N, Integer> nMap, N n) {
        if (!nMap.containsKey(n)) {
            int value = nMap.size();
            nMap.put(n, value);
        }

        return n.getClass().getSimpleName() + "_" + nMap.get(n);
    }

    //##################################################################################################################
    // Visitor pattern


    @Override
    public void visit(CompilationUnit n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Optional<TypeDeclaration<?>> primaryType = n.getPrimaryType();
        if (primaryType.isPresent()) {
            primaryType.get().accept(this, arg);
            arg.getFirst().content = "#CU." + arg.getFirst().content;
        } else {
            arg.getFirst().content = "#CU.#without_name";
            arg.getSecond().content = Optional.of("This represents a CompilationUnit that contains no class.");
        }
    }

    @Override
    public void visit(ArrayType n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        try {
            ResolvedArrayType resolvedArrayType = n.resolve();
            container.content = lookup(resolvedArrayType);
        } catch (RuntimeException e) {
            arg.getSecond().content = Optional.ofNullable(e.getMessage());
            unresolvedLookupVisitor.visit(n, container);
        }
    }

    @Override
    public void visit(ClassOrInterfaceType n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        try {
            ResolvedReferenceType resolvedReferenceType = n.resolve().asReferenceType();
            container.content = lookup(resolvedReferenceType);
        } catch (RuntimeException e) {
            arg.getSecond().content = Optional.ofNullable(e.getMessage());
            unresolvedLookupVisitor.visit(n, container);
        }
    }

    @Override
    public void visit(TypeParameter n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        try {
            ResolvedTypeVariable resolvedTypeVariable = n.resolve();
            container.content = lookup(resolvedTypeVariable);
        } catch (RuntimeException e) {
            arg.getSecond().content = Optional.ofNullable(e.getMessage());
            unresolvedLookupVisitor.visit(n, container);
        }
    }


    @Override
    public void visit(PrimitiveType n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        try {
            ResolvedPrimitiveType resolvedPrimitiveType = n.resolve();
            container.content = lookup(resolvedPrimitiveType);
        } catch (RuntimeException e) {
            arg.getSecond().content = Optional.ofNullable(e.getMessage());
            unresolvedLookupVisitor.visit(n, container);
        }
    }

    @Override
    public void visit(IntersectionType n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        try {
            ResolvedIntersectionType resolvedIntersectionType = n.resolve();
            container.content = lookup(resolvedIntersectionType);
        } catch (RuntimeException e) {
            arg.getSecond().content = Optional.ofNullable(e.getMessage());
            unresolvedLookupVisitor.visit(n, container);
        }
    }

    @Override
    public void visit(UnionType n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        try {
            ResolvedUnionType resolvedUnionType = n.resolve();
            container.content = lookup(resolvedUnionType);
        } catch (RuntimeException e) {
            arg.getSecond().content = Optional.ofNullable(e.getMessage());
            unresolvedLookupVisitor.visit(n, container);
        }
    }

    @Override
    public void visit(UnknownType n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        try {
            ResolvedType resolvedType = n.resolve();
            container.content = lookup(resolvedType);
        } catch (RuntimeException e) {
            arg.getSecond().content = Optional.ofNullable(e.getMessage());
            unresolvedLookupVisitor.visit(n, container);
        }
    }

    @Override
    public void visit(VoidType n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        try {
            ResolvedVoidType resolvedType = n.resolve();
            container.content = lookup(resolvedType);
        } catch (RuntimeException e) {
            arg.getSecond().content = Optional.ofNullable(e.getMessage());
            unresolvedLookupVisitor.visit(n, container);
        }
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        try {
            ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
            container.content = lookup(resolvedReferenceTypeDeclaration);
        } catch (RuntimeException e) {
            arg.getSecond().content = Optional.ofNullable(e.getMessage());
            unresolvedLookupVisitor.visit(n, container);
        }
    }

    @Override
    public void visit(EnumDeclaration n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        try {
            ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
            container.content = lookup(resolvedReferenceTypeDeclaration);
        } catch (RuntimeException e) {
            arg.getSecond().content = Optional.ofNullable(e.getMessage());
            unresolvedLookupVisitor.visit(n, container);
        }
    }

    @Override
    public void visit(AnnotationDeclaration n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        try {
            ResolvedReferenceTypeDeclaration resolvedReferenceTypeDeclaration = n.resolve();
            container.content = lookup(resolvedReferenceTypeDeclaration);
        } catch (RuntimeException e) {
            arg.getSecond().content = Optional.ofNullable(e.getMessage());
            unresolvedLookupVisitor.visit(n, container);
        }
    }

    @Override
    public void visit(ConstructorDeclaration n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        try {
            ResolvedConstructorDeclaration resolvedConstructorDeclaration = n.resolve();
            container.content = lookup(resolvedConstructorDeclaration);
        } catch (RuntimeException e) {
            arg.getSecond().content = Optional.ofNullable(e.getMessage());
            unresolvedLookupVisitor.visit(n, container);
        }
    }

    @Override
    public void visit(MethodDeclaration n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        try {
            ResolvedMethodDeclaration resolvedMethodDeclaration = n.resolve();
            container.content = lookup(resolvedMethodDeclaration);
        } catch (RuntimeException e) {
            arg.getSecond().content = Optional.ofNullable(e.getMessage());
            unresolvedLookupVisitor.visit(n, container);
        }
    }

    @Override
    public void visit(EnumConstantDeclaration n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        try {
            Node parentNode = n.getParentNode().orElseThrow();
            ResolvedEnumConstantDeclaration resolvedEnumConstantDeclaration = n.resolve();

            if (parentNode instanceof Resolvable<?>) {
                Object resolved = ((Resolvable<?>) parentNode).resolve();

                if (resolved instanceof ResolvedTypeDeclaration) {
                    container.content = lookup(((ResolvedTypeDeclaration) resolved), resolvedEnumConstantDeclaration);
                } else if (resolved instanceof ResolvedConstructorDeclaration) {
                    container.content = lookup(((ResolvedConstructorDeclaration) resolved).declaringType())
                            + "!Anonymous" + "." + resolvedEnumConstantDeclaration.getName();
                }
            }

        } catch (RuntimeException e) {
            arg.getSecond().content = Optional.ofNullable(e.getMessage());
            unresolvedLookupVisitor.visit(n, container);
        }
    }

    @Override
    public void visit(FieldDeclaration n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        if (n.getVariables().size() == 1) {

            try {
                ResolvedValueDeclaration resolvedValueDeclaration = n.resolve();
                ResolvedFieldDeclaration resolvedFieldDeclaration = resolvedValueDeclaration.asField();

                container.content = lookup(resolvedFieldDeclaration.declaringType(), resolvedFieldDeclaration);
            } catch (RuntimeException e) {
                arg.getSecond().content = Optional.ofNullable(e.getMessage());
                unresolvedLookupVisitor.visit(n, container);
            }
        } else {
            // No unresolved lookup
            // this fieldDeclaration declares multiple variables and has to be assigned a custom name
            container.content = "";
            n.getParentNode().ifPresent(t -> container.content += lookup(t).getFirst() + ".");
            container.content += getCustomName(fieldDeclarationMap, n);
        }
    }

    @Override
    public void visit(VariableDeclarator n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        try {
            ResolvedValueDeclaration resolvedValueDeclaration = n.resolve();
            if (resolvedValueDeclaration.isField()) {
                ResolvedFieldDeclaration resolvedFieldDeclaration = resolvedValueDeclaration.asField();
                container.content = lookup(resolvedFieldDeclaration.declaringType(), resolvedFieldDeclaration);
            } else if (resolvedValueDeclaration.isEnumConstant()) {
                ResolvedEnumConstantDeclaration resolvedEnumConstantDeclaration = resolvedValueDeclaration.asEnumConstant();
                Optional<ResolvedReferenceTypeDeclaration> maybeTypeDeclaration = resolvedValueDeclaration.getType().asReferenceType().getTypeDeclaration();
                maybeTypeDeclaration.ifPresent(resolvedReferenceTypeDeclaration ->
                        container.content = lookup(resolvedReferenceTypeDeclaration, resolvedValueDeclaration));

                container.content = lookup(resolvedEnumConstantDeclaration.getType()) + "." + resolvedEnumConstantDeclaration.getName();
            }
        } catch (RuntimeException e) {
            arg.getSecond().content = Optional.ofNullable(e.getMessage());
            unresolvedLookupVisitor.visit(n, container);
        }
    }

    @Override
    public void visit(AnnotationMemberDeclaration n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        try {
            ResolvedValueDeclaration resolvedValueDeclaration = n.resolve();
            Optional<ResolvedReferenceTypeDeclaration> maybeTypeDeclaration = resolvedValueDeclaration.getType().asReferenceType().getTypeDeclaration();
            maybeTypeDeclaration.ifPresent(resolvedReferenceTypeDeclaration ->
                    container.content = lookup(resolvedReferenceTypeDeclaration, resolvedValueDeclaration));
        } catch (RuntimeException e) {
            arg.getSecond().content = Optional.ofNullable(e.getMessage());
            unresolvedLookupVisitor.visit(n, container);
        }
    }

    @Override
    public void visit(InitializerDeclaration n, Pair<Container<String>, Container<Optional<String>>> arg) {
        Container<String> container = arg.getFirst();

        // No unresolved lookup
        // this initializerDeclaration has to be assigned a custom name
        container.content = "";
        n.getParentNode().ifPresent(t -> container.content += lookup(t).getFirst() + ".");
        container.content += getCustomName(initializerDeclarationMap, n);
    }
}
