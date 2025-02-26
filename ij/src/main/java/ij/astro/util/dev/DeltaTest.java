package ij.astro.util.dev;

import java.util.Arrays;
import java.util.Objects;

public class DeltaTest {
    private final ThreadLocal<Holder<?>> previousHolder = new ThreadLocal<>();

    public DeltaTest() {
    }

    public void compare(String name, Object... data) {
        var p = previousHolder.get();
        if ((p == null || Objects.equals(p.name(), name)) || !(p instanceof ObjectHolder)) {
            previousHolder.set(new ObjectHolder(name, data));
        } else {
            ((ObjectHolder) p).matches(new ObjectHolder(name, data));
        }
    }

    /*public void compare(String name, double... data) {
        var p = previousHolder.get();
        if ((p == null || Objects.equals(p.name(), name)) || !(p instanceof DoubleHolder)) {
            previousHolder.set(new DoubleHolder(name, data));
        } else {
            ((DoubleHolder) p).matches(new DoubleHolder(name, data));
        }
    }*/

    private record ObjectHolder(String name, Object[] data) implements Holder<ObjectHolder> {
        @Override
        public boolean matches(ObjectHolder other) {
            if (data.length != other.data().length) {
                throw new IllegalArgumentException("Cannot compare different sized datasets");
            }

            var i = Arrays.mismatch(data, other.data);

            if (i < 0) {
                return true;
            }

            System.out.println("Comparing '%s' with '%s': found first mismatch: %s".formatted(name, other.name, i));
            System.out.println("\t%s - %s != %s - %s".formatted(name, data[i], other.name, other.data[i]));
            return false;
        }
    }

    private record DoubleHolder(String name, double[] data) implements Holder<DoubleHolder> {
        @Override
        public boolean matches(DoubleHolder other) {
            if (data.length != other.data().length) {
                throw new IllegalArgumentException("Cannot compare different sized datasets");
            }

            var i = Arrays.mismatch(data, other.data);

            if (i < 0) {
                return true;
            }

            System.out.println("Comparing '%s' with '%s': found first mismatch: %s".formatted(name, other.name, i));
            return false;
        }
    }

    private interface Holder<T extends Holder<T>> {
        String name();

        boolean matches(T other);
    }
}
