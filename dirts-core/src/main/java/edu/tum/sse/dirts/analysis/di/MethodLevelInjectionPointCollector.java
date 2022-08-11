package edu.tum.sse.dirts.analysis.di;

import com.github.javaparser.ast.body.*;

/**
 * Collects InjectionPoints represented by method level nodes
 */
public interface MethodLevelInjectionPointCollector extends InjectionPointCollector<BodyDeclaration<?>> {

    /*
    Since generally all subclasses extend VoidVisitor, it is unfortunately not a compilation error
    if a subclass forgets to directly implement one of those methods

    Nevertheless, this can be used to check for completeness by clicking on the green circle on the left
    of each method generated by IntelliJ
     */

    void visit(MethodDeclaration n, InjectionPointStorage arg);

    void visit(ConstructorDeclaration n, InjectionPointStorage arg);

    void visit(FieldDeclaration n, InjectionPointStorage arg);

    void visit(EnumConstantDeclaration n, InjectionPointStorage arg);

    void visit(AnnotationMemberDeclaration n, InjectionPointStorage arg);

    void visit(InitializerDeclaration n, InjectionPointStorage arg);

}