package ui;

import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** Every page App can open must build without throwing and return a non-empty view. */
class PagesSmokeTest {

    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.startToolkit();
    }

    @Test
    void everyPageBuilds() throws Exception {
        Map<String, Supplier<Parent>> pages = new LinkedHashMap<>();
        pages.put(Sidebar.HOME, () -> new HomePage(p -> { }).getView());
        pages.put(Sidebar.IMPORT, () -> new ImportPage().getView());
        pages.put(Sidebar.GENERATE, () -> new GeneratePage().getView());
        pages.put(Sidebar.BATCH, () -> new BatchPage().getView());
        pages.put(Sidebar.AUTOCOMPLETE, () -> new AutoCompletePage().getView());
        pages.put(Sidebar.WORDS, () -> new WordExplorerPage().getView());
        pages.put(Sidebar.REPORTS, () -> new ReportsPage().getView());

        for (var entry : pages.entrySet()) {
            Parent view = FxTestSupport.onFxThread(() -> entry.getValue().get());
            assertNotNull(view, entry.getKey());
            assertFalse(FxTestSupport.findAll(view, javafx.scene.Node.class).size() < 5,
                    entry.getKey() + " looks empty");
        }
    }

    @Test
    void everyStylesheetThePagesLoadExists() {
        for (String css : new String[] {"styles", "import", "generate", "batch", "autocomplete", "explorer", "reports"}) {
            assertTrue(new java.io.File("src/ui/css/" + css + ".css").isFile(), css + ".css is missing");
        }
    }
}
