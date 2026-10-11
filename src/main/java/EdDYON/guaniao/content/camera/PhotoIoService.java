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
    private static volatile long generation;

    private PhotoIoService() {
    }

    public static synchronized <T> boolean submit(
            MinecraftServer server,
            Callable<T> work,
            Consumer<T> onSuccess,
            Consumer<Throwable> onFailure
    ) {
        if (!server.isRunning() || server.isStopped()) {
            return false;
        }
        if (executor.isShutdown()) {
            executor = createExecutor();
        }
        long taskGeneration = generation;
        try {
            executor.execute(() -> {
                T result;
                try {
                    result = work.call();
                } catch (Throwable throwable) {
                    dispatch(server, taskGeneration, () -> onFailure.accept(throwable));
                    return;
                }
                dispatch(server, taskGeneration, () -> onSuccess.accept(result));
            });
            return true;
        } catch (RejectedExecutionException exception) {
            GuaniaoMod.LOGGER.warn("Photograph I/O queue is full; rejecting work");
            return false;
        }
    }

    public static synchronized void shutdown() {
        generation++;
        executor.shutdownNow();
    }

    private static void dispatch(MinecraftServer server, long taskGeneration, Runnable callback) {
        if (!isCurrent(server, taskGeneration)) {
            return;
        }
        try {
            server.executeIfPossible(() -> {
                // A stopped MinecraftServer may execute a task on its submitting thread.
                if (isCurrent(server, taskGeneration) && server.isSameThread()) {
                    callback.run();
                }
            });
        } catch (RejectedExecutionException exception) {
            // Server shutdown clears the owning sessions; an old callback must not restore them.
        }
    }

    private static boolean isCurrent(MinecraftServer server, long taskGeneration) {
        return taskGeneration == generation && server.isRunning() && !server.isStopped();
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
