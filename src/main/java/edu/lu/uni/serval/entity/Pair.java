package edu.lu.uni.serval.entity;

public class Pair<E1, E2> {

    public final E1 firstElement;

    public final E2 secondElement;

    public Pair(E1 e1, E2 e2) {
        this.firstElement = e1;
        this.secondElement = e2;
    }

    public E1 getFirst() {
        return firstElement;
    }

    public E2 getSecond() {
        return secondElement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair<?, ?> pair = (Pair<?, ?>) o;

        if (!firstElement.equals(pair.firstElement)) return false;
        return secondElement.equals(pair.secondElement);

    }

    @Override
    public int hashCode() {
        int result = firstElement.hashCode();
        result = 31 * result + secondElement.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "(" + getFirst().toString() + "," + getSecond().toString() + ")";
    }

}
