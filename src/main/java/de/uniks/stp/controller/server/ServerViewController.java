package de.uniks.stp.controller.server;

import de.uniks.stp.StageManager;
import de.uniks.stp.builder.ModelBuilder;
import de.uniks.stp.cellfactories.UserListCell;
import de.uniks.stp.controller.AudioConnectedBoxController;
import de.uniks.stp.controller.ChatViewController;
import de.uniks.stp.controller.UserProfileController;
import de.uniks.stp.controller.home.HomeViewController;
import de.uniks.stp.controller.server.subcontroller.CategorySubController;
import de.uniks.stp.model.*;
import de.uniks.stp.net.RestClient;
import de.uniks.stp.net.websocket.serversocket.ServerChatWebSocket;
import de.uniks.stp.net.websocket.serversocket.ServerSystemWebSocket;
import de.uniks.stp.util.SortUser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import kong.unirest.JsonNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.*;

import static de.uniks.stp.util.Constants.*;

/**
 * The class ServerViewController is about showing the ServerView. It is used to update the builder.
 */
public class ServerViewController {

    private final ModelBuilder builder;
    private final RestClient restClient;
    private final Server server;
    private final Parent view;
    private final HomeViewController homeViewController;
    private MenuButton serverMenuButton;
    private Label textChannelLabel;
    private Label generalLabel;
    private Label welcomeToAccord;
    private ListView<User> onlineUsersList;
    private ListView<User> offlineUsersList;
    private VBox currentUserBox;
    private HBox root;
    private VBox chatBox;
    private ChatViewController chatViewController;
    private MenuItem serverSettings;
    private MenuItem inviteUsers;
    private Map<Categories, CategorySubController> categorySubControllerList;
    private VBox categoryBox;
    private Line dividerLineUser;
    private VBox userBox;
    private int loadedCategories;
    private int loadedChannel;
    private ServerChannel currentChannel;
    private ServerSystemWebSocket serverSystemWebSocket;
    private ServerChatWebSocket chatWebSocketClient;
    private VBox audioConnectionBox;
    private Button disconnectAudioButton;
    private Label headphoneLabel;
    private Label microphoneLabel;
    private UserProfileController userProfileController;
    private ArrayList<User> onlineUsers;
    private ArrayList<User> offlineUsers;

    /**
     * "ServerViewController takes Parent view, ModelBuilder modelBuilder, Server server.
     */
    public ServerViewController(Parent view, ModelBuilder modelBuilder, Server server, HomeViewController homeViewController) {
        this.view = view;
        builder = modelBuilder;
        this.server = server;
        this.restClient = modelBuilder.getRestClient();
        this.homeViewController = homeViewController;
        this.serverSystemWebSocket = builder.getServerSystemWebSocket();
        this.chatWebSocketClient = builder.getServerChatWebSocketClient();
        this.onlineUsers = new ArrayList<>();
        this.offlineUsers = new ArrayList<>();
    }


    public ChatViewController getChatViewController() {
        return chatViewController;
    }

    public Map<Categories, CategorySubController> getCategorySubControllerList() {
        return categorySubControllerList;
    }

    public VBox getCategoryBox() {
        return categoryBox;
    }

    public HomeViewController getHomeViewController() {
        return homeViewController;
    }

    public Server getServer() {
        return server;
    }

    public ServerChannel getCurrentChannel() {
        return this.currentChannel;
    }

    public void setCurrentChannel(ServerChannel channel) {
        this.currentChannel = channel;
    }

    public ServerSystemWebSocket getServerSystemWebSocket() {
        return serverSystemWebSocket;
    }

    public ServerChatWebSocket getChatWebSocketClient() {
        return chatWebSocketClient;
    }

    public ServerChannel getCurrentAudioChannel() {
        return builder.getCurrentAudioChannel();
    }

    public ListView<User> getOnlineUsersList() {
        return onlineUsersList;
    }


    /**
     * set User to MuteList
     */
    public void setMutedAudioMember(String user) {
        builder.getAudioStreamClient().setMutedUser(user);
    }

    /**
     * remove User from MuteList
     */
    public void setUnMutedAudioMember(String user) {
        builder.getAudioStreamClient().setUnMutedUser(user);
    }

    /**
     * get all mutedUsers
     */
    public ArrayList<String> getMutedAudioMember() {
        //return empty array if audioStreamClient doesn't exist
        if (builder.getAudioStreamClient() == null) {
            return new ArrayList<>();
        } else {
            return builder.getAudioStreamClient().getMutedAudioMember();
        }
    }


