package de.uniks.stp.controller.home;


import de.uniks.stp.StageManager;
import de.uniks.stp.builder.ModelBuilder;
import de.uniks.stp.cellfactories.PrivateChatListCell;
import de.uniks.stp.cellfactories.UserListCell;
import de.uniks.stp.controller.AudioConnectedBoxController;
import de.uniks.stp.controller.ChatViewController;
import de.uniks.stp.controller.UserProfileController;
import de.uniks.stp.model.CurrentUser;
import de.uniks.stp.model.Message;
import de.uniks.stp.model.PrivateChat;
import de.uniks.stp.model.User;
import de.uniks.stp.net.RestClient;
import de.uniks.stp.net.websocket.privatesocket.PrivateChatWebSocket;
import de.uniks.stp.net.websocket.privatesocket.PrivateSystemWebSocketClient;
import de.uniks.stp.util.ResourceManager;
import de.uniks.stp.util.SortUser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import kong.unirest.JsonNode;
import org.json.JSONArray;

import javax.json.JsonException;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.ResourceBundle;

import static de.uniks.stp.util.Constants.*;

public class PrivateViewController {
    private final RestClient restClient;

    private final Parent view;
    private final ModelBuilder builder;
    private HBox root;
    private VBox currentUserBox;
    private VBox audioConnectionBox;
    private Button disconnectAudioButton;
    private VBox chatBox;
    private ListView<PrivateChat> privateChatList;
    private ListView<User> onlineUsersList;
    private Label welcomeToAccord;
    private ChatViewController chatViewController;
    private PrivateSystemWebSocketClient privateSystemWebSocketClient;
    private PrivateChatWebSocket privateChatWebSocket;
    private Button headphoneButton;
    private Button microphoneButton;
    private Label headphoneLabel;
    private Label microphoneLabel;
    private UserProfileController userProfileController;

    public PrivateViewController(Parent view, ModelBuilder modelBuilder) {
        this.view = view;
        this.builder = modelBuilder;
        restClient = modelBuilder.getRestClient();
        this.privateSystemWebSocketClient = modelBuilder.getUSER_CLIENT();
        this.privateChatWebSocket = modelBuilder.getPrivateChatWebSocketClient();
    }

    public ChatViewController getChatViewController() {
        return chatViewController;
    }

    public UserProfileController getUserProfileController() {
        return userProfileController;
    }

    @SuppressWarnings("unchecked")
    public void init() {
        root = (HBox) view.lookup("#root");
        currentUserBox = (VBox) view.lookup("#currentUserBox");
        audioConnectionBox = (VBox) view.lookup("#audioConnectionBox");
        chatBox = (VBox) view.lookup("#chatBox");
        privateChatList = (ListView<PrivateChat>) view.lookup("#privateChatList");
        privateChatList.setCellFactory(new PrivateChatListCell(builder));
        this.privateChatList.setOnMouseReleased(mouseEvent -> onPrivateChatListClicked());
        ObservableList<PrivateChat> privateChats = FXCollections.observableArrayList();
        this.privateChatList.setItems(privateChats);
        onlineUsersList = (ListView<User>) view.lookup("#onlineUsers");
        UserListCell userListCell = new UserListCell(builder);
        userListCell.setRoot(root);
        onlineUsersList.setCellFactory(userListCell);
        this.onlineUsersList.setOnMouseReleased(this::onOnlineUsersListClicked);
        welcomeToAccord = (Label) view.lookup("#welcomeToAccord");
        showCurrentUser();
        showUsers();
        onLanguageChanged();
        if (privateChatWebSocket == null) {
            privateChatWebSocket = new PrivateChatWebSocket(URI.create(WS_SERVER_URL + WEBSOCKET_PATH + CHAT_WEBSOCKET_PATH + builder.
                    getPersonalUser().getName().replace(" ", "+")), builder.getPersonalUser().getUserKey());
        }
        privateChatWebSocket.setBuilder(builder);
        privateChatWebSocket.setPrivateViewController(this);
        builder.setPrivateChatWebSocketClient(privateChatWebSocket);
        builder.setHandleMicrophoneHeadphone(this::handleMicrophoneHeadphone);
    }

    private void startWebSocketConnection() {
        if (privateSystemWebSocketClient == null) {
            privateSystemWebSocketClient = new PrivateSystemWebSocketClient(URI.create(WS_SERVER_URL + WEBSOCKET_PATH + SYSTEM_WEBSOCKET_PATH), builder.getPersonalUser().getUserKey());
            privateSystemWebSocketClient.setBuilder(builder);
            privateSystemWebSocketClient.setPrivateViewController(this);
        }
        privateSystemWebSocketClient.setPrivateViewController(this);
        privateSystemWebSocketClient.setBuilder(builder);
        builder.setUSER_CLIENT(privateSystemWebSocketClient);
    }


