package ui;

import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HomePageTest {

    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.startToolkit();
    }

    /** Builds the page, clicks the button with this text, and returns the pages it navigated to. */
    private static List<String> clickAndCollect(String buttonText) throws Exception {
        List<String> navigated = new ArrayList<>();
        HomePage home = FxTestSupport.onFxThread(() -> new HomePage(navigated::add));
        FxTestSupport.onFxThread(() ->
                FxTestSupport.find(home.getView(), Button.class, b -> buttonText.equals(b.getText())).fire());
        return navigated;
    }

    @Test
    void importButtonOpensImportPage() throws Exception {
        assertEquals(List.of(Sidebar.IMPORT), clickAndCollect("Import text file"));
    }

    @Test
    void quickActionsOpenTheirPages() throws Exception {
        assertEquals(List.of(Sidebar.GENERATE), clickAndCollect("Generate a sentence"));
        assertEquals(List.of(Sidebar.BATCH), clickAndCollect("Batch generate sentences"));
        assertEquals(List.of(Sidebar.AUTOCOMPLETE), clickAndCollect("Write with auto-complete"));
        assertEquals(List.of(Sidebar.WORDS), clickAndCollect("Browse words"));
    }

    @Test
    void cardLinksOpenTheirPages() throws Exception {
        assertEquals(List.of(Sidebar.IMPORT), clickAndCollect("View all"));
        assertEquals(List.of(Sidebar.REPORTS), clickAndCollect("All sentences"));
    }

    @Test
    void everyNavigationTargetIsAKnownSidebarPage() throws Exception {
        List<String> targets = new ArrayList<>();
        HomePage home = FxTestSupport.onFxThread(() -> new HomePage(targets::add));
        FxTestSupport.onFxThread(() -> FxTestSupport.findAll(home.getView(), Button.class,
                b -> b.getStyleClass().contains("action") || b.getStyleClass().contains("link")
                        || b.getStyleClass().contains("primary")).forEach(Button::fire));

        List<String> known = List.of(Sidebar.HOME, Sidebar.IMPORT, Sidebar.GENERATE, Sidebar.BATCH,
                Sidebar.AUTOCOMPLETE, Sidebar.WORDS, Sidebar.REPORTS);
        assertFalse(targets.isEmpty());
        assertTrue(known.containsAll(targets), targets.toString());
    }
}