    /**
     * Callback, when all server information are loaded
     */
    public interface ServerReadyCallback {
        void onSuccess(String status);
    }

    /**
     * Initialise all view parameters
     */
    @SuppressWarnings("unchecked")
    public void startController(ServerReadyCallback serverReadyCallback) {
        root = (HBox) view.lookup("#root");
        serverMenuButton = (MenuButton) view.lookup("#serverMenuButton");
        ScrollPane scrollPaneCategories = (ScrollPane) view.lookup("#scrollPaneCategories");
        categoryBox = (VBox) scrollPaneCategories.getContent().lookup("#categoryVbox");
        textChannelLabel = (Label) view.lookup("#textChannel");
        generalLabel = (Label) view.lookup("#general");
        welcomeToAccord = (Label) view.lookup("#welcomeToAccord");
        ScrollPane scrollPaneUserBox = (ScrollPane) view.lookup("#scrollPaneUserBox");
        currentUserBox = (VBox) view.lookup("#currentUserBox");
        audioConnectionBox = (VBox) view.lookup("#audioConnectionBox");
        onlineUsersList = (ListView<User>) scrollPaneUserBox.getContent().lookup("#onlineUsers");
        onlineUsersList.setCellFactory(new UserListCell(builder));
        offlineUsersList = (ListView<User>) scrollPaneUserBox.getContent().lookup("#offlineUsers");
        offlineUsersList.setCellFactory(new UserListCell(builder));
        dividerLineUser = (Line) scrollPaneUserBox.getContent().lookup("#dividerline_online_offline_user");
        userBox = (VBox) scrollPaneUserBox.getContent().lookup("#userBox");
        chatBox = (VBox) view.lookup("#chatBox");
        categorySubControllerList = new HashMap<>();
        currentChannel = null;
        loadServerInfo(status -> {
            if (status.equals("success") && getServer().getCategories().size() == 0) {
                loadCategories(serverReadyCallback::onSuccess);
            }
        }); // members & (categories)
        buildSystemWebSocket();
        buildChatWebSocket();
        onLanguageChanged();
    }

    /**
     * Initialise all view parameters
     */
    public void startShowServer() throws InterruptedException {
        serverMenuButton.setText(this.server.getName());
        serverSettings = serverMenuButton.getItems().get(0);
        serverSettings.setOnAction(this::onServerSettingsClicked);
        if (serverMenuButton.getItems().size() > 1) {
            inviteUsers = serverMenuButton.getItems().get(1);
            inviteUsers.setOnAction(this::onInviteUsersClicked);
        }
        onLanguageChanged();
        builder.setServerChatWebSocketClient(this.chatWebSocketClient); // TODO because of message view
        builder.setHandleMicrophoneHeadphone(this::handleMicrophoneHeadphone);

        showCurrentUser();
        showOnlineOfflineUsers();

        if (builder.getCurrentAudioChannel() != null) {
            showAudioConnectedBox();
        } else if (this.audioConnectionBox.getChildren().size() > 0) {
            this.audioConnectionBox.getChildren().clear();
        }
        Platform.runLater(this::generateCategoriesChannelViews);
        if (currentChannel != null) {
            showMessageView();
        }
    }

    /**
     * WebSocket for system messages.
     */
    public void buildSystemWebSocket() {
        if (serverSystemWebSocket == null) {
            serverSystemWebSocket = new ServerSystemWebSocket(URI.create(WS_SERVER_URL + WEBSOCKET_PATH + SERVER_SYSTEM_WEBSOCKET_PATH + this.server.getId()), builder.getPersonalUser().getUserKey());
            serverSystemWebSocket.setServerViewController(this);
            serverSystemWebSocket.setBuilder(builder);
        }
        serverSystemWebSocket.setServerViewController(this);
        serverSystemWebSocket.setBuilder(builder);
    }

    /**
     * WebSocket for chat messages.
     */
    private void buildChatWebSocket() {
        if (chatWebSocketClient == null) {
            chatWebSocketClient = new ServerChatWebSocket(URI.
                    create(WS_SERVER_URL + WEBSOCKET_PATH + CHAT_WEBSOCKET_PATH + builder.
                            getPersonalUser().getName().replace(" ", "+") + SERVER_WEBSOCKET_PATH + this.server.getId()), builder.getPersonalUser().getUserKey());
            chatWebSocketClient.setServerViewController(this);
            chatWebSocketClient.setBuilder(builder);
        }
        chatWebSocketClient.setServerViewController(this);
        chatWebSocketClient.setBuilder(builder);
    }

