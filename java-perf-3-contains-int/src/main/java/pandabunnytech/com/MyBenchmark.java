package pandabunnytech.com;

import org.openjdk.jmh.annotations.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.stream.IntStream;

@Fork(value = 3, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations=5)
public class MyBenchmark {
    private static final int LOOKUPS = 10;
    private static final int ELEMENTS = 30;

    // State class containing a useful state to use!
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        final int[] elementsUnsorted;
        final int[] elementsSorted;
        final Map<Integer, Boolean> elementToTrueMap;
        final Set<Integer> elementSet;
        final int[] elementLookups;
        int curId;

        public BenchmarkState() {
            elementsUnsorted = new int[ELEMENTS];
            List<Integer> lookups = new ArrayList<>();
            elementToTrueMap = new HashMap<>();
            elementSet = new HashSet<>();
            curId = RandomUtils.nextInt(100, 4000);

            // Generate elements
            IntStream.range(0, ELEMENTS).forEach(value -> {
                int elem = id();
                elementsUnsorted[value] = elem;
                elementToTrueMap.put(elem, true);
                elementSet.add(elem);
            });

            // Generate lookups 50% random, 50% from actual elements
            IntStream.range(0, LOOKUPS / 2).forEach($ -> lookups.add(id()));
            IntStream.range(0, LOOKUPS / 2).forEach($ -> lookups.add(
                    elementsUnsorted[RandomUtils.nextInt(0, ELEMENTS)]));
            elementLookups = lookups.stream().mapToInt(Integer::intValue).toArray();
            ArrayUtils.shuffle(elementLookups); // mix random and actual by shuffling
            elementsSorted = Arrays.copyOf(elementsUnsorted, elementsUnsorted.length);
            ArrayUtils.shuffle(elementsUnsorted); // make it truly unsorted

            System.out.println("Elements Unsorted = " + Arrays.toString(elementsUnsorted));
            System.out.println("Elements Sorted = " + Arrays.toString(elementsSorted));
            System.out.println("Elements Lookups = " + Arrays.toString(elementLookups));
        }

        public int id() {
            curId = curId + RandomUtils.nextInt(2, 10);
            return curId;
        }
    }

    @Benchmark
    public void searchArraySorted(BenchmarkState x, Blackhole blackhole) {
        for (final int id: x.elementLookups) {
            blackhole.consume(findFromArraySorted(x, id));
        }
    }

    public boolean findFromArraySorted(BenchmarkState x, int id) {
        for (int p : x.elementsSorted) {
            if (id == p) {
                return true;
            }
        }
        return false;
    }

    @Benchmark
    public void searchArrayUnsorted(BenchmarkState x, Blackhole blackhole) {
        for (final int id: x.elementLookups) {
            blackhole.consume(findFromArrayUnsorted(x, id));
        }
    }

    public boolean findFromArrayUnsorted(BenchmarkState x, int id) {
        for (int p : x.elementsUnsorted) {
            if (id == p) {
                return true;
            }
        }
        return false;
    }


    @Benchmark
    public void fromMapGet(BenchmarkState x, Blackhole blackhole) {
        for (final int id: x.elementLookups) {
            blackhole.consume(findFromMap(x, id));
        }
    }

    public boolean findFromMap(BenchmarkState x, int id) {
        return x.elementToTrueMap.getOrDefault(id, false);
    }

    @Benchmark
    public void fromMapContainsKey(BenchmarkState x, Blackhole blackhole) {
        for (final int id: x.elementLookups) {
            blackhole.consume(findFromMapContainsKey(x, id));
        }
    }

    public boolean findFromMapContainsKey(BenchmarkState x, int id) {
        return x.elementToTrueMap.containsKey(id);
    }


    @Benchmark
    public void fromSet(BenchmarkState x, Blackhole blackhole) {
        for (final int id: x.elementLookups) {
            blackhole.consume(findFromSet(x, id));
        }
    }

    public boolean findFromSet(BenchmarkState x, int id) {
        return x.elementSet.contains(id);
    }
}

