package controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.Automaton;
import model.CommandHistory;
import model.InvalidRegexpException;
import view.GraphCanvasEvent;
import view.GraphCanvasFX;

public class RegexpVisApp implements Observer {

    /* Finals */
    final CheckMenuItem[] activityMenuItems;
    private final Activity[] activities;
    private final Automaton automaton;
    protected final GraphCanvasFX mCanvas;
    final ListView<String> historyList;
    final Stage stage;

    /* Constants */
    private static final String CONTROL_PANEL_HIDE_TEXT = "Hide Control Panel";
    private static final String CONTROL_PANEL_SHOW_TEXT = "Show Control Panel";
    private static final String HISTORY_LIST_HIDE_TEXT = "Hide History List";
    private static final String HISTORY_LIST_SHOW_TEXT = "Show History List";
    private static final String TEXTFIELD_PROMPT = "Type a regular expression and press Enter.";
    private static final int BUTTON_PANEL_PADDING_PX = 10;
    private static final int CONTROL_PANEL_PADDING_HORIZONTAL_PX = 35;
    private static final int CONTROL_PANEL_PADDING_VERTICAL_PX = 20;
    private static final int HISTORY_LIST_WIDTH_PX = 140;
    protected static final String ABOUT_HEADER = "Regular Expression Visualiser"
            + " (v" + Main.VERSION + ")";
    protected static final String ABOUT_CONTENT = "Authors:\n\n"
            + "Matthew Nicholls\t<mjn33@kent.ac.uk>\n"
            + "Parham Ghassemi\t<pg272@kent.ac.uk>\n"
            + "Samuel Pengelly\t<sp611@kent.ac.uk>\n"
            + "William Dix\t\t<wrd2@kent.ac.uk>\n";
    public static final String WINDOW_TITLE = Main.TITLE + " v" + Main.VERSION;

    /* Variables */
    protected Activity currentActivity;
    protected boolean enterKeyDown;

