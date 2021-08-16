package de.uniks.stp.util;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import de.uniks.stp.StageManager;
import de.uniks.stp.model.Message;
import de.uniks.stp.model.PrivateChat;
import de.uniks.stp.model.User;
import javafx.scene.image.Image;

import java.io.*;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static de.uniks.stp.util.Constants.*;

public class ResourceManager {

    private static String comboValue = "";

    /**
     * load highScore from file
     */
    public static int loadHighScore(String currentUserName) {
        int highScore = 0;

        // if file not exists - create and put highScore = 0
        try {
            if (!Files.isDirectory(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + SNAKE_PATH))) {
                Files.createDirectories(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + SNAKE_PATH));
            }
            if (!Files.exists(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + SNAKE_PATH + "/highscore_" + currentUserName + ".json"))) {
                Files.createFile(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + SNAKE_PATH + "/highscore_" + currentUserName + ".json"));
                BufferedWriter writer = Files.newBufferedWriter(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + SNAKE_PATH + "/highscore_" + currentUserName + ".json"));
                JsonObject obj = new JsonObject();
                obj.put("highScore", highScore);
                Jsoner.serialize(obj, writer);
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // load file and highScore
        try {
            Reader reader = Files.newBufferedReader(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + SNAKE_PATH + "/highscore_" + currentUserName + ".json"));
            JsonObject parser = (JsonObject) Jsoner.deserialize(reader);
            BigDecimal value = (BigDecimal) parser.get("highScore");
            highScore = value.intValue();
            reader.close();
        } catch (JsonException |
                IOException e) {
            e.printStackTrace();
        }

        return highScore;
    }


    /**
     * save highScore to file
     */
    public static void saveHighScore(String currentUserName, int highScore) {
        try {
            BufferedWriter writer = Files.newBufferedWriter(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + SNAKE_PATH + "/highscore_" + currentUserName + ".json"));
            JsonObject obj = new JsonObject();
            obj.put("highScore", highScore);

            Jsoner.serialize(obj, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * load snakeGameIcons from file
     */
    public static Image loadSnakeGameIcon(String image) {
        return new Image(Objects.requireNonNull(StageManager.class.getResource("controller/snake/" + image + ".png")).toString());
    }

    /**
     * load muteGame state from file
     */
    public static boolean loadMuteGameState(String currentUserName) {
        // if file not exists - create and put highScore = 0
        try {
            if (!Files.isDirectory(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + SNAKE_PATH))) {
                Files.createDirectories(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + SNAKE_PATH));
            }
            if (!Files.exists(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + SNAKE_PATH + "/muteSettings_" + currentUserName + ".json"))) {
                Files.createFile(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + SNAKE_PATH + "/muteSettings_" + currentUserName + ".json"));
                BufferedWriter writer = Files.newBufferedWriter(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + SNAKE_PATH + "/muteSettings_" + currentUserName + ".json"));
                JsonObject obj = new JsonObject();
                obj.put("isGameMute", false);
                Jsoner.serialize(obj, writer);
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // load file and highScore
        boolean isGameMute = false;
        try {
            Reader reader = Files.newBufferedReader(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + SNAKE_PATH + "/muteSettings_" + currentUserName + ".json"));
            JsonObject parser = (JsonObject) Jsoner.deserialize(reader);
            isGameMute = (boolean) parser.get("isGameMute");
            reader.close();

        } catch (JsonException |
                IOException e) {
            e.printStackTrace();
        }
        return isGameMute;
    }

    /**
     * save muteGame state to file
     */
    public static void saveMuteGameState(boolean isGameMute, String currentUserName) {
        try {
            if (!Files.isDirectory(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + SNAKE_PATH))) {
                Files.createDirectories(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + SNAKE_PATH));
            }
            BufferedWriter writer = Files.newBufferedWriter(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + SNAKE_PATH + "/muteSettings_" + currentUserName + ".json"));
            JsonObject obj = new JsonObject();
            obj.put("isGameMute", isGameMute);

            Jsoner.serialize(obj, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * save privateChat to file
     */
    public static void savePrivatChat(String currentUserName, String chatPartnerName, Message message) {
        try {
            if (!Files.isDirectory(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + PRIVATE_CHAT_PATH))) {
                Files.createDirectories(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + PRIVATE_CHAT_PATH));
            }

            JsonArray parser = new JsonArray();
            File f = new File(APPDIR_ACCORD_PATH + SAVES_PATH + PRIVATE_CHAT_PATH + "/chat_" + currentUserName + "_" + chatPartnerName + ".json");
            if (f.exists()) {
                Reader reader = Files.newBufferedReader(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + PRIVATE_CHAT_PATH + "/chat_" + currentUserName + "_" + chatPartnerName + ".json"));
                parser = (JsonArray) Jsoner.deserialize(reader);
            }
            BufferedWriter writer = Files.newBufferedWriter(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + PRIVATE_CHAT_PATH + "/chat_" + currentUserName + "_" + chatPartnerName + ".json"));


            JsonObject obj = new JsonObject();
            obj.put("currentUserName", message.getFrom());
            obj.put("chatPartnerName", chatPartnerName);
            obj.put("message", message.getMessage());
            obj.put("timestamp", message.getTimestamp());
            parser.add(obj);

            Jsoner.serialize(parser, writer);
            writer.close();
        } catch (IOException | JsonException e) {
            e.printStackTrace();
        }
    }

    /**
     * load privateChat from file
     */
    public static ArrayList<Message> loadPrivatChat(String currentUserName, String chatPartnerName, PrivateChat privateChat) throws IOException, JsonException {
        ArrayList<Message> messageList = new ArrayList<>();

        File f = new File(APPDIR_ACCORD_PATH + SAVES_PATH + PRIVATE_CHAT_PATH + "/chat_" + currentUserName + "_" + chatPartnerName + ".json");
        if (f.exists()) {
            Reader reader = Files.newBufferedReader(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + PRIVATE_CHAT_PATH + "/chat_" + currentUserName + "_" + chatPartnerName + ".json"));
            messageList = loadPrivateMessages(reader, privateChat);
        }
        return messageList;
    }

    private static ArrayList<Message> loadPrivateMessages(Reader reader, PrivateChat privateChat) throws JsonException {
        ArrayList<Message> messageList = new ArrayList<>();
        JsonArray parser = (JsonArray) Jsoner.deserialize(reader);
        for (Object jsonObject : parser) {
            Message message = new Message();
            JsonObject jsonObject1 = (JsonObject) jsonObject;
            message.setMessage((String) jsonObject1.get("message"));
            message.setFrom((String) jsonObject1.get("currentUserName"));
            message.setPrivateChat(privateChat);
            message.setTimestamp(((BigDecimal) jsonObject1.get("timestamp")).longValue());
            messageList.add(message);
        }
        return messageList;
    }

    /**
     * save blockedUsers to file
     */
    public static void saveBlockedUsers(String currentUserName, User user, boolean blocking) {
        try {
            if (!Files.isDirectory(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + BLOCKEDUSERS_PATH))) {
                Files.createDirectories(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + BLOCKEDUSERS_PATH));
            }

            JsonArray parser = new JsonArray();
            File f = new File(APPDIR_ACCORD_PATH + SAVES_PATH + BLOCKEDUSERS_PATH + "/user_" + currentUserName + ".json");
            if (f.exists()) {
                Reader reader = Files.newBufferedReader(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + BLOCKEDUSERS_PATH + "/user_" + currentUserName + ".json"));
                parser = (JsonArray) Jsoner.deserialize(reader);
            }
            BufferedWriter writer = Files.newBufferedWriter(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + BLOCKEDUSERS_PATH + "/user_" + currentUserName + ".json"));


            JsonObject obj = new JsonObject();
            obj.put("id", user.getId());
            obj.put("name", user.getName());
            if (blocking) {
                parser.add(obj);
            } else {
                parser.remove(obj);
            }
            Jsoner.serialize(parser, writer);
            writer.close();
        } catch (IOException | JsonException e) {
            e.printStackTrace();
        }
    }

    /**
     * load blockedUsers from file
     */
    public static List<User> loadBlockedUsers(String currentUserName) throws IOException, JsonException {
        JsonArray parser;
        List<User> userList = new ArrayList<>();

        File f = new File(APPDIR_ACCORD_PATH + SAVES_PATH + BLOCKEDUSERS_PATH + "/user_" + currentUserName + ".json");
        if (f.exists()) {
            Reader reader = Files.newBufferedReader(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + BLOCKEDUSERS_PATH + "/user_" + currentUserName + ".json"));
            parser = (JsonArray) Jsoner.deserialize(reader);
            for (Object jsonObject : parser) {
                User user = new User();
                JsonObject jsonObject1 = (JsonObject) jsonObject;
                user.setId((String) jsonObject1.get("id"));
                user.setName((String) jsonObject1.get("name"));
                userList.add(user);
            }
        }
        return userList;
    }

    public static void extractEmojis() {
        try {
            if (!Files.isDirectory(Path.of(APPDIR_ACCORD_PATH + TEMP_PATH + EMOJIS_PATH))) {
                Files.createDirectories(Path.of(APPDIR_ACCORD_PATH + TEMP_PATH + EMOJIS_PATH));

                URL zipFileURL = Thread.currentThread().getContextClassLoader().getResource("de/uniks/stp/emojis/emojitwo.zip");
                InputStream inputStream = Objects.requireNonNull(zipFileURL).openStream();
                ZipInputStream zipInputStream = new ZipInputStream(inputStream);
                ZipEntry entry = zipInputStream.getNextEntry();
                while (entry != null) {
                    String filePath = APPDIR_ACCORD_PATH + TEMP_PATH + EMOJIS_PATH + File.separator + entry.getName();

                    // if the entry is a file, extracts it
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
                    byte[] bytesIn = new byte[4096];
                    int read;
                    while ((read = zipInputStream.read(bytesIn)) != -1) {
                        bos.write(bytesIn, 0, read);
                    }
                    bos.close();

                    zipInputStream.closeEntry();
                    entry = zipInputStream.getNextEntry();
                }
                zipInputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * save sound
     */

    public static void copyDefaultSound(InputStream inputStream) {
        try {
            if (!Files.exists(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + NOTIFICATION_PATH + "/default.wav")) && !Files.isDirectory(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + NOTIFICATION_PATH))) {
                Files.createDirectories(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + NOTIFICATION_PATH));
            }
            OutputStream outputStream = new FileOutputStream(APPDIR_ACCORD_PATH + SAVES_PATH + NOTIFICATION_PATH + "/default.wav");
            inputStream.transferTo(outputStream);
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void saveNotifications(File file) {
        try {
            String targetPath = APPDIR_ACCORD_PATH + SAVES_PATH + NOTIFICATION_PATH + "/" + file.getName();
            copyFile(file, targetPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * get sound
     */
    public static List<File> getNotificationSoundFiles() {
        List<File> listOfFiles = new ArrayList<>();
        if (Files.isDirectory(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + NOTIFICATION_PATH))) {
            File folder = new File(APPDIR_ACCORD_PATH + SAVES_PATH + NOTIFICATION_PATH);
            listOfFiles = new ArrayList<>(Arrays.asList(Objects.requireNonNull(folder.listFiles())));
            return listOfFiles;
        }
        return listOfFiles;
    }

    /**
     * delete sound
     */
    public static void deleteNotificationSound(String name) {
        if (Files.isDirectory(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + NOTIFICATION_PATH))) {
            File folder = new File(APPDIR_ACCORD_PATH + SAVES_PATH + NOTIFICATION_PATH);
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                checkForFileToDelete(file, name);
            }
        }
    }

    private static void checkForFileToDelete(File file, String name) {
        File deleteFile;
        String fileName = file.getName().substring(0, file.getName().length() - 4);
        if (fileName.equals(name)) {
            deleteFile = file;
            if (file.exists()) {
                deleteFile.delete();
            }
        }
    }

    /**
     * get value of comboBox
     */
    public static String getComboValue(String currentUserName) {
        comboValue = "";
        if (new File(APPDIR_ACCORD_PATH + SAVES_PATH + "/CurrentNotification/" + currentUserName + ".json").exists()) {
            try {
                Reader reader = Files.newBufferedReader(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + "/CurrentNotification/" + currentUserName + ".json"));
                JsonObject parser = (JsonObject) Jsoner.deserialize(reader);
                comboValue = (String) parser.get("fileName");
                reader.close();
            } catch (JsonException |
                    IOException e) {
                e.printStackTrace();
            }
        }
        return comboValue;
    }

    /**
     * set value of comboBox
     */
    public static void setComboValue(String userName, String comboValue) {
        ResourceManager.comboValue = comboValue;
        try {
            if (!Files.isDirectory(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + "/CurrentNotification/"))) {
                Files.createDirectories(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + "/CurrentNotification/"));
            }
            if (!Files.exists(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + "/CurrentNotification/" + comboValue + ".wav"))) {
                checkFiles(userName);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static void checkFiles(String userName) {
        for (File file : Objects.requireNonNull(new File(APPDIR_ACCORD_PATH + SAVES_PATH + NOTIFICATION_PATH).listFiles())) {
            if (file.getName().substring(0, file.getName().length() - 4).equals(comboValue)) {
                saveUserNameAndSound(userName, file.getName().substring(0, file.getName().length() - 4));
            }
        }
    }

    /**
     * copy file
     */
    public static void copyFile(File file, String targetPath) throws IOException, URISyntaxException {
        FileChannel source;
        if (file.getName().equals("default.wav")) {
            URL zipFileURL = Thread.currentThread().getContextClassLoader().getResource("de/uniks/stp/sounds/notification/default.wav");
            assert zipFileURL != null;
            Path path = Paths.get(zipFileURL.toURI());
            File file1 = path.toFile();
            source = new FileInputStream(file1).getChannel();
        } else {
            source = new FileInputStream(file).getChannel();
        }
        FileChannel destination = new FileOutputStream(targetPath).getChannel();
        destination.transferFrom(source, 0, source.size());
        source.close();
        destination.close();
    }

    /**
     * save fileName
     */
    public static void saveUserNameAndSound(String currentUserName, String fileName) {
        try {
            BufferedWriter writer = Files.newBufferedWriter(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + "/CurrentNotification/" + currentUserName + ".json"));
            JsonObject obj = new JsonObject();
            obj.put("fileName", fileName);
            Jsoner.serialize(obj, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * save volume of slider in jar file
     */
    public static void saveVolume(String currentUserName, Float volume) {
        try {
            if (!Files.isDirectory(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + "/Volume"))) {
                Files.createDirectories(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + "/Volume"));
            }
            BufferedWriter writer = Files.newBufferedWriter(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + "/Volume/" + currentUserName + ".json"));
            JsonObject obj = new JsonObject();
            obj.put("volume", volume);
            Jsoner.serialize(obj, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * get value of jar file
     */
    public static Float getVolume(String currentUserName) {
        float volume = 0.0f;
        if (Files.isReadable(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + "/Volume/" + currentUserName + ".json"))) {
            try {
                Reader reader = Files.newBufferedReader(Path.of(APPDIR_ACCORD_PATH + SAVES_PATH + "/Volume/" + currentUserName + ".json"));
                JsonObject parser = (JsonObject) Jsoner.deserialize(reader);
                volume = ((BigDecimal) parser.get("volume")).floatValue();
                reader.close();
            } catch (JsonException |
                    IOException e) {
                e.printStackTrace();
            }
        }
        return volume;
    }

    /**
     * checks the current Accord-version and handles in cases
     */
    public static void checkVersion() {
        try {
            // check if config file is available
            if (!Files.isDirectory(Path.of(APPDIR_ACCORD_PATH + CONFIG_PATH))) {
                // delete all
                Files.createDirectories(Path.of(APPDIR_ACCORD_PATH + CONFIG_PATH));

            }
            // write version file if not existing
            if (!Files.exists(Path.of(APPDIR_ACCORD_PATH + CONFIG_PATH + "/accord_version.txt"))) {
                Files.createFile(Path.of(APPDIR_ACCORD_PATH + CONFIG_PATH + "/accord_version.txt"));
                BufferedWriter versionWriter = Files.newBufferedWriter(Path.of(APPDIR_ACCORD_PATH + CONFIG_PATH + "/accord_version.txt"));
                JsonObject obj = new JsonObject();
                obj.put("version", ACCORD_VERSION_NR);
                Jsoner.serialize(obj, versionWriter);
                versionWriter.close();

                // delete settings.json because version was not existing (old version)
                Files.deleteIfExists(Path.of(APPDIR_ACCORD_PATH + CONFIG_PATH + "/settings.json"));
            }

            // check for correct version in file
            // if not correct, delete settings.json
            Reader versionReader = Files.newBufferedReader(Path.of(APPDIR_ACCORD_PATH + CONFIG_PATH + "/accord_version.txt"));
            JsonObject parser = (JsonObject) Jsoner.deserialize(versionReader);
            String loadedVersion = (String) parser.get("version");
            if (!loadedVersion.equals(ACCORD_VERSION_NR)) {
                Files.deleteIfExists(Path.of(APPDIR_ACCORD_PATH + CONFIG_PATH + "/settings.json"));
                BufferedWriter versionWriter = Files.newBufferedWriter(Path.of(APPDIR_ACCORD_PATH + CONFIG_PATH + "/accord_version.txt"));
                JsonObject obj = new JsonObject();
                obj.put("version", ACCORD_VERSION_NR);
                Jsoner.serialize(obj, versionWriter);
                versionWriter.close();
            }
            versionReader.close();
        } catch (IOException | JsonException e) {
            e.printStackTrace();
        }
    }
}
