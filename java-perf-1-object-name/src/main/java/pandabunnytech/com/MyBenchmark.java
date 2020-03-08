

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

    private static final Person NULL = new Person(-1, "not found");

    static class Person {
        final int age;
        final String name;

        public Person(int age, String name) {
            this.age = age;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            return age == person.age &&
                    Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(age, name);
        }

        @Override
        public String toString() {
            return "Person("+ age + ", \"" + name + "\")";
        }
    }

    // State class containing a useful state to use!
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        final Person[] people;
        final Map<String, Person> namePersonMap;
        final String[] nameLookups;
        public BenchmarkState() {
            people = new Person[PEOPLE];
            List<String> lookups = new ArrayList<>();
            namePersonMap = new HashMap<>();

            // Generate people
            IntStream.range(0, PEOPLE).forEach(value -> {
                Person per = new Person(age(), name());
                people[value] = per;
                namePersonMap.put(per.name, per);
            });
            System.out.println("People = " + Arrays.toString(people));

            // Generate lookups 50% random, 50% from actual people
            IntStream.range(0, LOOKUPS / 2).forEach($ -> lookups.add(name()));
            IntStream.range(0, LOOKUPS / 2).forEach($ -> lookups.add(
                    String.format("%s", people[RandomUtils.nextInt(0, PEOPLE)].name)));
            nameLookups = lookups.toArray(new String[0]);
            ArrayUtils.shuffle(nameLookups); // mix random and actual by shuffling
        }

        public static String name() {
            return RandomStringUtils.random(10, ALPHA);
        }

        public static int age() {
            return RandomUtils.nextInt(20, 41);
        }
    }

    @Benchmark
    public void searchArray(BenchmarkState x, Blackhole blackhole) {
        for (final String name: x.nameLookups) {
            blackhole.consume(findFromArray(x, name));
        }
    }

    public Person findFromArray(BenchmarkState x, String name) {
        for (Person p : x.people) {
            if (name.equals(p.name)) {
                return p;
            }
        }
        return NULL;
    }

    @Benchmark
    public void fromMap(BenchmarkState x, Blackhole blackhole) {
        for (final String name: x.nameLookups) {
            blackhole.consume(findFromMap(x, name));
        }
    }

    public Person findFromMap(BenchmarkState x, String name) {
        return x.namePersonMap.getOrDefault(name, NULL);
    }
//
//    public static void main(String[] a) {
//        MyBenchmark m  = new MyBenchmark();
//        BenchmarkState x = new BenchmarkState();
//        for (final String name: x.nameLookups) {
//            System.out.println(m.findFromArray(x, name));
//        }
//    }
}

