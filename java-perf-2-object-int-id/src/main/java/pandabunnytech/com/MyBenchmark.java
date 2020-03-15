

package pandabunnytech.com;

import org.openjdk.jmh.annotations.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.stream.IntStream;

@Fork(value = 3, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class MyBenchmark {
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";
    private static final int LOOKUPS = 10;
    private static final int PEOPLE = 30;

    private static final Person NULL = new Person(-1, -1, "not found");

    static class Person {
        final int id;
        final int age;
        final String name;

        Person(int id, int age, String name) {
            this.id = id;
            this.age = age;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            return id == person.id &&
                    age == person.age &&
                    Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, age, name);
        }

        @Override
        public String toString() {
            return "Person("+ id + ", " +  age + ", \"" + name + "\")";
        }
    }

    static class PersonColumns {
        final int[] ids;
        final String[] names;
        final int[] ages;
        final Person[] people;

        PersonColumns(int size) {
            this.ids = new int[size];
            this.names = new String[size];
            this.ages = new int[size];
            this.people = new Person[size];
        }

        Person get(int index) {
            return people[index];
        }

        void set(int index, Person person) {
            ids[index] = person.id;
            names[index] = person.name;
            ages[index] = person.age;
            people[index] = person;
        }
    }

    // State class containing a useful state to use!
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        final Person[] sortedPeople;
        final Person[] unsortedPeople;
        final PersonColumns sortedPersonColumns;
        final Map<Integer, Person> namePersonMap;
        final int[] idLookups;
        int curId;

        public BenchmarkState() {
            System.out.printf("LOOKUPS = %d, PEOPLE = %d%n", LOOKUPS, PEOPLE);

            sortedPeople = new Person[PEOPLE];
            sortedPersonColumns = new PersonColumns(PEOPLE);
            List<Integer> lookups = new ArrayList<>();
            namePersonMap = new HashMap<>();
            curId = RandomUtils.nextInt(100, 4000);

            // Generate people
            IntStream.range(0, PEOPLE).forEach(value -> {
                Person per = new Person(id(), age(), name());
                sortedPeople[value] = per;
                namePersonMap.put(per.id, per);
                sortedPersonColumns.set(value, per);
            });
            System.out.println("People = " + Arrays.toString(sortedPeople));

            // Generate lookups 50% random, 50% from actual people
            IntStream.range(0, LOOKUPS / 2).forEach($ -> lookups.add(id()));
            IntStream.range(0, LOOKUPS / 2).forEach($ -> lookups.add(sortedPeople[RandomUtils.nextInt(0, PEOPLE)].id));
            idLookups = lookups.stream().mapToInt(Integer::intValue).toArray();
            ArrayUtils.shuffle(idLookups); // mix random and actual by shuffling
            unsortedPeople  = Arrays.copyOf(sortedPeople, sortedPeople.length);
            ArrayUtils.shuffle(unsortedPeople); // We are shuffling people to mix
        }

        static String name() {
            return RandomStringUtils.random(10, ALPHA);
        }

        static int age() {
            return RandomUtils.nextInt(20, 41);
        }

        int id() {
            curId = curId + RandomUtils.nextInt(2, 10);
            return curId;
        }
    }

    @Benchmark
    public void searchArrayNaiveSorted(BenchmarkState x, Blackhole blackhole) {
        for (final int id: x.idLookups) {
            blackhole.consume(findFromArrayNaiveSorted(x, id));
        }
    }

    public Person findFromArrayNaiveSorted(BenchmarkState x, int id) {
        for (Person p : x.sortedPeople) {
            if (id == p.id) {
                return p;
            }
        }
        return NULL;
    }

    @Benchmark
    public void searchArrayNaiveUnsorted(BenchmarkState x, Blackhole blackhole) {
        for (final int id: x.idLookups) {
            blackhole.consume(findFromArrayNaiveUnsorted(x, id));
        }
    }

    public Person findFromArrayNaiveUnsorted(BenchmarkState x, int id) {
        for (Person p : x.unsortedPeople) {
            if (id == p.id) {
                return p;
            }
        }
        return NULL;
    }

    @Benchmark
    public void searchBinarySearch(BenchmarkState x, Blackhole blackhole) {
        for (final int id: x.idLookups) {
            blackhole.consume(findWithBinarySearch(x, id));
        }
    }

    public Person findWithBinarySearch(BenchmarkState x, int id) {
        // Compare only IDs since it's unique!
        int index = Arrays.binarySearch(x.sortedPeople,
                new Person(id, 0, ""), Comparator.comparingInt(o -> o.id));
        // this returns `-insertion point - 1` if not found
        if (index < 0) {
            return NULL;
        }
        return x.sortedPeople[index];
    }

    @Benchmark
    public void searchColumnsBinarySearch(BenchmarkState x, Blackhole blackhole) {
        for (final int id: x.idLookups) {
            blackhole.consume(findWithColumnsBinarySearch(x, id));
        }
    }

    public Person findWithColumnsBinarySearch(BenchmarkState x, int id) {
        int index = Arrays.binarySearch(x.sortedPersonColumns.ids, id);
        if (index < 0) {
            return NULL;
        }
        return x.sortedPersonColumns.get(index);
    }

    @Benchmark
    public void searchColumnNaiveSearch(BenchmarkState x, Blackhole blackhole) {
        for (final int id: x.idLookups) {
            blackhole.consume(findWithColumnNaiveSearch(x, id));
        }
    }

    public Person findWithColumnNaiveSearch(BenchmarkState x, int id) {
        for (int i = 0; i < x.sortedPersonColumns.ids.length; i++) {
            if (x.sortedPersonColumns.ids[i] == id) {
                return x.sortedPersonColumns.get(i);
            }
        }
        return NULL;
    }

    @Benchmark
    public void fromMap(BenchmarkState x, Blackhole blackhole) {
        for (final int id: x.idLookups) {
            blackhole.consume(findFromMap(x, id));
        }
    }

    public Person findFromMap(BenchmarkState x, int id) {
        return x.namePersonMap.getOrDefault(id, NULL);
    }

    public static void main(String[] a) {
        MyBenchmark m  = new MyBenchmark();
        BenchmarkState x = new BenchmarkState();
        int id = x.idLookups[0];

        System.out.println(m.findFromArrayNaiveSorted(x, id));
        System.out.println(m.findFromArrayNaiveUnsorted(x, id));
        System.out.println(m.findFromMap(x, id));
        System.out.println(m.findWithBinarySearch(x, id));
        System.out.println(m.findWithColumnsBinarySearch(x, id));
        System.out.println(m.findWithColumnNaiveSearch(x, id));
    }
}

