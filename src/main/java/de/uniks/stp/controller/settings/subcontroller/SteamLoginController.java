package de.uniks.stp.controller.settings.subcontroller;

import de.uniks.stp.StageManager;
import de.uniks.stp.builder.ModelBuilder;
import de.uniks.stp.controller.titlebar.TitleBarController;
import de.uniks.stp.util.ResizeHelper;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import kong.unirest.JsonNode;

import java.io.IOException;
import java.util.Objects;

public class SteamLoginController {
    private final ModelBuilder builder;
    private WebView webView;
    private Stage loginStage;
    private Runnable refreshConnectionView;
    private Parent steamLoginView;
    private TitleBarController titleBarController;

    public SteamLoginController(ModelBuilder builder) {
        this.builder = builder;
        builder.setSteamLoginController(this);
    }

    public void init() {
        try {
            steamLoginView = FXMLLoader.load(Objects.requireNonNull(StageManager.class.getResource("controller/LoginWebView.fxml")), builder.getStageManager().getLangBundle());
        } catch (IOException e) {
            e.printStackTrace();
        }

        loginStage = new Stage();
        loginStage.initStyle(StageStyle.TRANSPARENT);
        loginStage.getIcons().add(new javafx.scene.image.Image(Objects.requireNonNull(StageManager.class.getResourceAsStream("icons/AccordIcon.png"))));
        Scene scene = new Scene(Objects.requireNonNull(steamLoginView));

        webView = (WebView) steamLoginView.lookup("#loginWebView");

        // create titleBar
        HBox titleBarBox = (HBox) steamLoginView.lookup("#titleBarBox");
        Parent titleBarView = null;
        try {
            titleBarView = FXMLLoader.load(Objects.requireNonNull(StageManager.class.getResource("controller/titlebar/TitleBarView.fxml")), builder.getStageManager().getLangBundle());
        } catch (IOException e) {
            e.printStackTrace();
        }
        titleBarBox.getChildren().add(titleBarView);
        titleBarController = new TitleBarController(loginStage, titleBarView, builder);
        titleBarController.init();
        titleBarController.setTheme();
        titleBarController.setMaximizable(true);
        titleBarController.setTitle("Steam Login");
        loginStage.setTitle("Steam Login");

        webView.prefHeightProperty().bind(loginStage.heightProperty());
        webView.prefWidthProperty().bind(loginStage.widthProperty());

        java.net.CookieHandler.setDefault(new java.net.CookieManager());
        webView.getEngine().load("https://steamcommunity.com/login/home/?goto=");
        webView.getEngine().locationProperty().addListener(this::getSteam64ID);

        loginStage.setScene(scene);
        loginStage.setResizable(true);
        loginStage.setMinWidth(660);
        loginStage.setMinHeight(710);
        loginStage.show();
        ResizeHelper.addResizeListener(loginStage);
    }

    private void getSteam64ID(Observable observable) {
        String[] link = webView.getEngine().getLocation().split("/");
        if (link.length > 1 && !link[link.length - 1].equals("goto")) {
            String selector = link[link.length - 2];
            if (selector.equals("id")) {   // https://steamcommunity.com/id/VanityID/
                resolveVanityUrl(link);
            } else if (selector.equals("profiles")) { // https://steamcommunity.com/profiles/steam64ID/
                Platform.runLater(() -> setSteam64ID(link[link.length - 1]));
            }
        }
    }

    private void resolveVanityUrl(String[] link) {
        builder.getRestClient().resolveVanityID(link[link.length - 1], response -> {
            JsonNode body = response.getBody();
            int status = body.getObject().getJSONObject("response").getInt("success");
            if (status == 1) {
                setSteam64ID(body.getObject().getJSONObject("response").getString("steamid"));
            }
        });
    }

    private void setSteam64ID(String steam64ID) {
        Platform.runLater(loginStage::close);
        builder.setSteamToken(steam64ID);
        builder.saveSettings();
        webView.getEngine().locationProperty().removeListener(this::getSteam64ID);
        webView = null;
        loginStage = null;
        Platform.runLater(() -> refreshConnectionView.run());
    }

    public void refresh(Runnable refresh) {
        refreshConnectionView = refresh;
    }

    public void setTheme() {
        if (builder.getTheme().equals("Bright")) {
            setWhiteMode();
        } else {
            setDarkMode();
        }
    }

    private void setWhiteMode() {
        if (steamLoginView != null) {
            steamLoginView.getStylesheets().clear();
            steamLoginView.getStylesheets().add(Objects.requireNonNull(StageManager.class.getResource("styles/themes/bright/LoginWebView.css")).toExternalForm());
        }
        if (titleBarController != null) {
            titleBarController.setTheme();
        }
    }

    private void setDarkMode() {
        if (steamLoginView != null) {
            steamLoginView.getStylesheets().clear();
            steamLoginView.getStylesheets().add(Objects.requireNonNull(StageManager.class.getResource("styles/themes/dark/LoginWebView.css")).toExternalForm());
        }
        if (titleBarController != null) {
            titleBarController.setTheme();
        }
    }
}
