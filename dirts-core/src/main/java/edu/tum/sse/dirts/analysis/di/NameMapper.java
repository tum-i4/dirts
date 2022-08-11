package edu.tum.sse.dirts.analysis.di;

import java.util.Set;

/**
 * Used to map beans to the names of corresponding nodes
 * @param <B>
 */
@FunctionalInterface
public interface NameMapper<B extends Bean> {

    Set<String> mapToString(B b);
}
