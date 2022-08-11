package edu.tum.sse.dirts.analysis.di;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.util.Collection;

/**
 * Collects InjectionPoints
 * @param <T>
 */
@SuppressWarnings("unused")
public interface InjectionPointCollector<T extends BodyDeclaration<?>> {

    void collectInjectionPoints(Collection<TypeDeclaration<?>> ts, InjectionPointStorage injectionPointStorage);
}