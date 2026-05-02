package info.kgeorgiy.ja.islamova.iterative;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    private static class Task<T, R> {

        public Task(T item, Function<? super T, ? extends R> func) {
            this.item = item;
            this.func = func;
        }
        T item;
        Function<? super T, ? extends R> func;
        R result = null;
        RuntimeException ex = null;
        boolean exDone = false;

        public synchronized void exec() {
            try {
                result = func.apply(item);
            } catch (RuntimeException e) {
                ex = e;
            }
            exDone = true;
            this.notify();
        }

        public boolean isDone() {
            return exDone;
        }

        public R getResult() throws RuntimeException {
            if (ex != null) {
                throw ex;
            }
            return result;
        }

    }

    private final Queue<Task<?, ?>> taskQueue = new ArrayDeque<>();
    private final List<Thread> workers;
    private boolean isClosed;

    /**
     * Constructor of ParallelMapperImpl
     *
     * @param threads the number of working threads
     */
    public ParallelMapperImpl(int threads) {
        isClosed = false;
        workers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread worker = new Thread(() -> {
                    while (!Thread.interrupted()) {
                        try {
                            Task<?, ?> task;
                            synchronized (taskQueue) {
                                while (taskQueue.isEmpty() && !isClosed) {
                                    taskQueue.wait();
                                }
                                if (isClosed) {
                                    return; //  :NOTE: кидать ошибку
                                }
                                task = taskQueue.poll();
                            }
                            task.exec();
                        } catch (InterruptedException e) {
                            return; // :NOTE: лишний return
                        }
                    }
            });
            worker.start();
            workers.add(worker);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> items) throws InterruptedException {
        if (isClosed) {
            throw new IllegalStateException("mapper is closed: cannot map item with function");
        }

        final List<R> result = new ArrayList<>(Collections.nCopies(items.size(), null));
        final List<Task<T, R>> tasks = new ArrayList<>();

        for (T item : items) {
            Task<T, R> task = new Task<>(item, f);
            tasks.add(task);
            synchronized (taskQueue) {
                taskQueue.add(task);
                taskQueue.notify();
            }
        }

        for (int i = 0; i < tasks.size(); i++) {
            Task<T, R> task = tasks.get(i);
            synchronized (task) {
                while(!task.isDone()) {
                    task.wait();
                }
            }
            result.set(i, task.getResult());
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        // :NOTE: if (isClosed) { }
        isClosed = true;
        for (Thread thr : workers) {
            thr.interrupt();
            try {
                thr.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
