package edu.tum.sse.dirts.graph;

import java.util.*;

public class MultiValueNtoNMap<K, V> {

    private final Map<K, Map<K, Set<V>>> regularMap;
    private final Map<K, Map<K, Set<V>>> inverseMap;

    public MultiValueNtoNMap() {
        inverseMap = new HashMap<>();
        regularMap = new HashMap<>();
    }

    public MultiValueNtoNMap(Map<K, Map<K, Set<V>>> regularMap, Map<K, Map<K, Set<V>>> inverseMap) {
        this.regularMap = regularMap;
        this.inverseMap = inverseMap;
    }

    public Map<K, Map<K, Set<V>>> getRegularMap() {
        return Collections.unmodifiableMap(regularMap);
    }

    public Map<K, Map<K, Set<V>>> getInverseMap() {
        return Collections.unmodifiableMap(inverseMap);
    }

    public V put(K k1, K k2, V value) {

        regularMap.computeIfAbsent(k1, k -> new HashMap<>());
        inverseMap.computeIfAbsent(k2, k -> new HashMap<>());

        Map<K, Set<V>> k1Map = regularMap.get(k1);

        if (!k1Map.containsKey(k2)) {
            Map<K, Set<V>> k2Map = inverseMap.get(k2);
            assert (!k2Map.containsKey(k1));

            Set<V> values = new HashSet<>();
            values.add(value);

            k1Map.put(k2, values);
            k2Map.put(k1, values);
        } else {
            k1Map.get(k2).add(value);
        }

        return value;
    }


    public void remove(K k) {
        if (regularMap.containsKey(k)) {
            regularMap.get(k).forEach((k2, values) -> inverseMap.get(k2).remove(k));
            regularMap.remove(k);
        }

        if (inverseMap.containsKey(k)) {
            inverseMap.get(k).forEach((k1, values) -> regularMap.get(k1).remove(k));
            inverseMap.remove(k);
        }
    }

    public void rename(K oldK, K newK) {
        if (regularMap.containsKey(oldK)) {
            Set<K> forwards = regularMap.get(oldK).keySet();
            forwards.forEach(to -> inverseMap.get(to).put(newK, inverseMap.get(to).remove(oldK)));
            regularMap.put(newK, regularMap.remove(oldK));
        }
        if (inverseMap.containsKey(oldK)) {
            Set<K> backwards = inverseMap.get(oldK).keySet();
            backwards.forEach(to -> regularMap.get(to).put(newK, regularMap.get(to).remove(oldK)));
            inverseMap.put(newK, inverseMap.remove(oldK));
        }
    }

    public void removeAllValues(Set<V> valuesToRemove) {
        Set<K> removedNodes = new HashSet<>();

        for (K from : regularMap.keySet()) {
            Map<K, Set<V>> k1Map = regularMap.get(from);
            Set<K> removedInnerNodes = new HashSet<>();
            k1Map.forEach((to, values) -> {
                values.removeAll(valuesToRemove);
                if (values.isEmpty())
                    removedInnerNodes.add(to);
            });
            removedInnerNodes.forEach(to -> {
                k1Map.remove(to);
                inverseMap.get(to).remove(from);
                if (inverseMap.get(to).isEmpty()) {
                    inverseMap.remove(to);
                }
            });
            if (k1Map.isEmpty()) {
                removedNodes.add(from);
            }
        }
        removedNodes.forEach(regularMap::remove);
    }

    public Set<K> removeRegularEntries(K k1, Set<V> consideredValues) {
        Set<K> ret = new HashSet<>();
        if (regularMap.containsKey(k1)) {
            Map<K, Set<V>> regularMappings = regularMap.get(k1);
            regularMappings.forEach((k2, values) -> {
                values.removeAll(consideredValues);
                if (values.isEmpty()) {
                    inverseMap.get(k2).remove(k1);
                    ret.add(k2);
                }
            });
            ret.forEach(regularMappings::remove);
        }
        return ret;
    }

    public Set<K> removeInverseEntries(K k2, Set<V> consideredValues) {
        Set<K> ret = new HashSet<>();
        if (inverseMap.containsKey(k2)) {
            Map<K, Set<V>> inverseMappings = inverseMap.get(k2);
            inverseMappings.forEach((k1, values) -> {
                values.removeAll(consideredValues);
                if (values.isEmpty()) {
                    regularMap.get(k1).remove(k2);
                    ret.add(k1);
                }
            });
            ret.forEach(inverseMappings::remove);
        }
        return ret;
    }

}

