package org.erachain.dbs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import org.junit.Test;
import org.mapdb.Fun;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class MergedIteratorNoDuplicatesTest {

    @Test
    public void hasNext() {
    }

    @Test
    public void next() {
        Set<Long> parent = new TreeSet<Long>() {{
            add(112L);
            add(10L);
            add(212L);
        }};

        Set<Long> fork = new TreeSet<Long>() {{
            add(112L);
            add(21L);
            add(152L);
        }};

        List<Long> list = new ArrayList<Long>() {{
            add(10L);
            add(21L);
            add(112L);
            add(152L);
            add(212L);
        }};

        Iterator<Long> parentIterator = parent.iterator();
        Iterator<Long> forkIterator = fork.iterator();
        Iterator<Long> merged = new MergedIteratorNoDuplicates((Iterable) ImmutableList.of(parentIterator, forkIterator), Fun.COMPARATOR);

        assertEquals(Iterators.size(merged), 5);

        // refresh
        parentIterator = parent.iterator();
        forkIterator = fork.iterator();
        merged = new MergedIteratorNoDuplicates<Long>((Iterable) ImmutableList.of(parentIterator, forkIterator), Fun.COMPARATOR);

        for (Long item : list) {
            assertEquals(item, merged.next());
        }

        // это должен быть уже конец и ошибка должна быть:
        try {
            assertEquals(merged.next(), "must be Exception: java.util.NoSuchElementException");
        } catch (java.util.NoSuchElementException e) {
        }
        assertEquals(merged.hasNext(), false);

        ///////////////////////
        parentIterator = parent.iterator();
        fork = new TreeSet<Long>() {{
            add(10L);
            add(21L);
            add(212L);
        }};
        forkIterator = fork.iterator();
        merged = new MergedIteratorNoDuplicates((Iterable) ImmutableList.of(parentIterator, forkIterator), Fun.COMPARATOR);

        assertEquals(Iterators.size(merged), 4);

        // refresh
        parentIterator = parent.iterator();
        forkIterator = fork.iterator();
        merged = new MergedIteratorNoDuplicates<Long>((Iterable) ImmutableList.of(parentIterator, forkIterator), Fun.COMPARATOR);

        list = new ArrayList<Long>() {{
            add(10L);
            add(21L);
            add(112L);
            add(212L);
        }};

        for (Long item : list) {
            assertEquals(item, merged.next());
        }

        // это должен быть уже конец и ошибка должна быть:
        try {
            assertEquals(merged.next(), "must be Exception: java.util.NoSuchElementException");
        } catch (java.util.NoSuchElementException e) {
        }
        assertEquals(merged.hasNext(), false);

    }
}