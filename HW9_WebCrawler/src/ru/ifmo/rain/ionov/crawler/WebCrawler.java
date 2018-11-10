package ru.ifmo.rain.ionov.crawler;

import info.kgeorgiy.java.advanced.crawler.CachingDownloader;
import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService downThreadPool;
    private final ExecutorService extraThreadPool;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        downThreadPool = Executors.newFixedThreadPool(downloaders);
        extraThreadPool = Executors.newFixedThreadPool(extractors);
    }

    private WebCrawler(int downloaders, int extractors, int perHost) throws IOException {
        this(new CachingDownloader(), downloaders, extractors, perHost);
    }

    private WebCrawler(int downloaders, int extractors) throws IOException {
        this(downloaders, extractors, 0);
    }

    private WebCrawler(int downloaders) throws IOException {
        this(downloaders, 8);
    }

    private WebCrawler() throws IOException {
        this(8);
    }

    private void getLinks(final Document page, final int depth, final Set<String> pages, final Map<String, IOException> errorPages, final Set<String> visited, final Phaser phaser) {
        try {
            page.extractLinks().forEach(
                    link -> {
                        if (visited.add(link)) {
                            phaser.register();
                            downThreadPool.submit(() -> downloadImpl(link, depth - 1, pages, errorPages, visited, phaser));
                        }
                    }
            );
        } catch (IOException ignored) {
        } finally {
            phaser.arrive();
        }
    }

    private void downloadImpl(final String url, final int depth, final Set<String> pages, final Map<String, IOException> errorPages, final Set<String> visited, final Phaser phaser) {
        final Runnable task = () -> {
            try {
                final Document page = downloader.download(url);
                pages.add(url);
                if (depth > 1) {
                    phaser.register();
                    extraThreadPool.submit(() -> getLinks(page, depth, pages, errorPages, visited, phaser));
                }
            } catch (IOException e) {
                errorPages.put(url, e);
            } finally {
                phaser.arrive();
            }
        };
        phaser.register();
        downThreadPool.submit(task);
        phaser.arrive();
    }

    @Override
    public Result download(String url, int depth) {
        final Set<String> pages = Collections.newSetFromMap(new ConcurrentHashMap<>());
        final Map<String, IOException> errorPages = new ConcurrentHashMap<>();
        final Set<String> visited = Collections.newSetFromMap(new ConcurrentHashMap<>());
        final Phaser phaser = new Phaser(2);
        visited.add(url);
        downloadImpl(url, depth, pages, errorPages, visited, phaser);
        phaser.arriveAndAwaitAdvance();
        return new Result(new ArrayList<>(pages), errorPages);
    }

    @Override
    public void close() {
        downThreadPool.shutdownNow();
        extraThreadPool.shutdownNow();
    }

    public static void main(String[] args) {
        if (args == null || args.length < 2 || args.length > 5) {
            System.err.println("Error: required from 2 to 5 arguments");
        } else {
            if (Arrays.stream(args).anyMatch(Objects::isNull)) {
                System.err.println("Error: required not null arguments");
            } else {
                int[] tmp = new int[args.length - 1];
                for (int i = 1; i < args.length; i++) {
                    try {
                        tmp[i - 1] = Integer.parseInt(args[i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Error: required num arguments");
                        return;
                    }
                }
                WebCrawler crawler = null;
                try {
                    switch (tmp.length) {
                        case 4:
                        case 3:
                            crawler = new WebCrawler(tmp[1], tmp[2]);
                            break;
                        case 2:
                            crawler = new WebCrawler(tmp[1]);
                            break;
                        case 1:
                            crawler = new WebCrawler();
                    }
                    Objects.requireNonNull(crawler).download(args[0], tmp[0]);
                    crawler.close();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }
}