    /**
     * Method for changing the current serverName.
     */
    public void changeServerName() {
        serverMenuButton.setText(server.getName());
    }

    /**
     * Initial Chat View and load chat history which is saved in list
     */
    public void showMessageView() {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(StageManager.class.getResource("controller/ChatView.fxml")), builder.getStageManager().getLangBundle());
            // stop videos from recent channel
            if (chatViewController != null) {
                chatViewController.stopMediaPlayers();
            }
            this.chatViewController = new ChatViewController(root, builder, currentChannel);
            this.chatBox.getChildren().clear();
            this.chatViewController.init();
            this.chatBox.getChildren().add(root);
            chatViewController.setTheme();
            builder.setCurrentChatViewController(chatViewController);
            chatWebSocketClient.setChatViewController(chatViewController);
            serverSystemWebSocket.setChatViewController(chatViewController);
            if (this.server != null && currentChannel != null) {
                for (Message msg : currentChannel.getMessage()) {
                    // Display each Message which are saved
                    chatViewController.printMessage(msg, false);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Display Current User and the headsetButtons
     */
    private void showCurrentUser() {
        userProfileController = builder.getUserProfileController();
        this.currentUserBox.getChildren().clear();
        this.currentUserBox.getChildren().add(userProfileController.root);

        Button headphoneButton = (Button) view.lookup("#mute_headphone");
        Button microphoneButton = (Button) view.lookup("#mute_microphone");
        headphoneLabel = (Label) view.lookup("#unmute_headphone");
        microphoneLabel = (Label) view.lookup("#unmute_microphone");
        //load headset settings
        headphoneButton.setOnAction(this::muteHeadphone);
        microphoneButton.setOnAction(this::muteMicrophone);

        microphoneLabelInit(microphoneButton);
        headphoneLabelInit(headphoneButton);
    }

    private void headphoneLabelInit(Button headphoneButton) {
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

    private void microphoneLabelInit(Button microphoneButton) {
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
                    disconnectAudioButton.setOnAction(actionEvent -> onAudioDisconnectClicked());
                    onLanguageChanged();
                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (this.audioConnectionBox.getChildren().size() > 0) {
            Platform.runLater(() -> this.audioConnectionBox.getChildren().clear());
        }
    }

    /**
     * when audio disconnect button is clicked
     */
    public void onAudioDisconnectClicked() {
        leaveVoiceChannel(builder.getCurrentAudioChannel().getCategories().getServer().getId(), builder.getCurrentAudioChannel().getCategories().getId(), builder.getCurrentAudioChannel().getId());
    }

    /**
     * is called when server owner and not server owner disconnects from audioChannel
     */
    public void leaveVoiceChannel(String serverId, String categoryId, String channelId) {
        builder.getRestClient().leaveVoiceChannel(serverId, categoryId, channelId, builder.getPersonalUser().getUserKey(), response -> {
            JsonNode body = response.getBody();
            String status = body.getObject().getString("status");
            if (status.equals("success")) {
                builder.playChannelSound("left");
            }

            this.disconnectAudioButton.setOnAction(null);
        });

        Platform.runLater(() -> this.audioConnectionBox.getChildren().clear());
    }

    /**
     * Update the builder and get the ServerUser as well as the categories. Also sets their online and offline Status.
     */
    public interface ServerInfoCallback {
        void onSuccess(String status);
    }

    /**
     * Method to get Server information
     */
    public void loadServerInfo(ServerInfoCallback serverInfoCallback) {
        restClient.getServerUsers(this.server.getId(), builder.getPersonalUser().getUserKey(), response -> {
            JsonNode body = response.getBody();
            String status = body.getObject().getString("status");
            this.server.setOwner(body.getObject().getJSONObject("data").getString("owner"));
            if (status.equals("success")) {
                JSONArray members = body.getObject().getJSONObject("data").getJSONArray("members");
                for (int i = 0; i < members.length(); i++) {
                    JSONObject member = members.getJSONObject(i);
                    String id = member.getString("id");
                    String description = member.getString("description");
                    String name = member.getString("name");
                    boolean online = member.getBoolean("online");
                    builder.buildServerUser(this.server, name, id, online, description);
                }
                serverInfoCallback.onSuccess(status);
            } else if (status.equals("failure")) {
            }
        });
    }

    /**
     * Split Users into offline and online users then update the list
     */
    public void showOnlineOfflineUsers() {
        onlineUsers.clear();
        offlineUsers.clear();
        for (User user : this.server.getUser()) {
            if (user.isStatus()) {
                handleOnlineUser(user);
            } else {
                handleOfflineUser(user);
            }
        }

        Platform.runLater(() -> {
            this.dividerLineUser.setVisible(onlineUsers.size() > 0 && offlineUsers.size() > 0);
            if (onlineUsers.size() == 0) {
                onlineUsersList.setItems(FXCollections.observableList(onlineUsers).sorted(new SortUser()));
                onlineUsersList.setPrefHeight(0);
                offlineUsersList.setItems(FXCollections.observableList(offlineUsers).sorted(new SortUser()));
                offlineUsersList.setPrefHeight(((offlineUsers.size()) * (40)) + (offlineUsers.size() * 5) + offlineUsers.size() + 2);
                userBox.setSpacing(0);
            } else {
                int heightOnlineUser = heightOnlineUsers();
                onlineUsersList.setItems(FXCollections.observableList(onlineUsers).sorted(new SortUser()));
                onlineUsersList.setPrefHeight(heightOnlineUser);
                offlineUsersList.setItems(FXCollections.observableList(offlineUsers).sorted(new SortUser()));
                offlineUsersList.setPrefHeight(((offlineUsers.size()) * (40)) + (offlineUsers.size() * 5) + offlineUsers.size() + 2);
            }
        });
    }

    private int heightOnlineUsers() {
        int heightOnlineUser = 0;
        for (User user : onlineUsers) {
            if (user.getDescription() != null && (user.getDescription().contains("?") && user.getDescription().length() >= 2)) {
                //54 cell size and 5 for padding
                heightOnlineUser += 54 + 5;
            } else {
                //40 cell size and 5 for padding
                heightOnlineUser += 40 + 5;
            }
        }
        heightOnlineUser += onlineUsers.size() + 2;
        return heightOnlineUser;
    }

    private void handleOfflineUser(User user) {
        if (!this.server.getCurrentUser().getName().equals(user.getName())) {
            offlineUsers.add(user);
        }
    }

    private void handleOnlineUser(User user) {
        if (user.getName().equals(builder.getPersonalUser().getName())) {
            Platform.runLater(() -> checkForOwnership(user.getId()));
        } else {
            onlineUsers.add(user);
        }
    }

    /**
     * Callback, when all category information are loaded
     */
    public interface CategoriesLoadedCallback {
        void onSuccess(String status);
    }

    /**
     * Gets categories from server and adds in list
     */
    public void loadCategories(CategoriesLoadedCallback categoriesLoadedCallback) {
        restClient.getServerCategories(this.server.getId(), builder.getPersonalUser().getUserKey(), response -> {
            JsonNode body = response.getBody();
            String status = body.getObject().getString("status");
            if (status.equals("success")) {
                JSONArray data = body.getObject().getJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    JSONObject categoryInfo = data.getJSONObject(i);
                    Categories categories = new Categories();
                    categories.setId(categoryInfo.getString("id"));
                    categories.setName(categoryInfo.getString("name"));
                    this.server.withCategories(categories);
                    loadChannelsCallback(categories, data, categoriesLoadedCallback);
                }
            }
        });
    }

    private void loadChannelsCallback(Categories categories, JSONArray data, CategoriesLoadedCallback categoriesLoadedCallback) {
        loadChannels(categories, status1 -> {
            loadedCategories++;
            if (loadedCategories == data.length()) {
                loadedCategories = 0;
                categoriesLoadedCallback.onSuccess(status1);
            }
        });
    }

    /**
     * Callback, when all channel information are loaded
     */
    public interface ChannelLoadedCallback {
        void onSuccess(String status);
    }

    /**
     * Gets all channels for a category and adds in list
     *
     * @param cat the category to load the channels from it
     */
    public void loadChannels(Categories cat, ChannelLoadedCallback channelLoadedCallback) {
        restClient.getCategoryChannels(this.server.getId(), cat.getId(), builder.getPersonalUser().getUserKey(), response -> {
            JsonNode body = response.getBody();
            String status = body.getObject().getString("status");
            if (status.equals("success")) {
                JSONArray data = body.getObject().getJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    JSONObject channelInfo = data.getJSONObject(i);
                    ServerChannel channel = createServerChannel(channelInfo, cat);

                    JSONObject json = new JSONObject(channelInfo.toString());

                    addAudioMemberToChannel(channel, json);
                    channelAddPrivledUsers(channel, json);

                    loadChannelData(channel, data, channelLoadedCallback);
                }
            }
        });
    }

    private void loadChannelData(ServerChannel channel, JSONArray data, ChannelLoadedCallback channelLoadedCallback) {
        if (channel.getType().equals("text")) {
            loadChannelMessages(channel, status1 -> {
                loadedChannel++;
                if (loadedChannel == data.length()) {
                    loadedChannel = 0;
                    channelLoadedCallback.onSuccess(status1);
                }
            });
        } else {
            loadedChannel++;
            if (loadedChannel == data.length()) {
                loadedChannel = 0;
                channelLoadedCallback.onSuccess("success");
            }
        }
    }

    private void channelAddPrivledUsers(ServerChannel channel, JSONObject json) {
        JSONArray jsonArray = json.getJSONArray("members");
        String memberId;
        for (int j = 0; j < jsonArray.length(); j++) {
            memberId = jsonArray.getString(j);
            for (User user : this.server.getUser()) {
                if (user.getId().equals(memberId)) {
                    channel.withPrivilegedUsers(user);
                }
            }
        }
    }

    private void addAudioMemberToChannel(ServerChannel channel, JSONObject json) {
        JSONArray jsonAudioMembers = json.getJSONArray("audioMembers");
        for (int j = 0; j < jsonAudioMembers.length(); j++) {
            String AudioMemberId = jsonAudioMembers.getString(j);
            for (User user : this.server.getUser()) {
                if (user.getId().equals(AudioMemberId)) {
                    AudioMember audioMemberUser = new AudioMember().setId(AudioMemberId).setName(user.getName());
                    channel.withAudioMember(audioMemberUser);
                }
            }
        }
    }

    private ServerChannel createServerChannel(JSONObject channelInfo, Categories cat) {
        ServerChannel channel = new ServerChannel();
        channel.setCurrentUser(builder.getPersonalUser());
        channel.setId(channelInfo.getString("id"));
        channel.setName(channelInfo.getString("name"));
        channel.setType(channelInfo.getString("type"));
        channel.setCategories(cat);
        channel.setPrivilege(channelInfo.getBoolean("privileged"));
        return channel;
    }

    /**
     * Callback, when all message information are loaded
     */
    public interface MessagesLoadedCallback {
        void onSuccess(String status);

    }

    public void loadChannelMessages(ServerChannel channel, MessagesLoadedCallback messagesLoadedCallback) {
        restClient.getChannelMessages(new Date().getTime(), this.server.getId(), channel.getCategories().getId(), channel.getId(), builder.getPersonalUser().getUserKey(), response -> {
            JsonNode body = response.getBody();
            String status = body.getObject().getString("status");
            if (status.equals("success")) {
                JSONArray data = body.getObject().getJSONArray("data");
                for (int i = 0; i < data.length(); i++) {
                    JSONObject jsonData = data.getJSONObject(i);
                    String from = jsonData.getString("from");
                    long timestamp = jsonData.getLong("timestamp");
                    String text = jsonData.getString("text");
                    String id = jsonData.getString("id");
                    Message message = new Message().setMessage(text).setFrom(from).setTimestamp(timestamp).setId(id);
                    channel.withMessage(message);
                }
                messagesLoadedCallback.onSuccess(status);
            }
        });
    }

    public void stop() {
        if (userProfileController != null) {
            userProfileController.stop();
        }
        try {
            if (this.serverSystemWebSocket != null) {
                if (this.serverSystemWebSocket.getSession() != null) {
                    this.serverSystemWebSocket.stop();
                }
            }
            if (this.chatWebSocketClient != null) {
                if (this.chatWebSocketClient.getSession() != null) {
                    this.chatWebSocketClient.stop();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (this.serverSettings != null) {
            this.serverSettings.setOnAction(null);
        }
        if (serverMenuButton.getItems().size() > 1 && this.inviteUsers != null) {
            this.inviteUsers.setOnAction(null);
        }

        for (CategorySubController categorySubController : this.categorySubControllerList.values()) {
            categorySubController.stop();
        }

        if (builder.getAudioStreamClient() != null) {
            builder.getAudioStreamClient().disconnectStream();
            builder.setAudioStreamClient(null);
        }

        builder.setHandleMicrophoneHeadphone(null);
    }

    /**
     * when language changed reset labels and texts with correct language
     */
    public void onLanguageChanged() {
        ResourceBundle lang = builder.getStageManager().getLangBundle();
        if (textChannelLabel != null)
            textChannelLabel.setText(lang.getString("label.textChannel"));

        if (disconnectAudioButton != null)
            disconnectAudioButton.setText(lang.getString("Button.disconnect"));

        if (generalLabel != null)
            generalLabel.setText(lang.getString("label.general"));

        if (welcomeToAccord != null)
            welcomeToAccord.setText(lang.getString("label.welcome_to_accord"));

        if (serverSettings != null)
            serverSettings.setText(lang.getString("menuItem.serverSettings"));

        if (inviteUsers != null)
            inviteUsers.setText(lang.getString("menuItem.inviteUsers"));

        if (chatViewController != null)
            chatViewController.onLanguageChanged();

        if (userProfileController != null)
            userProfileController.onLanguageChanged();

        if (categorySubControllerList != null) {
            for (var categorySubController : categorySubControllerList.values()) {
                categorySubController.onLanguageChanged();
            }
        }
    }

    private void onServerSettingsClicked(ActionEvent actionEvent) {
        Platform.runLater(() -> builder.getStageManager().showServerSettingsScreen());
    }

    private void onInviteUsersClicked(ActionEvent actionEvent) {
        Platform.runLater(() -> builder.getStageManager().showInviteUsersScreen());
    }

    /**
     * generates new views for all categories of the server
     */
    private void generateCategoriesChannelViews() {
        Platform.runLater(() -> this.categoryBox.getChildren().clear());
        for (Categories categories : builder.getCurrentServer().getCategories()) {
            generateCategoryChannelView(categories);
        }
    }

    /**
     * generates a new view for a category with a FIXED width for the scrollPane
     */
    public void generateCategoryChannelView(Categories categories) {
        try {
            Parent view = FXMLLoader.load(Objects.requireNonNull(StageManager.class.getResource("controller/serverview/CategorySubView.fxml")));
            view.setId(categories.getId());
            CategorySubController tempCategorySubController = new CategorySubController(view, builder, this, categories);
            tempCategorySubController.init();
            tempCategorySubController.setTheme();
            categorySubControllerList.put(categories, tempCategorySubController);
            Platform.runLater(() -> this.categoryBox.getChildren().add(view));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkForOwnership(String id) {
        if (!this.server.getOwner().equals(id)) {
            if (serverMenuButton.getItems().size() >= 2) {
                serverMenuButton.getItems().remove(1);
            }
        }
    }

    /**
     * reset current channel and throw user out from chat view
     */
    public void throwOutUserFromChatView() {
        setCurrentChannel(null);
        if (this.chatViewController != null) {
            this.chatViewController.stop();
        }
        Platform.runLater(() -> this.chatBox.getChildren().clear());
    }

    /**
     * refresh all channels to avoid multiple visual selected channels
     */
    public void refreshAllChannelLists() {
        for (Map.Entry<Categories, CategorySubController> entry : categorySubControllerList.entrySet()) {
            entry.getValue().refreshChannelList();
        }
    }

    public void setTheme() {
        if (builder.getTheme().equals("Bright")) {
            setWhiteMode();
        } else {
            setDarkMode();
        }
        if (currentChannel != null) {
            showMessageView();
        }
        refreshAllChannelLists();
    }

    private void setWhiteMode() {
        root.getStylesheets().clear();
        root.getStylesheets().add(Objects.requireNonNull(StageManager.class.getResource("styles/themes/bright/ServerView.css")).toExternalForm());
        if (chatViewController != null) {
            chatViewController.setTheme();
        }
        for (Categories categories : server.getCategories()) {
            if (categorySubControllerList.size() != 0) {
                categorySubControllerList.get(categories).setTheme();
            }
        }
    }

    private void setDarkMode() {
        root.getStylesheets().clear();
        root.getStylesheets().add(Objects.requireNonNull(StageManager.class.getResource("styles/themes/dark/ServerView.css")).toExternalForm());
        if (chatViewController != null) {
            chatViewController.setTheme();
        }
        for (Categories categories : server.getCategories()) {
            if (categorySubControllerList.size() != 0) {
                categorySubControllerList.get(categories).setTheme();
            }
        }
    }

    public String getTheme() {
        return builder.getTheme();
    }
}
