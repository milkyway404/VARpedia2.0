package main.java.controllers.createAudio;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import main.java.application.AlertFactory;
import main.java.application.StringManipulator;
import main.java.controllers.Controller;
import main.java.tasks.PreviewAudioTask;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Controller for displaying wikit search results
 *
 * @author wcho400
 */
public class ChooseText extends Controller {

    private final ArrayList<String> _voiceSample = new ArrayList<String>(Arrays.asList("Vanilla",
            "Chocolate", "Strawberry", "Banana", "Orange", "Apple"));
    private StringManipulator _manipulator = new StringManipulator();
    private String _term;
    private String _sourceString;
    //the actual name
    private ArrayList<String> _voices;
    //the displayed name
    private ArrayList<String> _voiceName;

    private PreviewAudioTask _previewTask;

    @FXML
    private Text _message;

    @FXML
    private TextArea _searchResults;

    @FXML
    private TextArea _chosenText;

    @FXML
    private Label _wordLimit;

    @FXML
    private ChoiceBox<String> _voiceSelection;

    /**
     * Initialise the searchResults TextArea and also the number of lines displayed to user.
     */
    public void setUp(String term, String searchResults) {
        _term = term;
        _sourceString = searchResults.trim();
        _wordLimit.setTextFill(Color.GREEN);

        _message.setText("TermSearch results for " + _term + ": ");
        _searchResults.setText(_sourceString);

        //generate list of different voices
        listOfVoices();
        _voiceSelection.setItems(FXCollections.observableArrayList(_voiceName));
        _voiceSelection.getSelectionModel().selectFirst();

    }

    /**
     * converts highlighted text from the search to be used in the audio
     */
    @FXML
    private void searchToChosen() {
        String highlightedText = _searchResults.getSelectedText();

        if (highlightedText.trim().isEmpty()) {
            new AlertFactory(AlertType.ERROR, "Error", "No valid text selected", "Please select " +
                    "some text.");
        } else {
            _chosenText.appendText(highlightedText);
            updateCount();
        }
    }

    /**
     * passes choosen text to be made into an audio
     * Checks if the text is valid for creation
     */
    @FXML
    private void create() {

        // kill the current task if there is one
        if (_previewTask != null) {
            _previewTask.cancel();
        }

        if (!_chosenText.getText().equals("") &&
                (_manipulator.countWords(_chosenText.getText()) < 41)) {
            //creates the audio
            //need to change to have voice type input

            if (_manipulator.countWords(_chosenText.getText()) < 6) {
                new AlertFactory(AlertType.ERROR, "Warning", "Short Audio Creation",
                        "A creation of this audio is too short, please make it longer");

                return;

            } else {
                int index = _voiceSelection.getSelectionModel().getSelectedIndex();
                _mainApp.displayCreateAudioNamingScene(_term, _chosenText.getText(),
                        _voices.get(index));
            }
        } else if (_chosenText.getText().equals("")) {
            new AlertFactory(AlertType.ERROR, "Error", "No text selected", "Please select some " +
                    "text.");
            return;
        } else {
            new AlertFactory(AlertType.ERROR, "Error", "Too much text selected", "Please remove " +
                    "some text.");
            return;
        }


        //creates the audio
        //need to change to have voice type input
        int index = _voiceSelection.getSelectionModel().getSelectedIndex();
        // remove all special characters
        String chosenText = _chosenText.getText().replaceAll("[^0-9 a-z\\.A-Z]", "");

        // clear the chosen text so if user comes back...
        _chosenText.clear();

        _mainApp.displayCreateAudioNamingScene(_term, chosenText, _voices.get(index));

    }

    /**
     * reset any edits of the wikit search results
     */
    @FXML
    private void reset() {
        _searchResults.setText(_sourceString);
    }

    /**
     * plays the current chosen text using the chosen voice
     */
    @FXML
    private void preview() {

        // kill the current task if there is one
        if (_previewTask != null) {
            _previewTask.cancel();
        }

        String chosenText = _chosenText.getText().trim().replaceAll("[^0-9 a-z\\.A-Z]", "");

        // Error handling
        if (chosenText.isEmpty()) {
            new AlertFactory(AlertType.ERROR, "Error", "No text chosen", "Please choose/enter " +
                    "some text.");
            return;
        }

        int wordNumber = _manipulator.countWords(chosenText.trim());

        if (wordNumber > 40) {
            new AlertFactory(AlertType.ERROR, "Error", "Too much text", "Exceeded maximum of 40 " +
                    "words.");
            return;
        }


        int index = _voiceSelection.getSelectionModel().getSelectedIndex();
        _previewTask = new PreviewAudioTask(chosenText, _voices.get(index));
        new Thread(_previewTask).start();


    }

    /**
     * choice box of possible voices
     *
     * @return an ArrayList of voices
     */
    private void listOfVoices() {
        _voices = new ArrayList<String>();
        _voiceName = new ArrayList<String>();

        Process source;
        try {
            source = new ProcessBuilder("bash", "-c", "echo \"(voice.list)\" | festival -i"
                    + " | grep \"festival> (\" | cut -d \"(\" -f2 | cut -d \")\" -f1").start();
            InputStream stdout = source.getInputStream();
            StringManipulator manipulator = new StringManipulator();
            String lineOfVoices = manipulator.inputStreamToString(stdout);

            if (manipulator.countWords(lineOfVoices) > 0) {
                String[] voicesOrder = lineOfVoices.split(" ");

                for (int i = 0; i < voicesOrder.length; i++) {
                    _voices.add(voicesOrder[i]);
                    // add names of voices
                    _voiceName.add(_voiceSample.get(i));
                }

            } else {
                //_voices.add(_voiceSample.get(0));
                //_voices.add(lineOfVoices);
                new AlertFactory(AlertType.ERROR, "Error", "No available voices", "Please install" +
                        " some festival voices");
            }


        } catch (IOException e) {
            new AlertFactory(AlertType.ERROR, "Error", "Cannot preview", "Could not preview with " +
                    "this voice.");
        }

    }

    /**
     * reset chosen text to empty
     */
    @FXML
    private void selectReset() {
        _chosenText.setText("");
        updateCount();
    }

    /**
     * displays the amount of words in the chosen text section
     * Indicates when the limit of words has been reached
     */
    private void updateCount() {
        String content = _chosenText.getText().trim();
        int wordNumber = _manipulator.countWords(content);
        String wordCount = String.valueOf(wordNumber);

        if (content.equals("")) {
            _wordLimit.setTextFill(Color.DEEPPINK);
            _wordLimit.setText("Nothing here yet!");
        } else if (wordNumber == 40) {
            _wordLimit.setTextFill(Color.DARKRED);
            _wordLimit.setText("At the limit!");
        } else if (wordNumber > 40) {
            _wordLimit.setTextFill(Color.RED);
            _wordLimit.setText("Over the limit :( (" + wordCount + ")");
        } else if (wordNumber > 30) {
            _wordLimit.setTextFill(Color.ORANGE);
            _wordLimit.setText("Near the limit (" + wordCount + ")");
        } else if (wordNumber < 4) {
            _wordLimit.setTextFill(Color.RED);
            _wordLimit.setText("A bit short! (" + wordCount + ")");
        } else {
            _wordLimit.setTextFill(Color.GREEN);
            _wordLimit.setText("You're Good! (" + wordCount + ")");
        }


    }

    /**
     * method to check the number of words in the choosen text
     */
    @FXML
    private void editCount() {
        if (_chosenText.getText().equals(" ")) {
            _chosenText.setText("");
        }
        updateCount();
    }


}