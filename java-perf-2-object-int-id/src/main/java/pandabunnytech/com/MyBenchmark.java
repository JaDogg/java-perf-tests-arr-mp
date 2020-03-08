

package pandabunnytech.com;

import org.openjdk.jmh.annotations.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.stream.IntStream;

@Fork(value = 3, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations=5)
public class MyBenchmark {
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";
    private static final int LOOKUPS = 10;
    private static final int PEOPLE = 30;

    private static final Person NULL = new Person(-1, -1, "not found");

    static class Person {
        final int id;
        final int age;
        final String name;

        public Person(int id, int age, String name) {
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

    // State class containing a useful state to use!
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        final Person[] people;
        final Map<Integer, Person> namePersonMap;
        final int[] idLookups;
        int curId;
        public BenchmarkState() {
            people = new Person[PEOPLE];
            List<Integer> lookups = new ArrayList<>();
            namePersonMap = new HashMap<>();
            curId = RandomUtils.nextInt(100, 4000);

            // Generate people
            IntStream.range(0, PEOPLE).forEach(value -> {
                Person per = new Person(id(), age(), name());
                people[value] = per;
                namePersonMap.put(per.id, per);
            });
            System.out.println("People = " + Arrays.toString(people));

            // Generate lookups 50% random, 50% from actual people
            IntStream.range(0, LOOKUPS / 2).forEach($ -> lookups.add(id()));
            IntStream.range(0, LOOKUPS / 2).forEach($ -> lookups.add(people[RandomUtils.nextInt(0, PEOPLE)].id));
            idLookups = lookups.stream().mapToInt(Integer::intValue).toArray();
            ArrayUtils.shuffle(idLookups); // mix random and actual by shuffling
            ArrayUtils.shuffle(people); // We are shuffling people to mix IDs
        }

        public static String name() {
            return RandomStringUtils.random(10, ALPHA);
        }

        public static int age() {
            return RandomUtils.nextInt(20, 41);
        }

        public int id() {
            curId = curId + RandomUtils.nextInt(2, 10);
            return curId;
        }
    }

    @Benchmark
    public void searchArray(BenchmarkState x, Blackhole blackhole) {
        for (final int id: x.idLookups) {
            blackhole.consume(findFromArray(x, id));
        }
    }

    public Person findFromArray(BenchmarkState x, int id) {
        for (Person p : x.people) {
            if (id == p.id) {
                return p;
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

    public Person findFromMap(BenchmarkState x, int name) {
        return x.namePersonMap.getOrDefault(name, NULL);
    }
}