    public RegexpVisApp(Stage stage) {
        final VBox root = new VBox();
        final HBox canvasContainer = new HBox();
        this.historyList = new ListView<>();
        final VBox controlPanel = new VBox();
        this.stage = stage;

        Scene scene = new Scene(root, 800, 600);

        MenuBar menuBar = new MenuBar();

        // --- Menu File
        Menu menuFile = new Menu("File");
        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                exitApplication();
            }
        });

        MenuItem menuFileImport = new MenuItem("Import Graph...");
        menuFileImport.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                onImportGraph(event);
            }

        });
        MenuItem menuFileExport = new MenuItem("Export Graph...");
        menuFileExport.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                onExportGraph(event);
            }

        });
        menuFile.getItems().addAll(menuFileImport, menuFileExport, exit);

        // --- Menu Edit
        Menu menuEdit = new Menu("Edit");
        MenuItem menuEditUndo = new MenuItem("Undo");
        menuEditUndo.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                RegexpVisApp.this.currentActivity.history.prev();
            }
        });
        MenuItem menuEditRedo = new MenuItem("Redo");
        menuEditRedo.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                RegexpVisApp.this.currentActivity.history.next();
            }
        });
        final CheckMenuItem menuEditClobberHistory = new CheckMenuItem(
                "Clobber History");
        menuEditClobberHistory.setSelected(CommandHistory.CLOBBER_BY_DEFAULT);
        menuEditClobberHistory.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                boolean clob = RegexpVisApp.this.currentActivity.history
                        .isClobbered();
                RegexpVisApp.this.currentActivity.history.setClobbered(!clob);
                menuEditClobberHistory.setSelected(!clob);
            }
        });
        MenuItem menuEditPreferences = new MenuItem("Preferences...");
        menuEdit.getItems().addAll(menuEditUndo, menuEditRedo,
                menuEditClobberHistory, menuEditPreferences);

        // --- Menu Activity
        Menu menuActivity = new Menu("Activity");
        Activity.ActivityType[] actTypes = Activity.ActivityType.values();
        this.activityMenuItems = new CheckMenuItem[actTypes.length];
        for (int i = 0; i < actTypes.length; i++) {
            final CheckMenuItem item = new CheckMenuItem(actTypes[i].getText());
            this.activityMenuItems[i] = item;
            final Activity.ActivityType act = actTypes[i];
            item.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    setActivity(act);
                }
            });
            menuActivity.getItems().add(item);
        }

        // --- Menu View
        Menu menuView = new Menu("View");
        final MenuItem menuViewHistory = new MenuItem(HISTORY_LIST_HIDE_TEXT);
        menuViewHistory.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                RegexpVisApp.this.historyList
                        .setVisible(!RegexpVisApp.this.historyList.isVisible());
                RegexpVisApp.this.historyList
                        .setManaged(RegexpVisApp.this.historyList.isVisible());
                menuViewHistory
                        .setText(RegexpVisApp.this.historyList.isVisible()
                                ? HISTORY_LIST_HIDE_TEXT
                                : HISTORY_LIST_SHOW_TEXT);
            }
        });
        final MenuItem menuViewControlPanel = new MenuItem(
                CONTROL_PANEL_HIDE_TEXT);
        menuViewControlPanel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                controlPanel.setVisible(!controlPanel.isVisible());
                controlPanel.setManaged(controlPanel.isVisible());
                menuViewControlPanel.setText(controlPanel.isVisible()
                        ? CONTROL_PANEL_HIDE_TEXT : CONTROL_PANEL_SHOW_TEXT);
            }
        });
        menuView.getItems().addAll(menuViewHistory, menuViewControlPanel);

        // --- Menu About
        Menu menuHelp = new Menu("Help");
        MenuItem about = new MenuItem("About");
        about.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Alert alert = new Alert(AlertType.INFORMATION);
                alert.setTitle("About");
                alert.setHeaderText(ABOUT_HEADER);
                alert.setContentText(ABOUT_CONTENT);
                alert.showAndWait();
            }
        });
        menuHelp.getItems().addAll(about);

        menuBar.getMenus().addAll(menuFile, menuEdit, menuActivity, menuView,
                menuHelp);
        root.getChildren().addAll(menuBar);

        // Graph canvas
        this.mCanvas = new GraphCanvasFX();
        VBox.setVgrow(canvasContainer, javafx.scene.layout.Priority.ALWAYS);
        VBox.setVgrow(this.mCanvas, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(canvasContainer, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(this.mCanvas, javafx.scene.layout.Priority.ALWAYS);
        canvasContainer.getChildren().add(this.mCanvas);

        // History list also part of the canvas container
        this.historyList.setMinWidth(HISTORY_LIST_WIDTH_PX);
        this.historyList.setMaxWidth(HISTORY_LIST_WIDTH_PX);
        this.historyList.getSelectionModel().selectedIndexProperty()
                .addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(
                            ObservableValue<? extends Number> observable,
                            Number oldValue, Number newValue) {
                        RegexpVisApp.this.setHistoryIdx(newValue);
                    }
                });
        canvasContainer.getChildren().add(this.historyList);

        root.getChildren().add(canvasContainer);
        this.mCanvas.requestFocus(); // Pulls focus away from the text field

        HBox buttonPanel = new HBox();
        buttonPanel.setMinSize(0, BUTTON_PANEL_PADDING_PX * 2);
        buttonPanel.setPadding(new Insets(BUTTON_PANEL_PADDING_PX));
        buttonPanel.setAlignment(Pos.CENTER);
        Button buttonBackToStart = new Button("|<<");
        buttonBackToStart.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
                RegexpVisApp.this.currentActivity.historyStart();
            }
        });
        Button buttonBack = new Button("<--");
        buttonBack.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
                RegexpVisApp.this.currentActivity.historyPrev();
            }
        });
        Button buttonLoad = new Button("Load");
        Button buttonSave = new Button("Save");
        Button buttonForward = new Button("-->");
        buttonForward.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
                // TODO: Breakdown next edge if at end of history?
                RegexpVisApp.this.currentActivity.historyNext();
            }
        });
        Button buttonForwardToEnd = new Button(">>|");
        buttonForwardToEnd.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
                // TODO: Breakdown all edges if at end of history?
                RegexpVisApp.this.currentActivity.historyEnd();
            }
        });
        buttonPanel.getChildren().addAll(buttonBackToStart, buttonBack,
                buttonLoad, buttonSave, buttonForward, buttonForwardToEnd);
        controlPanel.getChildren().add(buttonPanel);

        final HBox inputPanel = new HBox();
        final TextField textField = new TextField(TEXTFIELD_PROMPT);
        textField.setPadding(new Insets(5));
        HBox.setHgrow(textField, Priority.ALWAYS);
        Button buttonEnter = new Button("Enter");
        inputPanel.getChildren().addAll(textField, buttonEnter);
        controlPanel.setPadding(new Insets(CONTROL_PANEL_PADDING_VERTICAL_PX,
                CONTROL_PANEL_PADDING_HORIZONTAL_PX,
                CONTROL_PANEL_PADDING_VERTICAL_PX,
                CONTROL_PANEL_PADDING_HORIZONTAL_PX));
        controlPanel.getChildren().add(inputPanel);
        root.getChildren().add(controlPanel);

        // Textfield focus listener
        // https://stackoverflow.com/questions/16549296/how-perform-task-on-javafx-textfield-at-onfocus-and-outfocus
        textField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
                    Boolean oldValue, Boolean newValue) {
                if (newValue.booleanValue()) {
                    if (textField.getText().equals(TEXTFIELD_PROMPT)) {
                        // Clear text field on click if the prompt is displayed
                        textField.clear();
                        textField.requestFocus();
                    }
                } else {
                    if (textField.getText().isEmpty()) {
                        // Paste prompt on defocus if text box is empty
                        textField.setText(TEXTFIELD_PROMPT);
                    }
                }
            }
        });
        /*
         * onKeyReleased rather than onKeyPressed, to prevent double-hits. This
         * however may be perceived as undesirable behaviour by the user.
         */
        textField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER
                        && !RegexpVisApp.this.enterKeyDown) {
                    RegexpVisApp.this.enterKeyDown = true;
                    String input = textField.getText().trim();
                    if (!input.isEmpty()) {
                        onEnteredRegexp(input);
                    }
                }
            }
        });
        textField.setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {
                    RegexpVisApp.this.enterKeyDown = false;
                }
            }
        });
        buttonEnter.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String input = textField.getText().trim();
                if (!input.isEmpty()) {
                    onEnteredRegexp(input);
                }
            }
        });
        this.mCanvas.setOnNodeClicked(new EventHandler<GraphCanvasEvent>() {
            @Override
            public void handle(GraphCanvasEvent event) {
                onNodeClicked(event);
            }
        });
        this.mCanvas.setOnEdgeClicked(new EventHandler<GraphCanvasEvent>() {
            @Override
            public void handle(GraphCanvasEvent event) {
                onEdgeClicked(event);
            }
        });
        this.mCanvas.setOnBackgroundClicked(new EventHandler<GraphCanvasEvent>() {
            @Override
            public void handle(GraphCanvasEvent event) {
                onBackgroundClicked(event);
            }
        });

        this.mCanvas.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED,
        new EventHandler<ContextMenuEvent>() {
            @Override
            public void handle(ContextMenuEvent event) {
                onContextMenuRequested(event);
            }
        });

        this.mCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED,
        new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                onHideContextMenu(event);
            }
        });

        /* Init fields */
        this.automaton = new Automaton();
        this.activities = new Activity[Activity.ActivityType.values().length];
        /* Using ordinals of enum to prevent misordering */
        this.activities[Activity.ActivityType.ACTIVITY_REGEXP_BREAKDOWN
                .ordinal()] = new RegexpBreakdownActivity(this.mCanvas,
                        this.automaton);
        this.activities[Activity.ActivityType.ACTIVITY_NFA_TO_DFA
                .ordinal()] = new NfaToDfaActivity(this.mCanvas,
                        this.automaton);
        this.activities[Activity.ActivityType.ACTIVITY_NFA_TO_REGEXP
                .ordinal()] = new NfaToRegexpActivity(this.mCanvas,
                        this.automaton);
        setActivity(Activity.ActivityType.ACTIVITY_REGEXP_BREAKDOWN);

        stage.setTitle(WINDOW_TITLE);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Called by the {@link ListView} component to perform a
     * {@link CommandHistory} seek when a list item is selected.
     *
     * @param idx
     *            the historyIdx to seek to
     */
    protected void setHistoryIdx(Number value) {
        int idx = (int) value;
        // Note: called with value -1 when the history is reset.
        if (idx >= 0) {
            this.currentActivity.history.seekIdx(idx);
            this.historyList.getSelectionModel().select(idx);
        }
    }

    protected static void exitApplication() {
        // TODO Confirm exit (unsaved changes, etc.)
        System.out.println("Exiting application...");
        System.exit(0);
    }

    protected void setActivity(Activity.ActivityType actType) {
        if (actType == null) {
            throw new IllegalArgumentException();
        }

        /* Observe only the current CommandHistory */
        if (this.currentActivity != null) {
            this.currentActivity.history.deleteObserver(this);
        }
        this.currentActivity = this.activities[actType.ordinal()];
        this.currentActivity.history.addObserver(this);

        /* Update history list */
        this.historyList.getItems().clear();
        for (int i = 0; i <= this.currentActivity.history
                .getHistorySize(); i++) {
            // 1 extra, so that "Step 0" is the inital state
            this.historyList.getItems().add("Step " + i);
            this.historyList.getSelectionModel().select(i);
        }

        /*
         * Set "checked" status of all menu items to reflect the change of
         * activity
         */
        for (int i = 0; i < this.activityMenuItems.length; i++) {
            this.activityMenuItems[i].setSelected(i == actType.ordinal());
        }
    }

    private void onNodeClicked(GraphCanvasEvent event) {
        if (this.currentActivity != null) {
            this.currentActivity.onNodeClicked(event);
        }
    }

    private void onEdgeClicked(GraphCanvasEvent event) {
        if (this.currentActivity != null) {
            this.currentActivity.onEdgeClicked(event);
        }
    }

    private void onBackgroundClicked(GraphCanvasEvent event) {
        if (this.currentActivity != null) {
            this.currentActivity.onBackgroundClicked(event);
        }
    }

    private void onContextMenuRequested(ContextMenuEvent event) {
        if (this.currentActivity != null) {
            this.currentActivity.onContextMenuRequested(event);
        }
    }

    private void onHideContextMenu(MouseEvent event) {
        if (this.currentActivity != null) {
            this.currentActivity.onHideContextMenu(event);
        }
    }

    private void onImportGraph(ActionEvent event) {
        // Based on the nice example snippet in the JavaFX documentation
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Automaton Graph File");
        // Choose .txt, our file format is text based as this just makes it
        // easier to edit with text editors.
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Automaton Graph Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File selectedFile = fileChooser.showOpenDialog(this.stage);
        if (selectedFile == null) {
            // No file selected, exit early
            return;
        }

        // TODO: implement ways for the activities to respond to an import,
        // and possibly abort the attempt without any loss of data.
        try {
            GraphExportFile f = new GraphExportFile(selectedFile);
            f.loadFile(automaton, mCanvas);

            // TODO: part of this idea here
            //if (this.currentActivity != null) {
            //    this.currentActivity.onGraphFileImport();
            //}
        } catch (BadGraphExportFileException e) {
            new Alert(AlertType.ERROR,
                    "Failed to load file, file isn't a valid automaton graph file.")
                    .showAndWait();
        } catch (FileNotFoundException e) {
            new Alert(AlertType.ERROR,
                    "Failed to load file, could not find file: "
                            + selectedFile.getPath()).showAndWait();
        } catch (IOException e) {
            new Alert(AlertType.ERROR,
                    "Failed to load file, unexpected I/O error.")
                    .showAndWait();
        } catch (InvalidRegexpException e) {
            new Alert(AlertType.ERROR,
                    "Failed to load file, file contains an invalid regexp.")
                    .showAndWait();
        }
    }



    private void onExportGraph(ActionEvent event) {
        // Based on the nice example snippet in the JavaFX documentation
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Automaton Graph File");
        // Choose .txt, our file format is text based as this just makes it
        // easier to edit with text editors.
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Automaton Graph Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));

        File selectedFile = fileChooser.showSaveDialog(this.stage);
        if (selectedFile == null) {
            // No file selected, exit early
            return;
        }

        try {
            GraphExportFile f = new GraphExportFile(automaton, mCanvas);
            f.writeFile(selectedFile);
        } catch (IOException e) {
            new Alert(AlertType.ERROR,
                    "Failed to save file, unexpected I/O error.")
                    .showAndWait();
        }
    }

    private void onEnteredRegexp(String text) {
        if (this.currentActivity != null) {
            this.historyList.getItems().clear();
            this.historyList.getItems().add("Step 0");
            this.historyList.getSelectionModel().select(0);
            this.currentActivity.history.clear();
            this.currentActivity.onEnteredRegexp(text);
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        /* Called whenever CommandHistory of current Activity changes */
        if (arg instanceof Integer) {
            int idx = (int) arg;
            ObservableList<String> items = this.historyList.getItems();
            if (idx == CommandHistory.HISTORY_CLOBBERED) {
                /*
                 * The last element of the history list was removed, we must
                 * reflect this change in the UI.
                 */
                items.remove(items.size() - 1);
            } else {
                if (items.size() <= idx) {
                    items.add("Step " + idx);
                }
                this.historyList.getSelectionModel().select(idx);
            }
        }
    }

}
