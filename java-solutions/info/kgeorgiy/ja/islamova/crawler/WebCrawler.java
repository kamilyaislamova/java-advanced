package info.kgeorgiy.ja.islamova.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements NewCrawler {
    private final Downloader downloader;
    private final int perHost;
    private final ExecutorService resource;
    private final Semaphore downloadSemaphore;
    private final Semaphore extractSemaphore;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        resource = Executors.newVirtualThreadPerTaskExecutor(); // :NOTE: виртуальные потоки, лучше FixedThreadPool
        downloadSemaphore = new Semaphore(downloaders);
        extractSemaphore = new Semaphore(extractors);
    }

    @Override
    public Result download(String url, int depth, List<String> excludes) {
        ConcurrentMap<String, Semaphore> hostSemaphore = new ConcurrentHashMap<>(); // :NOTE:  perHost -- глобальные
        Set<String> used = ConcurrentHashMap.newKeySet();
        ConcurrentMap<String, IOException> errors = new ConcurrentHashMap<>();
        Set<String> documents = ConcurrentHashMap.newKeySet();
        //trying to fix tests
        Set<String> queue = ConcurrentHashMap.newKeySet();
        queue.add(url);
        for (int i = 0; i < depth; i++) {
            List <String> current = List.copyOf(queue);
            CountDownLatch cdl = new CountDownLatch(queue.size());
            queue.clear();
            for (String link: current) {
                downloadTask(link, used, errors, documents, queue, cdl, excludes, hostSemaphore);
            }
            while (true) {
                try {
                    cdl.await();
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return new Result(new ArrayList<>(documents), errors);
    }

    private void downloadTask(String url,
                              Set<String> used,
                              ConcurrentMap<String, IOException> errors,
                              Set<String> documents,
                              Set<String> queue,
                              CountDownLatch cdl,
                              List<String> excludes,
                              ConcurrentMap<String, Semaphore> hostSemaphore
    ) {
        resource.submit(() -> {
            String host;
            try {
                host = URLUtils.getHost(url);
            } catch (IOException e) {
                cdl.countDown();
                return;
            }
            if (host == null) {
                return;
            }
            Semaphore hs = hostSemaphore.computeIfAbsent(host, _ -> new Semaphore(perHost)); // :NOTE: hostSemaphore никогда не чистится
            hs.acquireUninterruptibly(); // :NOTE: ждать пока осводится perHost не очень, надо откладывать url в очередь для хоста и начинать качать следующую задачу
            downloadSemaphore.acquireUninterruptibly();
            try {
                if (isExcluded(excludes, url) || used.contains(url)) {
                    cdl.countDown();
                    return;
                }
                used.add(url);
                Document doc = downloader.download(url);
                documents.add(url);
                extractLinksTask(doc, queue, cdl);// :NOTE: на последнем уровне можно не леоать extract
            } catch (IOException e) {
                errors.put(url, e);
                cdl.countDown();
            } finally {
                downloadSemaphore.release();
                hs.release();
            }
        });
    }

    private void extractLinksTask(Document doc, Set<String> queue, CountDownLatch cdl) {
        resource.submit(() -> {
            extractSemaphore.acquireUninterruptibly();
            try {
                queue.addAll(doc.extractLinks());
            } catch (IOException _) {
            } finally {
                cdl.countDown();
                //System.out.println(cdl.getCount());
                extractSemaphore.release();
            }
        });
    }

    private boolean isExcluded(List<String> excludes, String url) throws IOException {
        for (String exc: excludes) {
            if (URLUtils.getHost(url).contains(exc)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() {
        resource.close(); // :NOTE: shutdownNow + awaitTermination
    }

    public static void main(String[] args) {
// :NOTE: не хватает
    }
}
