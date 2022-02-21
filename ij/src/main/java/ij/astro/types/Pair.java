package ij.astro.types;

import java.util.Optional;

public class Pair {
    public record GenericPair<T1, T2>(T1 first, T2 second) {
        public GenericPair<T1, T2> setFirst(T1 first) {
            return new GenericPair<>(first, second);
        }

        public GenericPair<T1, T2> setSecond(T2 second) {
            return new GenericPair<>(first, second);
        }
    }

    public record OptionalGenericPair<T1, T2>(Optional<T1> first, Optional<T2> second) {
        public OptionalGenericPair() {
            this(Optional.empty(), Optional.empty());
        }

        public OptionalGenericPair(T1 first, T2 second) {
            this(Optional.ofNullable(first), Optional.ofNullable(second));
        }
    }

    public record IntPair(int first, int second) {
        public IntPair() {
            this(0, 0);
        }

        public static IntPair empty() {
            return new IntPair();
        }
    }

    public record IntFloatPair(int first, float second) {
        public IntFloatPair() {
            this(0, 0);
        }

        public static IntFloatPair empty() {
            return new IntFloatPair();
        }
    }

    public record FloatPair(float first, float second) {
        public FloatPair() {
            this(0, 0);
        }

        public static FloatPair empty() {
            return new FloatPair();
        }
    }

    public record DoublePair(double first, double second) {
        public DoublePair() {
            this(0, 0);
        }

        public static DoublePair empty() {
            return new DoublePair();
        }
    }

    public record ShortPair(short first, short second) {
        public ShortPair() {
            this((short) 0, (short) 0);
        }

        public static ShortPair empty() {
            return new ShortPair();
        }
    }

    public record LongPair(long first, long second) {
        public LongPair() {
            this(0, 0);
        }

        public static LongPair empty() {
            return new LongPair();
        }
    }

    public record BooleanPair(boolean first, boolean second) {
        public BooleanPair() {
            this(false, false);
        }

        public static BooleanPair empty() {
            return new BooleanPair();
        }
    }

    public record BytePair(byte first, byte second) {
        public BytePair() {
            this((byte) 0, (byte) 0);
        }

        public static BytePair empty() {
            return new BytePair();
        }
    }

    public record CharPair(char first, char second) {
        public CharPair() {
            this('\u0000', '\u0000');
        }

        public static CharPair empty() {
            return new CharPair();
        }
    }

    private Pair() {}
}