    public ListView<PrivateChat> getPrivateChatList() {
        return privateChatList;
    }


    /**
     * Get the Online Users and reset old Online User List with new Online Users
     */
    public void showUsers() {
        restClient.getUsers(builder.getPersonalUser().getUserKey(), response -> {
            JSONArray jsonResponse = response.getBody().getObject().getJSONArray("data");
            for (int i = 0; i < jsonResponse.length(); i++) {
                String userName = jsonResponse.getJSONObject(i).get("name").toString();
                String userId = jsonResponse.getJSONObject(i).get("id").toString();
                String description = jsonResponse.getJSONObject(i).get("description").toString();
                if (!userName.equals(builder.getPersonalUser().getName())) {
                    builder.buildUser(userName, userId, description);
                } else {
                    builder.getPersonalUser().setId(userId);
                }
            }
            builder.loadUserVolumes();
            Platform.runLater(() -> onlineUsersList.setItems(FXCollections.observableList(builder.getPersonalUser().
                    getUser()).sorted(new SortUser())));
        });
        startWebSocketConnection();
    }

    /**
     * Display Current User
     */
    private void showCurrentUser() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(StageManager.class.getResource("controller/UserProfileView.fxml")));
            userProfileController = new UserProfileController(root, builder);
            userProfileController.init();
            CurrentUser currentUser = builder.getPersonalUser();
            userProfileController.setUserName(currentUser.getName());
            userProfileController.setOnline();
            builder.setUserProfileController(userProfileController);
            this.currentUserBox.getChildren().clear();
            this.currentUserBox.getChildren().add(root);
            headsetSettings();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addUserProfileController() {
        this.currentUserBox.getChildren().clear();
        this.currentUserBox.getChildren().add(builder.getUserProfileController().root);
    }

    /**
     * set and synchronize headsetButtons
     */
    public void headsetSettings() {
        if (headphoneButton == null) {
            headphoneButton = (Button) view.lookup("#mute_headphone");
            microphoneButton = (Button) view.lookup("#mute_microphone");
            headphoneLabel = (Label) view.lookup("#unmute_headphone");
            microphoneLabel = (Label) view.lookup("#unmute_microphone");
        }
        //load headset settings
        headphoneButton.setOnAction(this::muteHeadphone);
        microphoneButton.setOnAction(this::muteMicrophone);
        microphoneLabelInit();
        headphoneLabelInit();
    }

    private void headphoneLabelInit() {
        headphoneLabel.setVisible(builder.getMuteHeadphones());
        headphoneLabel.setOnMouseEntered(event -> {
            if (builder.getTheme().equals("Dark")) {
                headphoneButton.setStyle("-fx-background-color: #ffbdbd; -fx-background-radius: 80;");
            } else {
                headphoneButton.setStyle("-fx-background-color: #a0bade; -fx-background-radius: 80;");
            }
        });
        headphoneLabel.setOnMouseExited(event -> {
            if (builder.getTheme().equals("Dark")) {
                headphoneButton.setStyle("");
                setDarkMode();
            } else {
                headphoneButton.setStyle("");
                setWhiteMode();
            }
        }); //unMute headphone
        headphoneLabel.setOnMouseClicked(event -> {
            headphoneLabel.setVisible(false);
            builder.muteHeadphones(false);
            if (!builder.getMicrophoneFirstMuted()) {
                microphoneLabel.setVisible(false);
                builder.muteMicrophone(false);
            }
        });
    }

    private void microphoneLabelInit() {
        microphoneLabel.setVisible(builder.getMuteMicrophone());
        microphoneLabel.setOnMouseClicked(event -> {
            microphoneLabel.setVisible(false);
            builder.muteMicrophone(false);
            builder.setMicrophoneFirstMuted(false);
            if (builder.getMuteHeadphones()) {
                builder.muteHeadphones(false);
                headphoneLabel.setVisible(false);
            }
        });
        microphoneLabel.setOnMouseEntered(event -> {
            if (builder.getTheme().equals("Dark")) {
                microphoneButton.setStyle("-fx-background-color: #ffbdbd; -fx-background-radius: 80;");
            } else {
                microphoneButton.setStyle("-fx-background-color: #a0bade; -fx-background-radius: 80;");
            }
        });
        microphoneLabel.setOnMouseExited(event -> {
            if (builder.getTheme().equals("Dark")) {
                microphoneButton.setStyle("");
                setDarkMode();
            } else {
                microphoneButton.setStyle("");
                setWhiteMode();
            }
        });
    }

    /**
     * change microphone setting
     */
    private void muteMicrophone(ActionEvent actionEvent) {
        builder.setMicrophoneFirstMuted(true);
        microphoneLabel.setVisible(true);
        builder.muteMicrophone(true);
    }

    /**
     * change headphone setting
     */
    private void muteHeadphone(ActionEvent actionEvent) {
        headphoneLabel.setVisible(true);
        microphoneLabel.setVisible(true);
        builder.muteHeadphones(true);
        builder.muteMicrophone(true);
    }

    public void handleMicrophoneHeadphone() {
        headphoneLabel.setVisible(builder.getMuteHeadphones());
        microphoneLabel.setVisible(builder.getMuteMicrophone());
    }

    /**
     * Display AudioConnectedBox
     */
    public void showAudioConnectedBox() {
        if (builder.getCurrentAudioChannel() != null) {
            try {
                Parent root = FXMLLoader.load(Objects.requireNonNull(StageManager.class.getResource("controller/AudioConnectedBox.fxml")));
                AudioConnectedBoxController audioConnectedBoxController = new AudioConnectedBoxController(root);
                audioConnectedBoxController.init();
                audioConnectedBoxController.setServerName(builder.getCurrentAudioChannel().getCategories().getServer().getName());
                audioConnectedBoxController.setAudioChannelName(builder.getCurrentAudioChannel().getName());

                Platform.runLater(() -> {
                    this.audioConnectionBox.getChildren().clear();
                    this.audioConnectionBox.getChildren().add(root);
                    disconnectAudioButton = (Button) view.lookup("#button_disconnectAudio");
                    disconnectAudioButton.setOnAction(this::onAudioDisconnectClicked);
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (this.audioConnectionBox.getChildren().size() > 0) {
            this.audioConnectionBox.getChildren().clear();
        }
    }

    /**
     * when audio disconnect button is clicked
     */
    private void onAudioDisconnectClicked(ActionEvent actionEvent) {
        builder.getRestClient().leaveVoiceChannel(builder.getCurrentAudioChannel().getCategories().getServer().getId(), builder.getCurrentAudioChannel().getCategories().getId(), builder.getCurrentAudioChannel().getId(), builder.getPersonalUser().getUserKey(), response -> {
            this.disconnectAudioButton.setOnAction(null);

            JsonNode body = response.getBody();
            String status = body.getObject().getString("status");
            if (status.equals("success")) {
                builder.playChannelSound("left");
            }
        });

        Platform.runLater(() -> this.audioConnectionBox.getChildren().clear());
    }

    /**
     * Opens the existing chat and shows the messages
     */
    private void onPrivateChatListClicked() {
        if (builder.getCurrentChatViewController() != null) {
            builder.getCurrentChatViewController().stopMediaPlayers();
        }
        if (this.privateChatList.getSelectionModel().getSelectedItem() != null) {
            if (builder.getCurrentPrivateChat() == null || !builder.getCurrentPrivateChat().equals(this.privateChatList.getSelectionModel().
                    getSelectedItem())) {
                builder.setCurrentPrivateChat(this.privateChatList.getSelectionModel().getSelectedItem());
                updateUnreadMessageCounter();
                this.privateChatList.refresh();
                MessageViews();
            }
        }
    }

    private void updateUnreadMessageCounter() {
        if (builder.getCurrentPrivateChat().getUnreadMessagesCounter() > 0) {
            builder.getCurrentPrivateChat().setUnreadMessagesCounter(0);
        }
    }

    /**
     * Message View cleanup and display recent messages with selected Chat
     */
    public void MessageViews() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(StageManager.class.getResource("controller/ChatView.fxml")), builder.getStageManager().getLangBundle());
            //stop videos from recent chatViewController
            if (chatViewController != null) {
                chatViewController.stopMediaPlayers();
            }
            chatViewController = new ChatViewController(root, builder);
            this.chatBox.getChildren().clear();
            chatViewController.init();
            chatViewController.setTheme();
            this.chatBox.getChildren().add(root);
            privateChatWebSocket.setChatViewController(chatViewController);
            builder.setCurrentChatViewController(chatViewController);
            if (builder.getCurrentPrivateChat() != null) {
                for (Message msg : builder.getCurrentPrivateChat().getMessage()) {
                    // Display each Message which are saved
                    chatViewController.printMessage(msg, false);
                }
            }
        } catch (IOException | JsonException e) {
            e.printStackTrace();
        }
    }

    /**
     * Event Mouseclick on an online user
     * Create new channel if chat not existing or open the existing chat and shows the messages
     *
     * @param mouseEvent is called when clicked on an online User
     */
    private void onOnlineUsersListClicked(MouseEvent mouseEvent) {
        if (builder.getCurrentChatViewController() != null) {
            builder.getCurrentChatViewController().stopMediaPlayers();
        }
        PrivateChat currentChannel = builder.getCurrentPrivateChat();
        if (mouseEvent.getClickCount() == 2 && this.onlineUsersList.getItems().size() != 0) {

            String selectedUserName = this.onlineUsersList.getSelectionModel().getSelectedItem().getName();

            boolean chatExisting = checkIfChatExists(selectedUserName);
            if (!chatExisting) {
                updatePrivateChatlist(selectedUserName);
            }
            if (!builder.getCurrentPrivateChat().equals(currentChannel)) {
                MessageViews();
            }
        }
    }

    private void updatePrivateChatlist(String selectedUserName) {
        String selectUserId = this.onlineUsersList.getSelectionModel().getSelectedItem().getId();
        PrivateChat newPrivateChat = new PrivateChat().setName(selectedUserName).setId(selectUserId);
        builder.getPersonalUser().withPrivateChat(newPrivateChat);
        try {
            // load messages for new channel
            newPrivateChat.withMessage(ResourceManager.loadPrivatChat(builder.getPersonalUser().getName(), newPrivateChat.getName(), newPrivateChat));
        } catch (IOException | JsonException | com.github.cliftonlabs.json_simple.JsonException e) {
            e.printStackTrace();
        }
        builder.setCurrentPrivateChat(newPrivateChat);
        this.privateChatList.setItems(FXCollections.observableArrayList(builder.getPersonalUser().getPrivateChat()));
    }

    private boolean checkIfChatExists(String selectedUserName) {
        for (PrivateChat channel : builder.getPersonalUser().getPrivateChat()) {
            if (channel.getName().equals(selectedUserName)) {
                builder.setCurrentPrivateChat(channel);
                updateUnreadMessageCounter();
                this.privateChatList.refresh();
                return true;
            }
        }
        return false;
    }

    /**
     * Stop running Actions when Controller gets closed
     */
    public void stop() {
        this.onlineUsersList.setOnMouseReleased(null);
        this.privateChatList.setOnMouseReleased(null);
        if (userProfileController != null) {
            userProfileController.stop();
        }
        try {
            if (privateSystemWebSocketClient != null && privateSystemWebSocketClient.getSession() != null) {
                privateSystemWebSocketClient.stop();
            }
            if (privateChatWebSocket != null && privateChatWebSocket.getSession() != null) {
                privateChatWebSocket.stop();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        builder.setHandleMicrophoneHeadphone(null);
    }

    public ListView<User> getOnlineUsersList() {
        return onlineUsersList;
    }


    /**
     * when language changed reset labels and texts with correct language
     */
    public void onLanguageChanged() {
        ResourceBundle lang = builder.getStageManager().getLangBundle();
        if (welcomeToAccord != null)
            welcomeToAccord.setText(lang.getString("label.welcome_to_accord"));
        if (disconnectAudioButton != null)
            disconnectAudioButton.setText(lang.getString("Button.disconnect"));
        if (chatViewController != null) {
            chatViewController.onLanguageChanged();
        }
        if (userProfileController != null) {
            userProfileController.onLanguageChanged();
        }
        // needed for update users description
        Platform.runLater(() -> onlineUsersList.refresh());
    }

    public void setTheme() {
        if (builder.getTheme().equals("Bright")) {
            setWhiteMode();
        } else {
            setDarkMode();
        }
        if (builder.getCurrentPrivateChat() != null) {
            MessageViews();
        }
    }

    private void setWhiteMode() {
        root.getStylesheets().clear();
        root.getStylesheets().add(Objects.requireNonNull(StageManager.class.getResource("styles/themes/bright/PrivateView.css")).toExternalForm());
        if (chatViewController != null) {
            chatViewController.setTheme();
        }
    }

    private void setDarkMode() {
        root.getStylesheets().clear();
        root.getStylesheets().add(Objects.requireNonNull(StageManager.class.getResource("styles/themes/dark/PrivateView.css")).toExternalForm());
        if (chatViewController != null) {
            chatViewController.setTheme();
        }
    }
}
