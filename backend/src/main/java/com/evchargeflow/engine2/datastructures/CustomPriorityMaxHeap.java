package com.evchargeflow.engine2.datastructures;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Engine 2: Custom array-backed binary Max Heap implementation.
 * Does not delegate to java.util.PriorityQueue.
 *
 * Comparator contract: compare(a, b) > 0 indicates `a` has strictly higher priority than `b`.
 */
public class CustomPriorityMaxHeap<T> {

    private final ArrayList<T> heap = new ArrayList<>();
    private final Comparator<T> comparator;

    public CustomPriorityMaxHeap(Comparator<T> comparator) {
        if (comparator == null) {
            throw new IllegalArgumentException("Comparator must not be null.");
        }
        this.comparator = comparator;
    }

    public synchronized void insert(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot insert null element into priority heap.");
        }
        heap.add(item);
        heapifyUp(heap.size() - 1);
    }

    public synchronized T peek() {
        return heap.isEmpty() ? null : heap.get(0);
    }

    public synchronized T extractMax() {
        if (heap.isEmpty()) {
            return null;
        }
        T max = heap.get(0);
        T last = heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) {
            heap.set(0, last);
            heapifyDown(0);
        }
        return max;
    }

    public synchronized boolean remove(T item) {
        int index = heap.indexOf(item);
        if (index < 0) {
            return false;
        }
        T last = heap.remove(heap.size() - 1);
        if (index < heap.size()) {
            heap.set(index, last);
            heapifyDown(index);
            heapifyUp(index);
        }
        return true;
    }

    /**
     * Rebuilds heap invariants in O(n) after element priority values change.
     */
    public synchronized void rebuild() {
        for (int i = (heap.size() / 2) - 1; i >= 0; i--) {
            heapifyDown(i);
        }
    }

    public synchronized void clear() {
        heap.clear();
    }

    public synchronized int size() {
        return heap.size();
    }

    public synchronized boolean isEmpty() {
        return heap.isEmpty();
    }

    /**
     * Returns a non-destructive snapshot sorted in descending priority order using heap extraction on a copy.
     */
    public synchronized List<T> toSortedList() {
        CustomPriorityMaxHeap<T> copy = new CustomPriorityMaxHeap<>(comparator);
        copy.heap.addAll(this.heap);
        copy.rebuild();

        List<T> ordered = new ArrayList<>(copy.size());
        while (!copy.isEmpty()) {
            ordered.add(copy.extractMax());
        }
        return ordered;
    }

    public synchronized void heapifyUp(int index) {
        int current = index;
        while (current > 0) {
            int parent = (current - 1) / 2;
            if (comparator.compare(heap.get(current), heap.get(parent)) > 0) {
                swap(current, parent);
                current = parent;
            } else {
                break;
            }
        }
    }

    public synchronized void heapifyDown(int index) {
        int size = heap.size();
        int current = index;
        while (true) {
            int left = 2 * current + 1;
            int right = 2 * current + 2;
            int largest = current;

            if (left < size && comparator.compare(heap.get(left), heap.get(largest)) > 0) {
                largest = left;
            }
            if (right < size && comparator.compare(heap.get(right), heap.get(largest)) > 0) {
                largest = right;
            }
            if (largest == current) {
                break;
            }
            swap(current, largest);
            current = largest;
        }
    }

    private void swap(int i, int j) {
        T temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);
    }
}
