package ui;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BatchPageTest {

    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.startToolkit();
    }

    private static BatchPage newPage() throws Exception {
        return FxTestSupport.onFxThread(BatchPage::new);
    }

    private static CheckBox selectAll(BatchPage page) {
        return FxTestSupport.find(page.getView(), CheckBox.class, c -> c.getStyleClass().contains("select-all"));
    }

    /** The per-sentence checkboxes (everything except Select all and the options). */
    private static List<CheckBox> rowChecks(BatchPage page) {
        return FxTestSupport.findAll(page.getView(), CheckBox.class, c -> "".equals(c.getText()));
    }

    private static Label summary(BatchPage page) {
        return FxTestSupport.find(page.getView(), Label.class, l -> l.getStyleClass().contains("muted")
                && l.getText() != null && l.getText().contains("selected"));
    }

    private static Button addButton(BatchPage page) {
        return FxTestSupport.find(page.getView(), Button.class, b -> b.getStyleClass().contains("primary")
                && b.getText().startsWith("Add"));
    }

    @Test
    void startsWithNewSentencesTickedAndDuplicateUnticked() throws Exception {
        BatchPage page = newPage();

        assertEquals(10, rowChecks(page).size());
        assertEquals(9, rowChecks(page).stream().filter(CheckBox::isSelected).count());
        assertEquals("9 of 10 selected · 1 already exists", summary(page).getText());
        assertEquals("Add 9 sentences to corpus", addButton(page).getText());
        assertTrue(selectAll(page).isSelected());
    }

    @Test
    void duplicateRowIsMarkedWithAPill() throws Exception {
        BatchPage page = newPage();

        List<Label> pills = FxTestSupport.findAll(page.getView(), Label.class,
                l -> "Duplicate".equals(l.getText()));
        assertEquals(1, pills.size());
        assertTrue(pills.get(0).getStyleClass().contains("orange"));
    }

    @Test
    void untickingARowUpdatesSummaryAndClearsSelectAll() throws Exception {
        BatchPage page = newPage();

        FxTestSupport.onFxThread(() -> rowChecks(page).get(0).setSelected(false));

        assertEquals("8 of 10 selected · 1 already exists", summary(page).getText());
        assertEquals("Add 8 sentences to corpus", addButton(page).getText());
        assertFalse(selectAll(page).isSelected());
    }

    @Test
    void selectAllOffUntickAllNewRowsAndDisablesAdd() throws Exception {
        BatchPage page = newPage();

        FxTestSupport.onFxThread(() -> selectAll(page).setSelected(false));

        assertEquals(0, rowChecks(page).stream().filter(CheckBox::isSelected).count());
        assertEquals("0 of 10 selected · 1 already exists", summary(page).getText());
        assertEquals("Add 0 sentences to corpus", addButton(page).getText());
        assertTrue(addButton(page).isDisabled());
    }

    @Test
    void selectAllOnTicksNewRowsButNotTheDuplicate() throws Exception {
        BatchPage page = newPage();
        FxTestSupport.onFxThread(() -> selectAll(page).setSelected(false));

        FxTestSupport.onFxThread(() -> selectAll(page).setSelected(true));

        assertEquals(9, rowChecks(page).stream().filter(CheckBox::isSelected).count());
        assertFalse(addButton(page).isDisabled());
    }

    @Test
    void addButtonUsesSingularForOneSentence() throws Exception {
        BatchPage page = newPage();

        FxTestSupport.onFxThread(() -> {
            selectAll(page).setSelected(false);
            rowChecks(page).get(0).setSelected(true);
        });

        assertEquals("Add 1 sentence to corpus", addButton(page).getText());
        assertEquals("1 of 10 selected · 1 already exists", summary(page).getText());
    }

    @Test
    void tickingTheDuplicateCountsItButKeepsSelectAllTicked() throws Exception {
        BatchPage page = newPage();
        CheckBox duplicate = rowChecks(page).stream().filter(c -> !c.isSelected()).findFirst().orElseThrow();

        FxTestSupport.onFxThread(() -> duplicate.setSelected(true));

        assertEquals("10 of 10 selected · 1 already exists", summary(page).getText());
        assertTrue(selectAll(page).isSelected());
    }
}
