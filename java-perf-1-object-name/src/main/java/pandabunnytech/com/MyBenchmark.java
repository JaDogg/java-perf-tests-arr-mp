

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
    private static final int PEOPLE = 10;

    private static final Person NULL = new Person(-1, "not found");

    static class Person {
        final int age;
        final String name;

        Person(int age, String name) {
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

    static class PersonColumns {
        final String[] names;
        final int[] ages;
        final Person[] people;

        PersonColumns(int size) {
            this.names = new String[size];
            this.ages = new int[size];
            this.people = new Person[size];
        }

        Person get(int index) {
            return people[index];
        }

        void set(int index, Person person) {
            names[index] = person.name;
            ages[index] = person.age;
            people[index] = person;
        }
    }

    // State class containing a useful state to use!
    @State(Scope.Benchmark)
    public static class BenchmarkState {
        final Person[] people;
        final Map<String, Person> namePersonMap;
        final String[] nameLookups;
        final PersonColumns personColumns;

        public BenchmarkState() {
            System.out.printf("LOOKUPS = %d, PEOPLE = %d%n", LOOKUPS, PEOPLE);

            people = new Person[PEOPLE];
            List<String> lookups = new ArrayList<>();
            namePersonMap = new HashMap<>();
            personColumns = new PersonColumns(PEOPLE);

            // Generate people
            IntStream.range(0, PEOPLE).forEach(value -> {
                Person per = new Person(age(), name());
                people[value] = per;
                namePersonMap.put(per.name, per);
                personColumns.set(value, per);
            });
            System.out.println("People = " + Arrays.toString(people));

            // Generate lookups 50% random, 50% from actual people
            IntStream.range(0, LOOKUPS / 2).forEach($ -> lookups.add(name()));
            IntStream.range(0, LOOKUPS / 2).forEach($ -> lookups.add(
                    String.format("%s", people[RandomUtils.nextInt(0, PEOPLE)].name)));
            nameLookups = lookups.toArray(new String[0]);
            ArrayUtils.shuffle(nameLookups); // mix random and actual by shuffling
        }

        private static String name() {
            return RandomStringUtils.random(10, ALPHA);
        }

        private static int age() {
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
    public void searchWithHash(BenchmarkState x, Blackhole blackhole) {
        for (final String name: x.nameLookups) {
            blackhole.consume(fromFromArrayWithHash(x, name));
        }
    }

    public Person fromFromArrayWithHash(BenchmarkState x, String name) {
        int nameHash = name.hashCode();
        for (Person p : x.people) {
            if (nameHash == p.name.hashCode() && name.equals(p.name)) {
                return p;
            }
        }
        return NULL;
    }

    @Benchmark
    public void searchWithHashPersonColumns(BenchmarkState x, Blackhole blackhole) {
        for (final String name: x.nameLookups) {
            blackhole.consume(fromHashPersonColumns(x, name));
        }
    }

    public Person fromHashPersonColumns(BenchmarkState x, String name) {
        int nameHash = name.hashCode();
        for(int i = 0; i< x.personColumns.names.length; i++) {
            if (x.personColumns.names[i].hashCode() == nameHash && x.personColumns.names[i].equals(name)) {
                return x.personColumns.get(i);
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

    public static void main(String[] a) {
        MyBenchmark m  = new MyBenchmark();
        BenchmarkState x = new BenchmarkState();
        String name = x.nameLookups[0];

        System.out.println(m.findFromArray(x, name));
        System.out.println(m.findFromMap(x, name));
        System.out.println(m.fromFromArrayWithHash(x, name));
        System.out.println(m.fromHashPersonColumns(x, name));
    }
}

