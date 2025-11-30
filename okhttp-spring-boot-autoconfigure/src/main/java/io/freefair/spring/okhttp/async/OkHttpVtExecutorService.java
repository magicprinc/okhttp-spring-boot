package io.freefair.spring.okhttp.async;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/// Very light weight virtual threads [java.util.concurrent.ExecutorService]
/// @see Thread#startVirtualThread(Runnable)
/// @see java.util.concurrent.Executors#newVirtualThreadPerTaskExecutor()
@Slf4j
public class OkHttpVtExecutorService extends AbstractExecutorService implements ThreadFactory, Closeable {
    public static final Thread.UncaughtExceptionHandler THREAD_UNCAUGHT_EXCEPTION_HANDLER = (Thread thread, Throwable failure) ->{
        log.warn("UncaughtExceptionHandler @ {}", thread, failure);
    };

    public static final OkHttpVtExecutorService INSTANCE = new OkHttpVtExecutorService();

    final LongAdder cntAdded = new LongAdder();

    final ThreadFactory virtualThreadFactory;

    public OkHttpVtExecutorService(ThreadFactory virtualThreadFactory) {
        this.virtualThreadFactory = virtualThreadFactory;
    }

    public OkHttpVtExecutorService(String threadNamePrefix) {
        this(Thread.ofVirtual()// Thread.Builder.factory
                .name(threadNamePrefix, 1L)// e.g. OK-
                .uncaughtExceptionHandler(THREAD_UNCAUGHT_EXCEPTION_HANDLER)
                .inheritInheritableThreadLocals(true)
                .factory()
        );
    }//new

    public OkHttpVtExecutorService() {
        this("OK-");
    }//new

    /// @see java.util.concurrent.ThreadPoolExecutor#getTaskCount()
    public long getTaskCount() {
        return cntAdded.longValue();
    }

    @Override
    public void execute(@NonNull Runnable command) {
        newThread(command).start();
    }

    @Override
    public Thread newThread(@NonNull Runnable r) {
        return virtualThreadFactory.newThread(r);
    }

    @Override
    public void shutdown() {
        //no-op
    }

    @Override
    public final List<Runnable> shutdownNow() {
        cntAdded.reset();
        return Collections.emptyList();
    }

    @Override
    public final boolean isShutdown() {
        return false;
    }

    @Override
    public final boolean isTerminated() {
        return false;
    }

    @Override
    public final boolean awaitTermination(long timeout, TimeUnit unit) {
        return true;// prevent 1-DAY lock
    }

    @Override
    public void close() {
        shutdown();// fix JDK 21 ExecutorService#close waits for stopping for 1 DAY
    }
}
