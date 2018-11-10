package ru.ifmo.rain.ionov.arrayset;

import java.util.AbstractList;
import java.util.List;

public class DescendingList<E> extends AbstractList<E> {
    private final List<E> elements;
    private boolean isReverse;

    public DescendingList(List<E> list) {
        elements = list;
        isReverse = false;
    }

    public void reverse() {
        isReverse = !isReverse;
    }

    @Override
    public E get(int index) {
        if (!isReverse) return elements.get(index);
        else return elements.get(size() - index - 1);
    }

    @Override
    public int size() {
        return elements.size();
    }
}
