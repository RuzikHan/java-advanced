package ru.ifmo.rain.ionov.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final List<E> elements;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        elements = Collections.emptyList();
        comparator = null;
    }

    public ArraySet(Collection<E> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<E> collection, Comparator<? super E> comp) {
        TreeSet<E> treeSet = new TreeSet<>(comp);
        treeSet.addAll(collection);
        elements = new ArrayList<>(treeSet);
        comparator = comp;
    }

    private ArraySet(List<E> list, Comparator<? super E> comp, boolean sort) {
        elements = list;
        comparator = comp;
        if (list instanceof DescendingList) {
            ((DescendingList) list).reverse();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(elements).iterator();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public E first() {
        if (isEmpty()) {
            throw new NoSuchElementException("No first element");
        }
        return elements.get(0);
    }

    @Override
    public E last() {
        if (isEmpty()) {
            throw new NoSuchElementException("No last element");
        }
        return elements.get(size() - 1);
    }

    @Override
    @SuppressWarnings("Unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(elements, (E) o, comparator) >= 0;
    }

    private int positionHeadCheck(int i, boolean f) {
        if (i < 0) {
            return (-i - 1);
        }
        if (!f) {
            return i + 1;
        }
        return i;
    }

    private int positionTailCheck(int i, boolean f) {
        if (i < 0) {
            return (-i - 2);
        }
        if (!f) {
            return i - 1;
        }
        return i;
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
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public E floor(E e) {
        int pos = positionTailCheck(Collections.binarySearch(elements, e, comparator), true);
        if (pos >= 0) return elements.get(pos);
        else return null;
    }

    @Override
    public E ceiling(E e) {
        int pos = positionHeadCheck(Collections.binarySearch(elements, e, comparator), true);
        if (pos < elements.size()) return elements.get(pos);
        else return null;
    }

    @Override
    public E higher(E e) {
        int pos = positionHeadCheck(Collections.binarySearch(elements, e, comparator), false);
        if (pos < elements.size()) return elements.get(pos);
        else return null;
    }

    @Override
    public E lower(E e) {
        int pos = positionTailCheck(Collections.binarySearch(elements, e, comparator), false);
        if (pos >= 0) return elements.get(pos);
        else return null;
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(new DescendingList<>(elements), Collections.reverseOrder(comparator));
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        int pos = positionTailCheck(Collections.binarySearch(elements, toElement, comparator), inclusive) + 1;
        return new ArraySet<>(elements.subList(0, pos), comparator, true);
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        int posFrom = positionHeadCheck(Collections.binarySearch(elements, fromElement, comparator), fromInclusive), posTo = positionTailCheck(Collections.binarySearch(elements, toElement, comparator), toInclusive) + 1;
        if (posTo + 1 == posFrom) {
            posTo = posFrom;
        }
        return new ArraySet<>(elements.subList(posFrom, posTo), comparator, true);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        int posFrom = positionHeadCheck(Collections.binarySearch(elements, fromElement, comparator), inclusive);
        return new ArraySet<>(elements.subList(posFrom, size()), comparator, true);
    }
}