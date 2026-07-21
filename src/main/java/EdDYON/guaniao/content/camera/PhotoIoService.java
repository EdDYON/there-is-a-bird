package EdDYON.guaniao.content.camera;

import EdDYON.guaniao.GuaniaoMod;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;

/** Bounded executor for JPEG decoding, hashing and photograph disk I/O. */
public final class PhotoIoService {
    private static final int THREADS = 2;
    private static final int QUEUE_CAPACITY = 32;
    private static ThreadPoolExecutor executor = createExecutor();

    private PhotoIoService() {
    }

    public static synchronized <T> boolean submit(
            MinecraftServer server,
            Callable<T> work,
            Consumer<T> onSuccess,
            Consumer<Throwable> onFailure
    ) {
        if (executor.isShutdown()) {
            executor = createExecutor();
        }
        try {
            executor.execute(() -> {
                try {
                    T result = work.call();
                    server.execute(() -> onSuccess.accept(result));
                } catch (Throwable throwable) {
                    server.execute(() -> onFailure.accept(throwable));
                }
            });
            return true;
        } catch (RejectedExecutionException exception) {
            GuaniaoMod.LOGGER.warn("Photograph I/O queue is full; rejecting work");
            return false;
        }
    }

    public static synchronized void shutdown() {
        executor.shutdownNow();
    }

    public static synchronized int queuedTasks() {
        return executor.getQueue().size();
    }

    private static ThreadPoolExecutor createExecutor() {
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "Guaniao-Photo-IO");
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((ignored, throwable) ->
                    GuaniaoMod.LOGGER.error("Uncaught photograph I/O error", throwable));
            return thread;
        };
        ThreadPoolExecutor created = new ThreadPoolExecutor(
                THREADS,
                THREADS,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                factory,
                new ThreadPoolExecutor.AbortPolicy()
        );
        created.prestartAllCoreThreads();
        return created;
    }
}
