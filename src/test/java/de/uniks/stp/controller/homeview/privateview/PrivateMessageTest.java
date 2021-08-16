package de.uniks.stp.controller.homeview.privateview;

import de.uniks.stp.StageManager;
import de.uniks.stp.builder.ModelBuilder;
import de.uniks.stp.model.PrivateChat;
import de.uniks.stp.model.User;
import de.uniks.stp.net.RestClient;
import de.uniks.stp.net.websocket.privatesocket.PrivateChatWebSocket;
import de.uniks.stp.net.websocket.privatesocket.PrivateSystemWebSocketClient;
import de.uniks.stp.net.websocket.serversocket.ServerChatWebSocket;
import de.uniks.stp.net.websocket.serversocket.ServerSystemWebSocket;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import kong.unirest.Callback;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import javax.json.JsonObject;
import java.io.File;
import java.io.IOException;

import static de.uniks.stp.util.Constants.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PrivateMessageTest extends ApplicationTest {

    @InjectMocks
    StageManager mockApp = new StageManager();
    @Mock
    private RestClient restClient;
    @Mock
    private PrivateSystemWebSocketClient privateSystemWebSocketClient;
    @Mock
    private PrivateChatWebSocket privateChatWebSocket;
    @Mock
    private ServerSystemWebSocket serverSystemWebSocket;
    @Mock
    private ServerChatWebSocket serverChatWebSocket;
    @Mock
    private HttpResponse<JsonNode> response;
    @Mock
    private HttpResponse<JsonNode> response2;
    @Mock
    private HttpResponse<JsonNode> response3;
    @Mock
    private HttpResponse<JsonNode> response4;
    @Mock
    private HttpResponse<JsonNode> response5;
    @Captor
    private ArgumentCaptor<Callback<JsonNode>> callbackCaptor;
    @Captor
    private ArgumentCaptor<Callback<JsonNode>> callbackCaptor2;
    @Captor
    private ArgumentCaptor<Callback<JsonNode>> callbackCaptor3;
    @Captor
    private ArgumentCaptor<Callback<JsonNode>> callbackCaptor4;
    @Captor
    private ArgumentCaptor<Callback<JsonNode>> callbackCaptor5;

    @BeforeClass
    public static void setupHeadlessMode() {
        System.setProperty("testfx.robot", "glass");
        System.setProperty("testfx.headless", "true");
        System.setProperty("headless.geometry", "1920x1080-32");
    }

    @BeforeAll
    static void setup() {
        MockitoAnnotations.openMocks(PrivateMessageTest.class);
    }

    @After
    public void cleanup() {
        mockApp.cleanEmojis();
    }

    @Override
    public void start(Stage stage) {
        //start application
        ModelBuilder builder = new ModelBuilder();
        builder.setUSER_CLIENT(privateSystemWebSocketClient);
        builder.setPrivateChatWebSocketClient(privateChatWebSocket);
        builder.setSERVER_USER(serverSystemWebSocket);
        builder.setServerChatWebSocketClient(serverChatWebSocket);
        StageManager app = mockApp;
        app.setBuilder(builder);
        app.setRestClient(restClient);

        builder.setLoadUserData(false);
        mockApp.getBuilder().setSpotifyShow(false);
        mockApp.getBuilder().setSpotifyToken(null);
        mockApp.getBuilder().setSpotifyRefresh(null);

        app.start(stage);
        stage.centerOnScreen();
    }

    public void mockGetServers() {
        JSONObject jsonString = new JSONObject()
                .put("status", "success")
                .put("message", "")
                .put("data", new JSONArray().put(new JSONObject().put("id", "5e2fbd8770dd077d03df505").put("name", "TestServer Team Bit Shift")));
        String jsonNode = new JsonNode(jsonString.toString()).toString();
        when(response2.getBody()).thenReturn(new JsonNode(jsonNode));
        doAnswer((Answer<Void>) invocation -> {
            Callback<JsonNode> callback = callbackCaptor2.getValue();
            callback.completed(response2);
            mockGetServerUser();
            return null;
        }).when(restClient).getServers(anyString(), callbackCaptor2.capture());
    }

    public void mockGetServerUser() {
        JSONArray members = new JSONArray();
        JSONArray categories = new JSONArray();
        categories.put("60b77ba0026b3534ca5a61ae");
        JSONObject member = new JSONObject();
        member.put("id", "60ad230ac77d3f78988b3e5b")
                .put("name", "Peter Lustig")
                .put("online", true).put("description", "");
        members.put(member);
        JSONObject jsonString = new JSONObject()
                .put("status", "success")
                .put("message", "")
                .put("data", new JSONObject()
                        .put("id", "5e2fbd8770dd077d03df505")
                        .put("name", "asdfasdf")
                        .put("owner", "60ad230ac77d3f78988b3e5b")
                        .put("categories", categories)
                        .put("members", members)
                );
        String jsonNode = new JsonNode(jsonString.toString()).toString();
        when(response3.getBody()).thenReturn(new JsonNode(jsonNode));
        doAnswer((Answer<Void>) invocation -> {
            Callback<JsonNode> callback = callbackCaptor3.getValue();
            callback.completed(response3);
            mockGetCategories();
            return null;
        }).when(restClient).getServerUsers(anyString(), anyString(), callbackCaptor3.capture());
    }

    public void mockGetCategories() {
        JSONArray channels = new JSONArray();
        channels.put("60b77ba0026b3534ca5a61af");
        JSONArray data = new JSONArray();
        data.put(new JSONObject()
                .put("id", "60b77ba0026b3534ca5a61ae")
                .put("name", "default")
                .put("server", "5e2fbd8770dd077d03df505")
                .put("channels", channels));
        JSONObject jsonString = new JSONObject()
                .put("status", "success")
                .put("message", "")
                .put("data", data);
        String jsonNode = new JsonNode(jsonString.toString()).toString();
        when(response4.getBody()).thenReturn(new JsonNode(jsonNode));
        doAnswer((Answer<Void>) invocation -> {
            Callback<JsonNode> callback = callbackCaptor4.getValue();
            callback.completed(response4);
            return null;
        }).when(restClient).getServerCategories(anyString(), anyString(), callbackCaptor4.capture());
    }

    public void mockGetChannels() {
        JSONArray members = new JSONArray();
        JSONArray audioMembers = new JSONArray();
        JSONArray data = new JSONArray();
        data.put(new JSONObject()
                .put("id", "60b77ba0026b3534ca5a61af")
                .put("name", "testChannel")
                .put("type", "text")
                .put("privileged", false)
                .put("category", "60b77ba0026b3534ca5a61ae")
                .put("members", members)
                .put("audioMembers", audioMembers));
        JSONObject jsonString = new JSONObject()
                .put("status", "success")
                .put("message", "")
                .put("data", data);
        String jsonNode = new JsonNode(jsonString.toString()).toString();
        when(response5.getBody()).thenReturn(new JsonNode(jsonNode));
        doAnswer((Answer<Void>) invocation -> {
            Callback<JsonNode> callback = callbackCaptor5.getValue();
            callback.completed(response5);
            return null;
        }).when(restClient).getCategoryChannels(anyString(), anyString(), anyString(), callbackCaptor5.capture());
    }


    public void loginInit() throws InterruptedException, IOException {
        FileUtils.deleteDirectory(new File(APPDIR_ACCORD_PATH + SAVES_PATH + PRIVATE_CHAT_PATH));

        doCallRealMethod().when(privateSystemWebSocketClient).handleMessage(any());
        doCallRealMethod().when(privateSystemWebSocketClient).setBuilder(any());
        doCallRealMethod().when(privateSystemWebSocketClient).setPrivateViewController(any());
        doCallRealMethod().when(privateChatWebSocket).handleMessage(any());
        doCallRealMethod().when(privateChatWebSocket).setBuilder(any());
        doCallRealMethod().when(privateChatWebSocket).setChatViewController(any());
        doCallRealMethod().when(privateChatWebSocket).setPrivateViewController(any());
        doCallRealMethod().when(serverChatWebSocket).handleMessage(any());
        doCallRealMethod().when(serverChatWebSocket).setBuilder(any());
        doCallRealMethod().when(serverChatWebSocket).setServerViewController(any());
        mockGetServers();
        mockGetServerUser();
        mockGetCategories();
        mockGetChannels();

        JSONObject jsonString = new JSONObject()
                .put("status", "success")
                .put("name", "Peter")
                .put("password", "1234")
                .put("data", new JSONObject().put("userKey", "c3a981d1-d0a2-47fd-ad60-46c7754d9271"));
        String jsonNode = new JsonNode(jsonString.toString()).toString();
        when(response.getBody()).thenReturn(new JsonNode(jsonNode));
        doAnswer((Answer<Void>) invocation -> {
            Callback<JsonNode> callback = callbackCaptor.getValue();
            callback.completed(response);
            return null;
        }).when(restClient).login(anyString(), anyString(), callbackCaptor.capture());
        TextField usernameTextField = lookup("#usernameTextfield").query();
        usernameTextField.setText("Peter");
        PasswordField passwordField = lookup("#passwordTextField").query();
        passwordField.setText("1234");
        clickOn("#loginButton");

        String message = "{\"action\":\"userJoined\",\"data\":{\"id\":\"60c8b3fb44453702009c07b3\",\"name\":\"Gustav\"}}";
        JsonObject jsonObject = (JsonObject) org.glassfish.json.JsonUtil.toJson(message);
        privateSystemWebSocketClient.handleMessage(jsonObject);

        message = "{\"channel\":\"private\",\"to\":\"Mr. Poopybutthole\",\"message\":\"Hallo\",\"from\":\"Allyria Dayne\",\"timestamp\":1623805070036}\"";
        jsonObject = (JsonObject) org.glassfish.json.JsonUtil.toJson(message);
        privateChatWebSocket.handleMessage(jsonObject);
    }

    @Test
    public void testMessageHandling() throws InterruptedException, IOException {
        doCallRealMethod().when(privateSystemWebSocketClient).handleMessage(any());
        doCallRealMethod().when(privateSystemWebSocketClient).setBuilder(any());
        doCallRealMethod().when(privateSystemWebSocketClient).setPrivateViewController(any());

        loginInit();
        WaitForAsyncUtils.waitForFxEvents();

        mockApp.getBuilder().setSpotifyShow(false);

        ListView<User> userList = lookup("#onlineUsers").query();
        User testUserOne = userList.getItems().get(0);
        doubleClickOn(userList.lookup("#" + testUserOne.getId()));
        WaitForAsyncUtils.waitForFxEvents();

        VBox privateChatCell = lookup("#cell_" + testUserOne.getId()).query();
        doubleClickOn(privateChatCell);
        TextField textField = lookup("#messageTextField").query();
        String msg1 = "Moin Gusti altes Haus!";
        textField.setText(msg1);
        clickOn("#sendButton");

        JSONObject message = new JSONObject().put("channel", "private").put("timestamp", 942351123).put("message", msg1).put("from", "Peter").put("to", "Gustav");
        JsonObject jsonObject = (JsonObject) org.glassfish.json.JsonUtil.toJson(message.toString());
        privateChatWebSocket.handleMessage(jsonObject);

        String msg2 = ":wave:Moin Peter lange nix mehr gehoert von dir";
        message = new JSONObject().put("channel", "private").put("timestamp", 942351453).put("message", msg2).put("to", "Peter").put("from", "Gustav");
        jsonObject = (JsonObject) org.glassfish.json.JsonUtil.toJson(message.toString());
        privateChatWebSocket.handleMessage(jsonObject);
        WaitForAsyncUtils.waitForFxEvents();

        ScrollPane messageScrollPane = (ScrollPane) lookup("#messageScrollPane").query();
        VBox messageVBox = (VBox) messageScrollPane.getContent().lookup("#messageVBox");

        //Assert.assertEquals(2, messageList.getItems().size());
        //Assert.assertEquals(msg1, messageList.getItems().get(0).getMessage());
        //Assert.assertEquals(msg2, messageList.getItems().get(1).getMessage());

        ListView<PrivateChat> privateChat = lookup("#privateChatList").query();

        doubleClickOn("#cell_" + privateChat.getItems().get(0).getId());
        WaitForAsyncUtils.waitForFxEvents();
        messageVBox = (VBox) messageScrollPane.getContent().lookup("#messageVBox");

        //Assert.assertEquals("Hallo", messageList.getItems().get(0).getMessage());

        doubleClickOn("#cell_" + privateChat.getItems().get(1).getId());
        WaitForAsyncUtils.waitForFxEvents();
        messageVBox = (VBox) messageScrollPane.getContent().lookup("#messageVBox");

        //Assert.assertEquals(2, messageList.getItems().size());
        //Assert.assertEquals(msg1, messageList.getItems().get(0).getMessage());
        //Assert.assertEquals(msg2, messageList.getItems().get(1).getMessage());

        //Test onOnlineUserListClicked
        userList = lookup("#onlineUsers").query();
        testUserOne = userList.getItems().get(0);
        privateChat = lookup("#privateChatList").query();
        privateChat.getItems().get(1).setUnreadMessagesCounter(1);
        doubleClickOn(userList.lookup("#" + testUserOne.getId()));
        WaitForAsyncUtils.waitForFxEvents();

        rightClickOn("#userNameLabel");
        moveBy(0, 15);
        write("\n");
        WaitForAsyncUtils.waitForFxEvents();

        ContextMenu contextMenu = mockApp.getBuilder().getCurrentChatViewController().getContextMenu();
        Assert.assertEquals(3, contextMenu.getItems().size());
        Assert.assertTrue(contextMenu.getItems().get(0).isVisible());
        Assert.assertFalse(contextMenu.getItems().get(1).isVisible());
        Assert.assertFalse(contextMenu.getItems().get(2).isVisible());

        String msg3 = "*Moin* %Peter% <lange> \\!nix mehr gehoert von dir";
        message = new JSONObject().put("channel", "private").put("timestamp", 942351453).put("message", msg3).put("to", "Peter").put("from", "Gustav");
        jsonObject = (JsonObject) org.glassfish.json.JsonUtil.toJson(message.toString());
        privateChatWebSocket.handleMessage(jsonObject);
        WaitForAsyncUtils.waitForFxEvents();

        String msg4 = "###quoteInit###repliedToText###quoteMessage###sendMessage[###messageId###repliedToId][###timestamp####repliedTimestamp]###quoteStop###";
        message = new JSONObject().put("channel", "private").put("timestamp", 942351453).put("message", msg4).put("to", "Peter").put("from", "Gustav");
        jsonObject = (JsonObject) org.glassfish.json.JsonUtil.toJson(message.toString());
        privateChatWebSocket.handleMessage(jsonObject);
        WaitForAsyncUtils.waitForFxEvents();

        String msg5 = "!guess akflsjdflka";
        message = new JSONObject().put("channel", "private").put("timestamp", 942351453).put("message", msg5).put("to", "Peter").put("from", "Gustav");
        jsonObject = (JsonObject) org.glassfish.json.JsonUtil.toJson(message.toString());
        privateChatWebSocket.handleMessage(jsonObject);
        WaitForAsyncUtils.waitForFxEvents();

        String msg6 = "!choose paper";
        message = new JSONObject().put("channel", "private").put("timestamp", 942351453).put("message", msg6).put("to", "Peter").put("from", "Gustav");
        jsonObject = (JsonObject) org.glassfish.json.JsonUtil.toJson(message.toString());
        privateChatWebSocket.handleMessage(jsonObject);
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    public void testEmojiView() throws InterruptedException, IOException {
        doCallRealMethod().when(privateSystemWebSocketClient).handleMessage(any());
        doCallRealMethod().when(privateSystemWebSocketClient).setBuilder(any());
        doCallRealMethod().when(privateSystemWebSocketClient).setPrivateViewController(any());

        loginInit();
        WaitForAsyncUtils.waitForFxEvents();

        mockApp.getBuilder().setSpotifyShow(false);

        ListView<User> userList = lookup("#onlineUsers").query();
        User testUserOne = userList.getItems().get(0);
        doubleClickOn(userList.lookup("#" + testUserOne.getId()));
        WaitForAsyncUtils.waitForFxEvents();

        VBox privateChatCell = lookup("#cell_" + testUserOne.getId()).query();
        doubleClickOn(privateChatCell);
        WaitForAsyncUtils.waitForFxEvents();

        clickOn("#emojiButton");
        WaitForAsyncUtils.waitForFxEvents();

        moveBy(0, -55);
        clickOn();
        WaitForAsyncUtils.waitForFxEvents();

        TextField textField = lookup("#textField_search").query();
        String msg1 = ":dog:";
        String finalMsg = msg1;
        Platform.runLater(() -> textField.setText(finalMsg));
        WaitForAsyncUtils.waitForFxEvents();

        msg1 = "";
        String finalMsg1 = msg1;
        Platform.runLater(() -> textField.setText(finalMsg1));
        WaitForAsyncUtils.waitForFxEvents();
    }
}