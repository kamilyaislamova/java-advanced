package info.kgeorgiy.ja.islamova.arrayset;

import java.util.Iterator;

public class SetIterator<E> implements Iterator<E> {

    private final Iterator<E> it;
    public SetIterator(Iterator<E> it) {
        this.it = it;
    }

    @Override
    public boolean hasNext() {
        return it.hasNext();
    }

    @Override
    public E next() {
        return it.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("ArraySet is immutable");
    }
}
