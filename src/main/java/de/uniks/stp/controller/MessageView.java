package de.uniks.stp.controller;

import com.pavlobu.emojitextflow.EmojiTextFlow;
import com.pavlobu.emojitextflow.EmojiTextFlowParameters;
import de.uniks.stp.StageManager;
import de.uniks.stp.builder.ModelBuilder;
import de.uniks.stp.model.Message;
import de.uniks.stp.util.EmojiTextFlowExtended;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageView {
    private String urlType;
    private ModelBuilder builder;
    private ChatViewController chatViewController;
    private Runnable scroll;

    public void setBuilder(ModelBuilder builder) {
        this.builder = builder;
    }

    public void setChatViewController(ChatViewController chatViewController) {
        this.chatViewController = chatViewController;
        chatViewController.getContainer().boundsInLocalProperty().addListener(new ChangeListener<Bounds>() {
            @Override
            public void changed(ObservableValue<? extends Bounds> observableValue, Bounds bounds, Bounds t1) {
                if (bounds.getMaxY() != t1.getMaxX() || bounds.getMaxX() != t1.getMaxX()) {
                    for(WebView view : chatViewController.getWebEngines()) {
                        setSizeWebView(view, t1, 0, 0);
                    }
                }
            }
        });
    }

    public void updateItem(Message item) {
        boolean messageIsInfo = false;
        boolean loadImage;
        boolean loadVideo;
        StackPane cell = new StackPane();
        cell.setId("messageCell");
        //Background for the messages

        if (item.getMessage().endsWith("#arrival") || item.getMessage().endsWith("#exit")) {
            messageIsInfo = true;
        }

        VBox vbox = new VBox();
        Label userName = userNameLabel();

        //right alignment if User is currentUser else left
        String textMessage = item.getMessage();
        String url = searchUrl(textMessage);
        loadImage = false;
        loadVideo = false;
        WebView webView = new WebView();
        webView.setOnScroll(chatViewController.getMessageScrollPane().getContent().getOnScroll());
        MediaView mediaView = new MediaView();
        if (urlType.equals("video") || urlType.equals("localVideo")) {
            loadVideo = true;
            setVideo(url, mediaView);
            textMessage = textMessage.replace(url, "");
        } else if (!urlType.equals("None") && !urlType.equals("link")) {
            loadImage = true;
            setMedia(url, webView.getEngine());
            textMessage = textMessage.replace(url, "");
        }
        if (loadImage) {
            webView.setContextMenuEnabled(false);
            setImageSize(chatViewController.getMessageScrollPane(), url, webView);
            chatViewController.getWebEngines().add(webView);
        }

        EmojiTextFlowExtended message = setupMessage(messageIsInfo, vbox, userName, item);

        textMessage = parseTeamEncoding(textMessage);

        double lyw = 0.0f;
        if (!textMessage.equals("")) {
            message.setId("messageLabel");
            String str;
            if (messageIsInfo) {
                str = showSystemMessage(item);
            } else {
                str = textMessage;
            }
            if (urlType.equals("link")) {
                message.addTextLinkNode(str, url);
            } else {
                message.parseAndAppend(str);
            }
            lyw = getLayoutBoundsGetWidth(message) + 10;
        }

        HBox messageBox = new HBox();
        messageBox.getChildren().add(message);
        HBox finalMessageBox = new HBox();
        setSize(lyw, messageBox, finalMessageBox);

        setupMessageBackground(finalMessageBox, messageIsInfo, messageBox, item);

        setUpCell(loadImage, vbox, userName, webView, cell, loadVideo, mediaView, url, finalMessageBox);

        if (!messageIsInfo) {
            boolean messageIsLink = loadImage || loadVideo;
            cell.setOnMouseClicked(event -> chatViewController.chatClicked(event, messageIsLink));
        }
        chatViewController.getContainer().getChildren().add(cell);
        chatViewController.getMessagesHashMap().put(cell, item);
        chatViewController.getStackPaneHashMap().put(item, cell);
        if (scroll != null) {
            scroll.run();
        }
    }

    private void setUpCell(boolean loadImage, VBox vbox, Label userName, WebView webView, StackPane cell, boolean loadVideo, MediaView mediaView, String url, HBox finalMessageBox) {
        if (loadImage) {
            vbox.getChildren().addAll(userName, webView);
            cell.setMinSize(webView.getMaxWidth(), webView.getPrefHeight());
        } else if (loadVideo) {
            MediaControl mediaControl = new MediaControl();
            VBox mediaBox = mediaControl.setMediaControls(mediaView);
            setVideoSize(chatViewController.getMessageScrollPane(), url, mediaView);
            vbox.getChildren().addAll(userName, mediaBox);

        } else {
            vbox.getChildren().addAll(userName, finalMessageBox);
            vbox.setMouseTransparent(true);
        }

        cell.setAlignment(Pos.CENTER_RIGHT);
        cell.getChildren().addAll(vbox);
    }

    private void setSize(double lyw, HBox messageBox, HBox finalMessageBox) {
        if (lyw > 320) {
            messageBox.setMaxWidth(320);
        } else {
            messageBox.setMaxWidth(lyw);
        }
        if (lyw > 320) {
            finalMessageBox.setMaxWidth(320 + 10);
        } else {
            finalMessageBox.setMaxWidth(lyw + 10);
        }
    }

    private Label userNameLabel() {
        Label userName = new Label();
        userName.setId("userNameLabel");
        if (builder.getTheme().equals("Bright")) {
            userName.setTextFill(Color.BLACK);
        } else {
            userName.setTextFill(Color.WHITE);
        }
        return userName;
    }

    private EmojiTextFlowExtended setupMessage(boolean messageIsInfo, VBox vbox, Label userName, Message item) {
        Date date = new Date(item.getTimestamp());
        DateFormat formatterTime = new SimpleDateFormat("dd.MM - HH:mm");
        EmojiTextFlowExtended message;
        if (messageIsInfo) {
            vbox.setAlignment(Pos.CENTER_LEFT);
            userName.setText((formatterTime.format(date)));

            message = handleEmojis(this.builder, "system");
        } else if (builder.getPersonalUser().getName().equals(item.getFrom())) {
            vbox.setAlignment(Pos.CENTER_RIGHT);
            userName.setText((formatterTime.format(date)) + " " + item.getFrom());

            message = handleEmojis(this.builder, "self");
        } else {
            vbox.setAlignment(Pos.CENTER_LEFT);
            userName.setText(item.getFrom() + " " + (formatterTime.format(date)));

            message = handleEmojis(this.builder, "other");
        }
        return message;
    }

    private void setupMessageBackground(HBox finalMessageBox, boolean messageIsInfo, HBox messageBox, Message item) {
        Polygon polygon = new Polygon();
        if (messageIsInfo) {
            polygon.getStyleClass().add("messagePolygonSystem");
            messageBox.setId("messageBoxSystem");
            polygon.getPoints().addAll(0.0, 0.0,
                    10.0, 0.0,
                    10.0, 10.0);
            finalMessageBox.getChildren().addAll(polygon, messageBox);
        } else if (builder.getPersonalUser().getName().equals(item.getFrom())) {
            polygon.getStyleClass().add("messagePolygonCurrentUser");
            messageBox.setId("messageBoxCurrentUser");
            polygon.getPoints().addAll(0.0, 0.0,
                    10.0, 0.0,
                    0.0, 10.0);
            finalMessageBox.getChildren().addAll(messageBox, polygon);
        } else {
            polygon.getStyleClass().add("messagePolygonOther");
            messageBox.setId("messageBoxOtherUser");
            polygon.getPoints().addAll(0.0, 0.0,
                    10.0, 0.0,
                    10.0, 10.0);
            finalMessageBox.getChildren().addAll(polygon, messageBox);
        }
    }

    private String showSystemMessage(Message item) {
        ResourceBundle lang = builder.getStageManager().getLangBundle();
        if (item.getMessage().endsWith("#arrival")) {
            return ":white_check_mark: " + item.getFrom() + " " + lang.getString("message.user_arrived");
        } else if (item.getMessage().endsWith("#exit")) {
            return ":no_entry: " + item.getFrom() + " " + lang.getString("message.user_exited");
        }
        return null;
    }

    private String parseTeamEncoding(String textMessage) {
        Matcher matchRegexTeamCommands = Pattern.compile("^###.+###.+###.+###.+\\[###.+###.+]\\[###.+####.+]###.+###|%.+%|\\*.+\\*|<.+>|.+/.+/.+/.+/.+/.+|\\$.+\\$.+|!guess+.+|!choose +(scissor|paper|rock)|\\\\+(!|%|\\*).+$").matcher(textMessage);
        if (matchRegexTeamCommands.find()) {
            Matcher replyRegex = Pattern.compile("^###.+###.+###.+###.+\\[###.+###.+]\\[###.+####.+]###.+###$").matcher(textMessage);
            Matcher spoilerRegex = Pattern.compile("%([^ ]*?)%").matcher(textMessage);
            Matcher boldRegex = Pattern.compile("\\*([^ ]*?)\\*").matcher(textMessage);
            Matcher hideLinkRegex = Pattern.compile("<([^ ]*?)>").matcher(textMessage);
            Matcher guessRegex = Pattern.compile("^!guess+.+$").matcher(textMessage);
            Matcher choseRegex = Pattern.compile("^!choose +(scissor|paper|rock)$").matcher(textMessage);
            Matcher escapeRegex = Pattern.compile("\\\\(%|!|\\*)([^ ]*)").matcher(textMessage);

            if (replyRegex.find()) {
                String[] splitString = textMessage.split("###");
                return splitString[4].substring(0, splitString[4].length() - 1);
            }

            if (spoilerRegex.find()) {
                spoilerRegex.reset();
                List<String> matchList = new ArrayList<>();
                while (spoilerRegex.find()) {//Finds Matching Pattern in String
                    matchList.add(spoilerRegex.group(1));//Fetching Group from String
                }
                for (String word : matchList) {
                    textMessage = textMessage.replace("%" + word + "%", word);
                }
            }

            if (boldRegex.find()) {
                boldRegex.reset();
                List<String> matchList = new ArrayList<>();
                while (boldRegex.find()) {//Finds Matching Pattern in String
                    matchList.add(boldRegex.group(1));//Fetching Group from String
                }
                for (String word : matchList) {
                    textMessage = textMessage.replace("*" + word + "*", word);
                }
            }

            if (hideLinkRegex.find()) {
                hideLinkRegex.reset();
                List<String> matchList = new ArrayList<>();
                while (hideLinkRegex.find()) {//Finds Matching Pattern in String
                    matchList.add(hideLinkRegex.group(1));//Fetching Group from String
                }
                for (String word : matchList) {
                    textMessage = textMessage.replace("<" + word + ">", word);
                }
            }

            if (escapeRegex.find()) {
                escapeRegex.reset();
                List<String> matchList = new ArrayList<>();
                while (escapeRegex.find()) {//Finds Matching Pattern in String
                    matchList.add(escapeRegex.group(1));//Fetching Group from String
                }
                for (String word : matchList) {
                    textMessage = textMessage.replace("\\" + word, word);
                }
            }

            if (guessRegex.find()) {
                String[] splitString = textMessage.split("!guess ");
                return splitString[1];
            }

            if (choseRegex.find()) {
                String[] splitString = textMessage.split("!choose ");
                return splitString[1];
            }
        }
        return textMessage;
    }

    public EmojiTextFlowExtended handleEmojis(ModelBuilder builder, String type) {
        EmojiTextFlowParameters emojiTextFlowParameters;
        {
            emojiTextFlowParameters = new EmojiTextFlowParameters();
            emojiTextFlowParameters.setEmojiScaleFactor(1D);
            emojiTextFlowParameters.setTextAlignment(TextAlignment.LEFT);
            emojiTextFlowParameters.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
        }
        if (type.equals("system")) {
            emojiTextFlowParameters.setTextColor(Color.BLACK);
        } else if (type.equals("self")) {
            if (builder.getTheme().equals("Dark")) {
                emojiTextFlowParameters.setTextColor(Color.BLACK);
            } else {
                emojiTextFlowParameters.setTextColor(Color.WHITE);
            }
        } else {
            emojiTextFlowParameters.setTextColor(Color.BLACK);
        }
        return new EmojiTextFlowExtended(emojiTextFlowParameters);
    }

    /**
     * Sums the width of each node, Text and ImageView
     *
     * @param message the given message
     * @return the total width
     */
    private double getLayoutBoundsGetWidth(EmojiTextFlow message) {
        double width = 0.0;

        for (int x = 0; x < message.getChildren().size(); x++) {
            Node T = message.getChildren().get(x);
            width += T.getLayoutBounds().getWidth();
        }
        return width;
    }

    public String searchUrl(String msg) {
        String urlRegex = "\\b(https?|ftp|file|src)(://|/)[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(msg);
        String url = "";
        if (matcher.find()) {
            url = matcher.toMatchResult().group();
        }
        if (url.contains(".png") || url.contains(".jpg") || url.contains(".bmp") || url.contains(".svg")) {
            urlType = "picture";
        } else if (url.contains(".gif")) {
            urlType = "gif";
        } else if (url.contains("youtube") || url.contains("youtu.be")) {
            urlType = "youtube";
        } else if ((url.contains("src/") || url.contains("file://")) && url.contains(".mp4")) {
            urlType = "localVideo";
        } else if (url.contains(".mp4")) {
            urlType = "video";
        } else if (!url.equals("")) {
            urlType = "link";
        } else {
            urlType = "None";
        }
        return url;
    }

    private void setVideo(String url, MediaView mediaView) {
        Media mediaUrl;
        if (urlType.equals("localVideo")) {
            File file = new File(url);
            mediaUrl = new Media(file.toURI().toString());
        } else {
            mediaUrl = new Media(url);
        }
        MediaPlayer mp = new MediaPlayer(mediaUrl);
        chatViewController.getMediaPlayers().add(mp);
        mediaView.setMediaPlayer(mp);
    }

    private void setMedia(String url, WebEngine engine) {
        switch (urlType) {
            case "picture":
                engine.load(url);
                engine.setJavaScriptEnabled(false);
                break;
            case "gif":
                engine.loadContent("<html><body><img src=\"" + url + "\" class=\"center\"></body></html>");
                engine.setJavaScriptEnabled(false);
                break;
            case "youtube":
                String videoIdPatternRegex = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";
                Pattern videoIdPattern = Pattern.compile(videoIdPatternRegex);
                Matcher videoIdMatcher = videoIdPattern.matcher(url); //url is YouTube url for which you want to extract the id.
                String youtube_url = "";
                if (videoIdMatcher.find()) {
                    String videoId = videoIdMatcher.group();
                    youtube_url = "https://www.youtube.com/embed/" + videoId;
                }
                engine.load(youtube_url);
                engine.setJavaScriptEnabled(true);
                break;
        }
        if (builder.getTheme().equals("Bright")) {
            engine.setUserStyleSheetLocation(Objects.requireNonNull(getClass().getResource("/de/uniks/stp/styles/themes/bright/webView.css")).toExternalForm());
        } else {
            engine.setUserStyleSheetLocation(Objects.requireNonNull(getClass().getResource("/de/uniks/stp/styles/themes/dark/webView.css")).toExternalForm());
        }

    }

    private void setVideoSize(Parent parent, String url, MediaView mediaView) {
        try {
            Bounds bounds = parent.getParent().getParent().getParent().getBoundsInLocal();
            double maxX = bounds.getMaxX();
            double maxY = bounds.getMaxY();
            int height = 0;
            int width = 0;
            if (!urlType.equals("None")) {
                URL url_stream;
                if (url.contains("src/test")) {
                    File file = new File(url);
                    url_stream = new URL(file.toURI().toString());
                } else {
                    url_stream = new URL(url);
                }
                BufferedImage image = ImageIO.read(url_stream.openStream());
                if (image != null) {
                    height = image.getHeight();
                    width = image.getWidth();
                }
            }
            if (height != 0 && width != 0 && (height < maxY - 50 || width < maxX - 50)) {

                mediaView.setFitHeight(height);
                mediaView.setFitWidth(width);
            } else {
                mediaView.setFitHeight(maxY - 50);
                mediaView.setFitWidth(maxX - 50);
            }
            mediaView.autosize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setImageSize(Parent parent, String url, WebView webView) {
        try {
            while (parent.getParent() != null && parent.getId() != null && !Objects.equals(parent.getId(), "chatBox")) {
                parent = parent.getParent();
            }
            Bounds bounds = parent.getBoundsInLocal();
            int height = 0;
            int width = 0;
            if (!urlType.equals("None")) {
                URL url_stream = new URL(url);
                BufferedImage image = ImageIO.read(url_stream.openStream());
                if (image != null) {
                    height = image.getHeight();
                    width = image.getWidth();
                }
            }
            setSizeWebView(webView, bounds, height, width);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setSizeWebView(WebView webView, Bounds bounds,int height, int width) {
        double maxX = bounds.getMaxX();
        double maxY = bounds.getMaxY();
        if (height != 0 && width != 0 && (height < maxY - 50 || width < maxX - 50)) {
            webView.setMaxSize(width, height);
        } else {
            webView.setMaxSize(maxX - 50, maxY - 50);
        }
        webView.autosize();
    }

    public void setScroll(Runnable scroll) {
        this.scroll = scroll;
    }

    public String getUrlType() {
        return urlType;
    }
}
