package ru.ifmo.rain.ionov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

/**
 * Implementation for interface {@link ParallelMapper}
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threads;
    private final Queue<Runnable> tasks;

    /**
     * Create exemplar of {@link ParallelMapperImpl} with given count of threads
     *
     * @param threads how many threads we can use in calculating
     */
    public ParallelMapperImpl(int threads) {
        this.threads = new ArrayList<>();
        tasks = new ArrayDeque<>();
        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        Runnable task;
                        synchronized (tasks) {
                            while (tasks.isEmpty()) {
                                tasks.wait();
                            }
                            task = tasks.poll();
                        }
                        try {
                            task.run();
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                } catch (InterruptedException ignored) {

                }
            });
            thread.start();
            this.threads.add(thread);
        }
    }

    /**
     * This method maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @param f    function which we want to apply
     * @param args list of values to function
     * @param <T>  type of elements in given list
     * @param <R>  type of elements in list of answers
     * @return list after apply given function
     * @throws InterruptedException if one of created threads was interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        List<R> answers = new ArrayList<>(Collections.nCopies(args.size(), null));
        final int[] ansSize = {0};
        for (int i = 0; i < args.size(); i++) {
            final T task = args.get(i);
            final int index = i;
            synchronized (tasks) {
                tasks.add(() -> {
                    R ans = f.apply(task);
                    synchronized (answers) {
                        answers.set(index, ans);
                        synchronized (ansSize) {
                            ansSize[0]++;
                            if (ansSize[0] == args.size()) {
                                ansSize.notifyAll();
                            }
                        }
                        answers.notify();
                    }
                });
                tasks.notifyAll();
            }
        }
        synchronized (ansSize) {
            while (ansSize[0] < args.size()) {
                ansSize.wait();
            }
        }
        return answers;
    }

    /**
     * This method stops all threads. All unfinished mappings leave in undefined state.
     */
    @Override
    public void close() {
        threads.forEach(Thread::interrupt);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}