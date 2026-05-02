package info.kgeorgiy.ja.islamova.iterative;

import info.kgeorgiy.java.advanced.iterative.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


// :NOTE: code style
// :NOTE: реализация методов интерфейса ожидается по 1-2 строки.

/**
 * List iterative parallelism realisation
 * Implementation of {@link ListIP}
 */
public class IterativeParallelism implements ListIP {
    private ParallelMapper mapper = null;

    /**
     * Default constructor of IterativeParallelism
     */
    public IterativeParallelism() {
    }

    /**
     * Constructor of IterativeParallelism
     *
     * @param mapper the instance of {@link ParallelMapper}
     */
    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int[] indices(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        List<List<Integer>> results = threadsWorking(threads, values, (res, ind) -> {
            T current = values.get(ind);
            if (predicate.test(current)) {
                res.add(ind);
            }
            return res;
        }, ArrayList::new);

        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < Math.min(threads, values.size()); i++) {
            result.addAll(results.get(i));
        }

        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    private <T, R> List<R> listProcess(int threads, List<? extends T> values,
                                       BiFunction<List<R>, Integer, List<R>> processor) throws InterruptedException {
        List<List<R>> results = threadsWorking(threads, values, processor, ArrayList::new);

        List<R> result = new ArrayList<>();
        for (int i = 0; i < Math.min(threads, values.size()); i++) {
            result.addAll(results.get(i));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return listProcess(threads, values, (res, ind) -> {
            T current = values.get(ind);
            if (predicate.test(current)) {
                res.add(current);
            }
            return res;
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        Objects.requireNonNull(f, "function can't be null");

        return listProcess(threads, values, (res, ind) -> {
            T current = values.get(ind);
            res.add(f.apply(current));
            return res;
        });
    }

    private <T, R> List<R> threadsWorking(int threads, List<T> values, BiFunction<R, Integer, R> func, Supplier<R> init) throws InterruptedException {
        int size = values.size();
        threads = Math.min(threads, size);
        int chunkSize = size / threads;
        int remainder = size % threads;

        List<R> result = new ArrayList<>(Collections.nCopies(threads, null));
        List<Thread> workers = new ArrayList<>();
        List<Pair<List<T>, Integer>> chunks = new ArrayList<>();

        int cnt = 0;
        for (int i = 0; i < values.size(); ) {
            final int start = i;
            final int end = start + chunkSize + (cnt < remainder ? 1 : 0);
            final int threadIndex = cnt++;

            if (mapper != null) {
                chunks.add(new Pair<>(values.subList(start, end), start));
            } else {
                Thread worker = new Thread(() -> {
                    R localResult = init.get();

                    for (int j = start; j < end; j++) {
                        localResult = func.apply(localResult, j);
                    }

                    result.set(threadIndex, localResult);
                });
                workers.add(worker);
                worker.start();
            }
            i = end;
        }

        if (mapper != null) {
            return mapper.map(it -> {
                R localResult = init.get();

                for (int j = 0; j < it.first().size(); j++) {
                    localResult = func.apply(localResult, j + it.second());
                }

                return localResult;
            }, chunks);
        }
        try {
            for (Thread worker : workers) {
                worker.join();
            }
        } catch (InterruptedException e) {
            for (Thread worker : workers) {
                worker.interrupt();
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int argMax(int threads, List<T> values, Comparator<? super T> comparator) throws InterruptedException {
        List<Integer> maxIndices = threadsWorking(threads, values, (res, ind) -> {
            T current = values.get(ind);
            if (comparator.compare(current, values.get(res)) > 0) {
                res = ind;
            }
            return res;
        }, () -> 0);

        int globalIndex = maxIndices.getFirst();

        for (int i = 1; i < Math.min(threads, values.size()); i++) {
            if (comparator.compare(values.get(maxIndices.get(i)), values.get(globalIndex)) > 0) {
                globalIndex = maxIndices.get(i);
            }
        }

        return globalIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int argMin(int threads, List<T> values, Comparator<? super T> comparator) throws InterruptedException {
        return argMax(threads, values, comparator.reversed());
    }

    private <T> int getIndex(int threads, List<T> values, Predicate<? super T> predicate, Function<int[], Integer> index) throws InterruptedException {
        int[] results = indices(threads, values, predicate);
        if (results.length == 0) {
            return -1;
        }
        return index.apply(results);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int indexOf(int threads, List<T> values, Predicate<? super T> predicate) throws InterruptedException {
        return getIndex(threads, values, predicate, results -> results[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int lastIndexOf(int threads, List<T> values, Predicate<? super T> predicate) throws InterruptedException {
        return getIndex(threads, values, predicate, results -> results[results.length - 1]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> long sumIndices(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        List<Long> results = threadsWorking(threads, values, (res, ind) -> {
            if (predicate.test(values.get(ind))) {
                res += ind;
            }
            return res;
        }, () -> 0L);

        long result = 0;

        for (int i = 0; i < Math.min(threads, values.size()); i++) {
            result += results.get(i);
        }

        return result;
    }

    private record Pair<T, R>(T first, R second) {
    }
}
