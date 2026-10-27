package ui;

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.io.PrintWriter;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

/** Runs every JUnit test in the ui package and exits non-zero if any fail (no Maven/Gradle needed). */
public class RunTests {
    public static void main(String[] args) {
        var request = LauncherDiscoveryRequestBuilder.request().selectors(selectPackage("ui")).build();
        var listener = new SummaryGeneratingListener();
        Launcher launcher = LauncherFactory.create();
        launcher.execute(request, listener);
        var summary = listener.getSummary();
        PrintWriter out = new PrintWriter(System.out);
        summary.printTo(out);
        summary.printFailuresTo(out, 15);
        out.flush();
        System.exit(summary.getTotalFailureCount() == 0 ? 0 : 1);
    }
}
