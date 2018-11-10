package ru.ifmo.rain.ionov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.min;

/**
 * Implementation for interface {@link ListIP}
 */
public class IterativeParallelism implements ListIP {

    private final ParallelMapper mapper;

    /**
     * Create exemplar of {@link IterativeParallelism} with given {@link ParallelMapper}
     *
     * @param mapper {@link ParallelMapper} which we want to use in concurrent programming
     */
    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Create exemplar of {@link IterativeParallelism} with {@link ParallelMapper} = null
     */
    public IterativeParallelism() {
        this.mapper = null;
    }

    /**
     * This method apply given function {@code f} to given values {@code values} using given count of threads {@code threads}
     * After that method apply second function {@code ft} to list of new values
     *
     * @param threads how many threads we can use in calculating
     * @param values  list in which we want to apply function
     * @param f       first function which we want to apply
     * @param ft      second function which we want to apply after first function
     * @param <T>     type of elements in given list
     * @param <R>     type of answer
     * @return answer which we find after using first and second functions on given list
     * @throws InterruptedException if one of created threads was interrupted
     */
    private <T, E, R> R submit(int threads, List<? extends T> values, Function<Stream<? extends T>, E> f, Function<Stream<? extends E>, R> ft) throws InterruptedException {
        threads = min(threads, values.size());
        int elementsCount = values.size() / threads + (values.size() % threads != 0 ? 1 : 0);
        List<E> arrayListAnswers;
        ArrayList<Stream<? extends T>> streams = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            streams.add(values.subList(min(i * elementsCount, values.size()), min((i + 1) * elementsCount, values.size())).stream());
        }
        if (mapper != null) {
            arrayListAnswers = mapper.map(f, streams);
        } else {
            arrayListAnswers = new ArrayList<>(Collections.nCopies(threads, null));
            ArrayList<Thread> threadArrayList = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                final int index = i;
                final Stream<? extends T> splitValues = streams.get(i);
                Thread thread = new Thread(() -> {
                    E ans = f.apply(splitValues);
                    arrayListAnswers.set(index, ans);
                });
                thread.start();
                threadArrayList.add(thread);
            }
            for (Thread thread : threadArrayList) {
                thread.join();
            }
        }
        arrayListAnswers.removeIf(Objects::isNull);
        return ft.apply(arrayListAnswers.stream());
    }

    /**
     * This method check threads, values and comparator on correctness
     *
     * @param threads    parameter which we want to check
     * @param values     parameter which we want to check
     * @param comparator parameter which we want to check
     * @param <T>        type of elements in list
     * @throws InterruptedException if at least one parameter not correct
     */
    private <T> void checkNumFunc(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        checkData(threads, values);
        if (comparator == null) {
            throw new InterruptedException("Error: comparator must be not null");
        }
    }

    /**
     * This method check threads, values and predicate on correctness
     *
     * @param threads   parameter which we want to check
     * @param values    parameter which we want to check
     * @param predicate parameter which we want to check
     * @param <T>       type of elements in list
     * @throws InterruptedException if at least one parameter not correct
     */
    private <T> void checkPredicateFunc(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        checkData(threads, values);
        if (predicate == null) {
            throw new InterruptedException("Error: predicate must be not null");
        }
    }

    /**
     * This method check threads and values on correctness
     *
     * @param threads parameter which we want to check
     * @param values  parameter which we want to check
     * @param <T>     type of elements in list
     * @throws InterruptedException if at least one parameter not correct
     */
    private <T> void checkData(int threads, List<? extends T> values) throws InterruptedException {
        if (values == null) {
            throw new InterruptedException("Error: values must be not null");
        }
        if (values.isEmpty()) {
            throw new InterruptedException("Error: values size must be > 0");
        }
        if (threads <= 0) {
            throw new InterruptedException("Error: count of threads must be > 0");
        }
    }


    /**
     * This method finds max element in list {@code values} using concurrent programming and return it
     *
     * @param threads    how many threads we can use in calculating
     * @param values     list in which we are looking for max element
     * @param comparator using to compare elements
     * @param <T>        type of elements in given list
     * @return max element in given list
     * @throws InterruptedException if one of created threads was interrupted
     */
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        checkNumFunc(threads, values, comparator);
        return submit(threads, values, element -> element.max(comparator).orElse(null), element -> element.max(comparator).orElse(null));
    }

    /**
     * This method finds min element in list {@code values} using concurrent programming and return it
     *
     * @param threads    how many threads we can use in calculating
     * @param values     list in which we are looking for min element
     * @param comparator using to compare elements
     * @param <T>        type of elements in given list
     * @return min element in given list
     * @throws InterruptedException if one of created threads was interrupted
     */
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    /**
     * This method allow us to know if all elements in list {@code values} satisfy the predicate {@code predicate}
     *
     * @param threads   how many threads we can use in calculating
     * @param values    list whose elements we check using predicate
     * @param predicate condition for checking elements
     * @param <T>       type of elements in given list
     * @return {@code true} if all elements satisfy the predicate; {@code false} otherwise
     * @throws InterruptedException if one of created threads was interrupted
     */
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        checkPredicateFunc(threads, values, predicate);
        return submit(threads, values, element -> element.allMatch(predicate), element -> element.allMatch(Boolean::booleanValue));
    }

    /**
     * This method allow us to know if in list exist {@code values} element which satisfy the predicate {@code predicate}
     *
     * @param threads   how many threads we can use in calculating
     * @param values    list whose elements we check using predicate
     * @param predicate condition for checking elements
     * @param <T>       type of elements in given list
     * @return {@code true} if exist element which satisfy the predicate; {@code false} otherwise
     * @throws InterruptedException if one of created threads was interrupted
     */
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    /**
     * This method concatenate string representation of all elements from {@code values} and return this string
     *
     * @param threads how many threads we can use in calculating
     * @param values  list whose elements we concatenate
     * @return string which contains string representation of all elements from {@code values}
     * @throws InterruptedException if one of created threads was interrupted
     */
    public String join(int threads, List<?> values) throws InterruptedException {
        checkData(threads, values);
        return submit(threads, values, element -> element.map(Object::toString).collect(Collectors.joining()), element -> element.collect(Collectors.joining()));
    }

    /**
     * This method check if elements from {@code values} satisfy the predicate {@code predicate}
     *
     * @param threads   how many threads we can use in calculating
     * @param values    list whose elements we check using predicate
     * @param predicate condition for checking elements
     * @param <T>       type of elements in given list
     * @return list with elements which satisfy the predicate
     * @throws InterruptedException if one of created threads was interrupted
     */
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        checkPredicateFunc(threads, values, predicate);
        return submit(threads, values, element -> element.filter(predicate), element -> element.flatMap(Function.identity()).collect(Collectors.toCollection(ArrayList::new)));
    }

    /**
     * This method apply {@code f} to {@code values}
     *
     * @param threads how many threads we can use in calculating
     * @param values  list to the elements of which we want to apply {@code f}
     * @param f       function which we want to apply
     * @param <T>     type of elements in given list
     * @param <U>     type of elements which {@code f} return
     * @return list with elements after applying {@code f}
     * @throws InterruptedException if one of created threads was interrupted
     */
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        checkData(threads, values);
        return submit(threads, values, element -> element.map(f), element -> element.flatMap(Function.identity()).collect(Collectors.toCollection(ArrayList::new)));
    }
}