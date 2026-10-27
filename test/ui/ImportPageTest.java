package ui;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImportPageTest {

    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.startToolkit();
    }

    private static ImportPage newPage() throws Exception {
        return FxTestSupport.onFxThread(ImportPage::new);
    }

    @Test
    void optionsDefaultToStripHeaderAndLowerCaseOnly() throws Exception {
        List<CheckBox> options = FxTestSupport.findAll(newPage().getView(), CheckBox.class);

        assertEquals(3, options.size());
        assertEquals(List.of(true, true, false), options.stream().map(CheckBox::isSelected).toList());
    }

    @Test
    void historyTableHasSixColumnsAndFourRows() throws Exception {
        TableView<?> table = FxTestSupport.find(newPage().getView(), TableView.class, t -> true);

        assertEquals(List.of("File", "Words", "Unique words", "Imported", "Time taken", "Status"),
                table.getColumns().stream().map(c -> c.getText()).toList());
        assertEquals(4, table.getItems().size());
    }

    @Test
    void historyHeaderCountMatchesRows() throws Exception {
        ImportPage page = newPage();
        TableView<?> table = FxTestSupport.find(page.getView(), TableView.class, t -> true);

        assertTrue(FxTestSupport.findAll(page.getView(), Label.class,
                l -> (table.getItems().size() + " files").equals(l.getText())).size() == 1);
    }

    @Test
    void progressBarShowsSampleProgress() throws Exception {
        ProgressBar bar = FxTestSupport.find(newPage().getView(), ProgressBar.class, b -> true);

        assertEquals(0.62, bar.getProgress(), 1e-9);
    }

    @Test
    void dropZoneNamesTheSupportedFormat() throws Exception {
        ImportPage page = newPage();

        assertEquals(1, FxTestSupport.findAll(page.getView(), Label.class,
                l -> "Drag .txt files here".equals(l.getText())).size());
        assertEquals(1, FxTestSupport.findAll(page.getView(), Label.class,
                l -> l.getText() != null && l.getText().contains("PDF and EPUB are not supported")).size());
    }
}
