package edu.tum.sse.dirts.graph;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static org.junit.jupiter.api.Assertions.*;

class MultiValueNtoNMapTest {

    @Test
    void testPut() {
        /* given */
        MultiValueNtoNMap<Integer, Integer> sut1 = new MultiValueNtoNMap<>();
        MultiValueNtoNMap<Integer, Integer> sut2 = new MultiValueNtoNMap<>();

        /* when */
        for (int i = 0; i < 1000; i++) {
            int randomInt1 = (int) (Math.random() * 1000);
            int randomInt2 = (int) (Math.random() * 1000);
            int randomInt3 = (int) (Math.random() * 1000);
            sut1.put(randomInt1, randomInt2, randomInt3);
            sut2.put(randomInt2, randomInt1, randomInt3);
        }

        /* then */

        // check if metamorphic relation between regular and inverse map holds
        assertThat(sut1.getRegularMap()).usingRecursiveComparison().isEqualTo(sut2.getInverseMap());
        assertThat(sut2.getRegularMap()).usingRecursiveComparison().isEqualTo(sut1.getInverseMap());
    }

    @Test
    void remove() {
        /* given */
        MultiValueNtoNMap<Integer, Integer> sut1 = new MultiValueNtoNMap<>();
        MultiValueNtoNMap<Integer, Integer> sut2 = new MultiValueNtoNMap<>();

        Set<Integer> toRemove = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            int randomInt1 = (int) (Math.random() * 1000);
            int randomInt2 = (int) (Math.random() * 1000);
            int randomInt3 = (int) (Math.random() * 1000);
            sut1.put(randomInt1, randomInt2, randomInt3);
            sut2.put(randomInt2, randomInt1, randomInt3);

            if (i % 10 == 0) {
                toRemove.add(randomInt1);
            }
        }

        for (Integer integer : toRemove) {
            assertThat(sut1.getRegularMap()).containsKey(integer);
            assertThat(sut2.getInverseMap()).containsKey(integer);

            /* when */
            sut1.remove(integer);
            sut2.remove(integer);

            /* then */

            // check if removed correctly
            assertThat(sut1.getRegularMap()).doesNotContainKey(integer);
            assertThat(sut2.getInverseMap()).doesNotContainKey(integer);
        }

        // check if metamorphic relation between regular and inverse map holds
        assertThat(sut1.getRegularMap()).usingRecursiveComparison().isEqualTo(sut2.getInverseMap());
        assertThat(sut2.getRegularMap()).usingRecursiveComparison().isEqualTo(sut1.getInverseMap());

        for (Integer integer : toRemove) {
            assertThat(sut1.getRegularMap()).doesNotContainKey(integer);
            assertThat(sut2.getInverseMap()).doesNotContainKey(integer);
        }
    }

    @Test
    void rename() {
        /* given */
        MultiValueNtoNMap<Integer, Integer> sut1 = new MultiValueNtoNMap<>();
        MultiValueNtoNMap<Integer, Integer> sut2 = new MultiValueNtoNMap<>();

        for (int i = 0; i < 1000; i++) {
            sut1.put(i, i + 1, i + 2);
            sut2.put(i + 1, i, i + 2);
        }

        for (int i = 1; i < 100; i++) {
            int oldInt = i * 10;
            int newInt = i * 10000;

            assertThat(sut1.getRegularMap()).containsKey(oldInt);
            assertThat(sut2.getInverseMap()).containsKey(oldInt);

            /* when */
            sut1.rename(oldInt, newInt);
            sut2.rename(oldInt, newInt);

            /* then */

            // check if removed correctly
            assertThat(sut1.getRegularMap()).doesNotContainKey(oldInt);
            assertThat(sut2.getInverseMap()).doesNotContainKey(oldInt);

            assertThat(sut1.getRegularMap()).containsKey(newInt);
            assertThat(sut2.getInverseMap()).containsKey(newInt);
        }
        ;


        // check if metamorphic relation between regular and inverse map holds
        assertThat(sut1.getRegularMap()).usingRecursiveComparison().isEqualTo(sut2.getInverseMap());
        assertThat(sut2.getRegularMap()).usingRecursiveComparison().isEqualTo(sut1.getInverseMap());
    }

    @Test
    void removeAllValues() {
        /* given */
        MultiValueNtoNMap<Integer, Integer> sut1 = new MultiValueNtoNMap<>();
        MultiValueNtoNMap<Integer, Integer> sut2 = new MultiValueNtoNMap<>();

        for (int i = 0; i < 1000; i++) {
            sut1.put(i, i + 1, i % 10);
            sut2.put(i + 1, i, i % 10);
        }

        /* when */
        sut1.removeAllValues(Set.of(3));
        sut2.removeAllValues(Set.of(3));

        for (int i = 0; i < 100; i++) {

            /* then */

            // check if removed correctly
            assertThat(sut1.getRegularMap()).doesNotContainKey(i * 10 + 3);
            assertThat(sut2.getInverseMap()).doesNotContainKey(i * 10 + 3);
        }


        // check if metamorphic relation between regular and inverse map holds
        assertThat(sut1.getRegularMap()).usingRecursiveComparison().isEqualTo(sut2.getInverseMap());
        assertThat(sut2.getRegularMap()).usingRecursiveComparison().isEqualTo(sut1.getInverseMap());
    }

    @Test
    void removeRegularEntries() {
        // TODO
    }

    @Test
    void removeInverseEntries() {
        // TODO
    }
}