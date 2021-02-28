package cloud.gcp.service;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

/**
 * Synchronized Linked List with max size constrain
 *
 * @param <T>
 */
public class SynchronizedLinkedList<T> {

    private static final int MAX_SIZE = 1000;

    private final Deque<T> list;

    private final Object lock;

    private final int maxSize;

    SynchronizedLinkedList() {
        this(MAX_SIZE);
    }

    public SynchronizedLinkedList(int maxSize) {
        this.list = new LinkedList<>();
        this.maxSize = maxSize;
        lock = "lock";
    }

    T add(T object) {
        synchronized (lock) {
            T removed = null;
            if (list.size() > maxSize) {
                removed = list.pollFirst();
            }
            list.add(object);
            return removed;
        }
    }

    public int size() {
        synchronized (lock) {
            return list.size();
        }
    }

    Optional<T> getLast() {
        synchronized (lock) {
            return list.isEmpty() ? Optional.empty() : Optional.of(list.pollLast());
        }
    }

    LinkedList<T> copy() {
        LinkedList<T> copy = null;
        synchronized (lock) {
            copy = new LinkedList<>(this.list);
        }
        return copy;
    }

    LinkedList<T> copy(int size) {
        LinkedList<T> copy = new LinkedList<>();
        int toRemove = 0;
        synchronized (lock) {
            copy = new LinkedList<>(this.list);
            toRemove = this.list.size() - size;
        }
        while (toRemove-- > 0 && !copy.isEmpty()) {
            copy.removeFirst();
        }
        return copy;
    }


}
