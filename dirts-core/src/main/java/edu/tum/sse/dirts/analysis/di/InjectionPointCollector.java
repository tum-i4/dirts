package edu.tum.sse.dirts.analysis.di;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import edu.tum.sse.dirts.util.Log;

import java.util.Collection;

import static java.util.logging.Level.FINER;

public interface InjectionPointCollector<T extends BodyDeclaration<?>> {

    void collectInjectionPoints(Collection<TypeDeclaration<?>> ts, InjectionPointStorage injectionPointStorage);
}