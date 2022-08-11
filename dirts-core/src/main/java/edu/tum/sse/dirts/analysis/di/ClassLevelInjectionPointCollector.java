package edu.tum.sse.dirts.analysis.di;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

/**
 * Collects InjectionPoints represented by class level nodes
 */
public interface ClassLevelInjectionPointCollector extends InjectionPointCollector<TypeDeclaration<?>> {

    /*
    Since generally all subclasses extend VoidVisitor, it is unfortunately not a compilation error
    if a subclass forgets to directly implement one of those methods

    Nevertheless, this can be used to check for completeness by clicking on the green circle on the left
    of each method generated by IntelliJ
     */

    void visit(ClassOrInterfaceDeclaration n, InjectionPointStorage arg);

    void visit(EnumDeclaration n, InjectionPointStorage arg);

    void visit(AnnotationDeclaration n, InjectionPointStorage arg);
}