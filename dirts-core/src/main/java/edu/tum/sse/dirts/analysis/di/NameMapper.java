package edu.tum.sse.dirts.analysis.di;

import edu.tum.sse.dirts.util.tuples.Pair;

import java.util.Set;

public interface NameMapper<B extends Bean> {

    public Set<String> mapToString(B b);
}
