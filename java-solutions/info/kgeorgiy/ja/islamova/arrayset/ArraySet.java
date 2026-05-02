package info.kgeorgiy.ja.islamova.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {

    private final List<E> elements;
    private final Comparator<? super E> comparator;

    public ArraySet(Collection<E> elements, Comparator<? super E> comparator) {
        this.comparator = comparator;
        TreeSet<E> set = new TreeSet<>(comparator);
        set.addAll(elements);
        this.elements = new ArrayList<>(set);
    }

    private ArraySet(List<E> elements, Comparator<? super E> comparator) {
        this.elements = elements;
        this.comparator = comparator;
    }

    public ArraySet(Collection<E> elements) {
        this(elements, null);
    }

    public ArraySet() {
        this(List.of(), null);
    }

    @SuppressWarnings("unchecked")
    private int binarySearchWar(Object e) {
        return Collections.binarySearch(elements, (E) e, comparator);
    }

    @Override
    public boolean contains(Object e) {
        return binarySearchWar(e) >= 0;
    }

    private E findElement(E e, int c1, int c2) {
        int index = Collections.binarySearch(elements, e, comparator);
        index = index < 0 ? (index + 1) * -1 : index + c1;
        boolean condition = c2 == 1 ? index > 0 : index < elements.size();
        return condition ? elements.get(index - c2) : null;
    }

    @Override
    public E lower(E e) {
        return findElement(e, 0, 1);
    }

    @Override
    public E floor(E e) {
        return findElement(e, 1, 1);
    }

    @Override
    public E ceiling(E e) {
        return findElement(e, 0, 0);
    }

    @Override
    public E higher(E e) {
        return findElement(e, 1, 0);
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException("ArraySet is immutable");
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException("ArraySet is immutable");
    }

    @Override
    public Iterator<E> iterator() {
        return new SetIterator<>(elements.iterator());
    }

    @Override
    public NavigableSet<E> descendingSet() {
//        List<E> reversedList = new ArrayList<>(elements);
//        Collections.reverse(reversedList);
        return new ArraySet<>(elements.reversed(), comparator.reversed());
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    private int reformIndex(int index, boolean inclusive) {
        if (index < 0) {
            index = -index - 1;
        } else if (inclusive) {
            index++;
        }
        return index;
    }

    private ArraySet<E> emptySet() {
        return new ArraySet<>(List.of(), comparator);
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if ((comparator == null && Collections.reverseOrder().reversed().compare(fromElement, toElement) > 0) ||
                (comparator != null && comparator.compare(fromElement, toElement) > 0)) {
            throw new IllegalArgumentException();
        }
        int fromIndex = Collections.binarySearch(elements, fromElement, comparator);
        int toIndex = Collections.binarySearch(elements, toElement, comparator);
        return getSubList(fromInclusive, toInclusive, fromIndex, toIndex);
    }

    private ArraySet<E> getSubList(boolean fromInclusive, boolean toInclusive, int fromIndex, int toIndex) {
        fromIndex = reformIndex(fromIndex, !fromInclusive);
        toIndex = reformIndex(toIndex, toInclusive);
        if (fromIndex > toIndex) {
            return emptySet();
        }
        return new ArraySet<>(elements.subList(fromIndex, toIndex), comparator);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        int toIndex = Collections.binarySearch(elements, toElement, comparator);
        return getSubList(true, inclusive, 0, toIndex);
//        try  {
//            return subSet(first(), true, toElement, inclusive);
//        } catch (NoSuchElementException | IllegalArgumentException e) {
//            return emptySet();
//        }
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        try {
            return subSet(fromElement, inclusive, last(), true);
        } catch (NoSuchElementException | IllegalArgumentException e) {
            return emptySet();
        }
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E first() {
//        if (elements.isEmpty()) {
//            throw new NoSuchElementException();
//        }
        return elements.getFirst();
    }

    @Override
    public E last() {
//        if (elements.isEmpty()) {
//            throw new NoSuchElementException();
//        }
        return elements.getLast();
    }

    @Override
    public int size() {
        return elements.size();
    }

}
