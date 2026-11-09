package ui;

import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SidebarTest {

    private static final List<String> PAGES = List.of(
            Sidebar.HOME, Sidebar.IMPORT, Sidebar.GENERATE, Sidebar.BATCH,
            Sidebar.AUTOCOMPLETE, Sidebar.WORDS, Sidebar.REPORTS);

    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.startToolkit();
    }

    private static List<Button> navButtons(Sidebar sidebar) {
        return FxTestSupport.findAll(sidebar.getView(), Button.class,
                b -> b.getStyleClass().contains("nav-button"));
    }

    @Test
    void hasOneNavButtonPerPageInOrder() throws Exception {
        Sidebar sidebar = FxTestSupport.onFxThread(() -> new Sidebar(page -> { }));

        assertEquals(PAGES, navButtons(sidebar).stream().map(Button::getText).toList());
    }

    @Test
    void clickingAButtonReportsItsPageName() throws Exception {
        List<String> clicked = new ArrayList<>();
        Sidebar sidebar = FxTestSupport.onFxThread(() -> new Sidebar(clicked::add));

        FxTestSupport.onFxThread(() -> navButtons(sidebar).forEach(Button::fire));

        assertEquals(PAGES, clicked);
    }

    @Test
    void setActiveHighlightsOnlyThatButton() throws Exception {
        Sidebar sidebar = FxTestSupport.onFxThread(() -> new Sidebar(page -> { }));

        FxTestSupport.onFxThread(() -> sidebar.setActive(Sidebar.BATCH));

        for (Button button : navButtons(sidebar)) {
            assertEquals(button.getText().equals(Sidebar.BATCH), button.getStyleClass().contains("active"),
                    button.getText());
        }
    }

    @Test
    void setActiveMovesTheHighlightAndNeverDuplicatesIt() throws Exception {
        Sidebar sidebar = FxTestSupport.onFxThread(() -> new Sidebar(page -> { }));

        FxTestSupport.onFxThread(() -> {
            sidebar.setActive(Sidebar.HOME);
            sidebar.setActive(Sidebar.HOME);
            sidebar.setActive(Sidebar.REPORTS);
        });

        List<Button> active = navButtons(sidebar).stream()
                .filter(b -> b.getStyleClass().contains("active")).toList();
        assertEquals(1, active.size());
        assertEquals(Sidebar.REPORTS, active.get(0).getText());
        assertEquals(1, active.get(0).getStyleClass().stream().filter("active"::equals).count());
    }

    @Test
    void unknownPageLeavesNothingHighlighted() throws Exception {
        Sidebar sidebar = FxTestSupport.onFxThread(() -> new Sidebar(page -> { }));

        FxTestSupport.onFxThread(() -> {
            sidebar.setActive(Sidebar.HOME);
            sidebar.setActive("Nope");
        });

        assertTrue(navButtons(sidebar).stream().noneMatch(b -> b.getStyleClass().contains("active")));
    }
}
