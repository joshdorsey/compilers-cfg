package compilers.util;

import java.util.Objects;

public class Tuple<S, T> {
    private final S first;
    private final T second;

    public static <U, V>  Tuple<U, V> of(U a, V b) {
        return new Tuple<U, V>(a, b);
    }

    private Tuple(S a, T b) {
        this.first = a;
        this.second = b;
    }

    public S getFirst() {
        return first;
    }

    public T getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple<?, ?> tuple = (Tuple<?, ?>) o;
        return Objects.equals(first, tuple.first) &&
                Objects.equals(second, tuple.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}
