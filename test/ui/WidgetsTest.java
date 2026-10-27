package ui;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WidgetsTest {

    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.startToolkit();
    }

    @Test
    void headerHasTitleAndSubtitleWithStyles() throws Exception {
        VBox header = FxTestSupport.onFxThread(() -> Widgets.header("Home", "Overview"));

        Label title = (Label) header.getChildren().get(0);
        Label subtitle = (Label) header.getChildren().get(1);
        assertEquals("Home", title.getText());
        assertTrue(title.getStyleClass().contains("page-title"));
        assertEquals("Overview", subtitle.getText());
        assertTrue(subtitle.getStyleClass().contains("page-subtitle"));
    }

    @Test
    void pageWrapsSectionsInAScrollPaneThatFitsWidth() throws Exception {
        ScrollPane scroll = FxTestSupport.onFxThread(
                () -> Widgets.page(new Label("one"), new Label("two")));

        assertTrue(scroll.isFitToWidth());
        assertTrue(scroll.getStyleClass().contains("page-scroll"));
        VBox content = (VBox) scroll.getContent();
        assertTrue(content.getStyleClass().contains("page"));
        assertEquals(2, content.getChildren().size());
    }

    @Test
    void captionIsUpperCased() throws Exception {
        Label caption = FxTestSupport.onFxThread(() -> Widgets.caption("Starting word"));

        assertEquals("STARTING WORD", caption.getText());
        assertTrue(caption.getStyleClass().contains("field-label"));
    }

    @Test
    void pillCarriesItsColorClass() throws Exception {
        Label pill = FxTestSupport.onFxThread(() -> Widgets.pill("Done", "green"));

        assertEquals("Done", pill.getText());
        assertTrue(pill.getStyleClass().containsAll(java.util.List.of("pill", "green")));
    }

    @Test
    void mutedUsesMutedStyle() throws Exception {
        Label muted = FxTestSupport.onFxThread(() -> Widgets.muted("hint"));

        assertEquals("hint", muted.getText());
        assertTrue(muted.getStyleClass().contains("muted"));
    }
}
