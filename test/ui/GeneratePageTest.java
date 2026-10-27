package ui;

import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeneratePageTest {

    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.startToolkit();
    }

    private static GeneratePage newPage() throws Exception {
        return FxTestSupport.onFxThread(GeneratePage::new);
    }

    private static List<RadioButton> radios(GeneratePage page) {
        return FxTestSupport.findAll(page.getView(), RadioButton.class);
    }

    @Test
    void offersThreeAlgorithmsWithWeightedBigramSelected() throws Exception {
        List<RadioButton> radios = radios(newPage());

        assertEquals(List.of("Weighted bigram", "Most frequent", "Random follower"),
                radios.stream().map(RadioButton::getText).toList());
        assertEquals(List.of(true, false, false), radios.stream().map(RadioButton::isSelected).toList());
    }

    @Test
    void onlyOneAlgorithmCanBeSelectedAtATime() throws Exception {
        GeneratePage page = newPage();

        FxTestSupport.onFxThread(() -> radios(page).get(1).setSelected(true));

        assertEquals(List.of(false, true, false), radios(page).stream().map(RadioButton::isSelected).toList());
    }

    @Test
    void selectedChoiceBoxGetsTheSelectedStyleExactlyOnce() throws Exception {
        GeneratePage page = newPage();

        FxTestSupport.onFxThread(() -> {
            radios(page).get(2).setSelected(true);
            radios(page).get(1).setSelected(true);
        });

        List<VBox> choices = FxTestSupport.findAll(page.getView(), VBox.class,
                v -> v.getStyleClass().contains("choice"));
        assertEquals(3, choices.size());
        assertEquals(List.of(false, true, false),
                choices.stream().map(c -> c.getStyleClass().contains("selected")).toList());
    }

    @Test
    void clickingAnywhereOnAChoiceSelectsIt() throws Exception {
        GeneratePage page = newPage();
        VBox third = FxTestSupport.findAll(page.getView(), VBox.class,
                v -> v.getStyleClass().contains("choice")).get(2);

        FxTestSupport.onFxThread(() -> third.getOnMouseClicked().handle(new MouseEvent(
                MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, true, false, false, null)));

        assertTrue(radios(page).get(2).isSelected());
    }

    @Test
    void defaultsAreSetOnTheFields() throws Exception {
        GeneratePage page = newPage();

        List<String> fields = FxTestSupport.findAll(page.getView(), TextField.class).stream()
                .map(TextField::getText).toList();
        assertEquals(List.of("elizabeth", "15"), fields);
        CheckBox stop = FxTestSupport.find(page.getView(), CheckBox.class, c -> true);
        assertTrue(stop.isSelected());
    }
}
