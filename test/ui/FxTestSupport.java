package ui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/** Starts the JavaFX toolkit once and gives tests ways to build pages and find nodes in them. */
final class FxTestSupport {

    private static boolean started;

    private FxTestSupport() {
    }

    static synchronized void startToolkit() throws InterruptedException {
        if (started) {
            return;
        }
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyRunning) {
            ready.countDown();
        }
        if (!ready.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX toolkit did not start");
        }
        started = true;
    }

    /** Runs the code on the JavaFX thread and returns its result (or rethrows its failure). */
    static <T> T onFxThread(Callable<T> work) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return work.call();
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(work.call());
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });
        if (!done.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX thread did not finish in time");
        }
        if (failure.get() != null) {
            throw new RuntimeException(failure.get());
        }
        return result.get();
    }

    static void onFxThread(Runnable work) throws Exception {
        onFxThread(() -> {
            work.run();
            return null;
        });
    }

    /** Every node under root (including root) that has the given type and passes the filter. */
    static <T extends Node> List<T> findAll(Node root, Class<T> type, Predicate<T> filter) {
        List<T> found = new ArrayList<>();
        collect(root, type, filter, found);
        return found;
    }

    static <T extends Node> List<T> findAll(Node root, Class<T> type) {
        return findAll(root, type, node -> true);
    }

    /** The first node of this type that passes the filter; fails the test if there is none. */
    static <T extends Node> T find(Node root, Class<T> type, Predicate<T> filter) {
        return findAll(root, type, filter).stream().findFirst()
                .orElseThrow(() -> new AssertionError("No " + type.getSimpleName() + " matched under " + root));
    }

    private static <T extends Node> void collect(Node node, Class<T> type, Predicate<T> filter, List<T> found) {
        if (type.isInstance(node) && filter.test(type.cast(node))) {
            found.add(type.cast(node));
        }
        if (node instanceof ScrollPane scroll && scroll.getContent() != null) {
            collect(scroll.getContent(), type, filter, found);
        } else if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collect(child, type, filter, found);
            }
        }
    }
}
