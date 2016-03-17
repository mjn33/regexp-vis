package controller;

import java.net.URL;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Based on the example at
 * https://docs.oracle.com/javafx/2/webview/jfxpub-webview.htm
 *
 * @author sp611
 *
 */
public class UserGuideView extends Region {

    public static final double WIDTH = 400;
    public static final double HEIGHT = 600;

    private final WebView browser;
    private final WebEngine engine;

    public UserGuideView() {
        super();
        this.browser = new WebView();
        this.browser.setPrefWidth(WIDTH);
        this.engine = this.browser.getEngine();
        URL url = this.getClass().getResource("/userguide/index.html");
        this.engine.load(url.toExternalForm());
        // Make sure the WebView itself grows
        HBox.setHgrow(this.browser, Priority.ALWAYS);

        // Put the WebView into a HBox which automatically resizes
        HBox hbox = new HBox();
        hbox.getChildren().add(this.browser);
        hbox.prefWidthProperty().bind(widthProperty());
        hbox.prefHeightProperty().bind(heightProperty());
        getChildren().add(hbox);
    }

}
