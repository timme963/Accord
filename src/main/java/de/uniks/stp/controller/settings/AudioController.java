package de.uniks.stp.controller.settings;

import de.uniks.stp.builder.ModelBuilder;
import de.uniks.stp.net.udp.Microphone;
import de.uniks.stp.net.udp.Speaker;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class AudioController extends SubSetting {

    private final Parent view;
    private final ModelBuilder builder;
    private boolean senderActive;
    private volatile boolean stopped;
    private boolean isMuted;
    private Runnable myRunnable;
    private Thread soundThread;

    private ComboBox<String> inputDeviceComboBox;
    private ComboBox<String> outputDeviceComboBox;
    private Slider volumeInput;
    private Slider volumeOutput;
    private Button startButton;
    private ProgressBar microphoneProgressBar;
    private Microphone microphone;
    private Speaker speaker;


    public AudioController(Parent view, ModelBuilder builder) {
        this.view = view;
        this.builder = builder;
        this.builder.setAudioController(this);
    }

    public void setMicrophone(Microphone microphone) {
        this.microphone = microphone;
    }

    @SuppressWarnings("unchecked")
    public void init() {
        inputDeviceComboBox = (ComboBox<String>) view.lookup("#comboBox_input");
        outputDeviceComboBox = (ComboBox<String>) view.lookup("#comboBox_output");
        volumeInput = (Slider) view.lookup("#slider_volumeInput");
        volumeOutput = (Slider) view.lookup("#slider_volumeOutput");
        microphoneProgressBar = (ProgressBar) view.lookup("#progressBar_microphone");
        startButton = (Button) view.lookup("#button_audioStart");

        startButton.setOnAction(this::onMicrophoneTestStart);
        senderActive = false;
        stopped = true;
        myRunnable = this::runMicrophoneTest;


        // ComboBox Settings
        this.inputDeviceComboBox.setPromptText(builder.getLinePoolService().getSelectedMicrophoneName());
        this.inputDeviceComboBox.getItems().clear();
        this.inputDeviceComboBox.setOnAction(this::onInputDeviceClicked);

        // set microphone names
        for (var microphone : builder.getLinePoolService().getMicrophones().entrySet()) {
            this.inputDeviceComboBox.getItems().add(microphone.getKey());
        }

        this.outputDeviceComboBox.setPromptText(builder.getLinePoolService().getSelectedSpeakerName());
        this.outputDeviceComboBox.getItems().clear();
        this.outputDeviceComboBox.setOnAction(this::onOutputDeviceClicked);

        // set speaker names
        for (var speaker : builder.getLinePoolService().getSpeakers().entrySet()) {
            this.outputDeviceComboBox.getItems().add(speaker.getKey());
        }

        // Slider Settings
        // set values
        volumeInput.setValue(builder.getLinePoolService().getMicrophoneVolume());
        Text valueTextInputSlider = volumeInit(volumeInput);
        Pane thumbInputSlider = (Pane) volumeInput.lookup(".thumb");
        valueTextInputSlider.setText(String.valueOf((int) (volumeInput.getValue() * 100) + 50));
        thumbInputSlider.getChildren().add(valueTextInputSlider);

        // get new Value
        volumeInput.valueProperty().addListener((observable, oldValue, newValue) -> {
            builder.getLinePoolService().setMicrophoneVolume(newValue.floatValue());
            valueTextInputSlider.setText(String.valueOf((int) (volumeInput.getValue() * 100) + 50));
            builder.saveSettings();
        });

        // set values
        volumeOutput.setValue(builder.getLinePoolService().getSpeakerVolume());

        Text valueTextOutputSlider = volumeInit(volumeOutput);
        Pane thumbOutputSlider = (Pane) volumeOutput.lookup(".thumb");
        valueTextOutputSlider.setText(String.valueOf((int) (volumeOutput.getValue() * 100)));
        thumbOutputSlider.getChildren().add(valueTextOutputSlider);

        // get new Value
        volumeOutput.valueProperty().addListener((observable, oldValue, newValue) -> {
            builder.getLinePoolService().setSpeakerVolume(newValue.floatValue());
            valueTextOutputSlider.setText(String.valueOf((int) (volumeOutput.getValue() * 100)));
            builder.saveSettings();
        });
    }

    private Text volumeInit(Slider volumeXXXput) {
        volumeXXXput.setMin(0.0);
        volumeXXXput.setMax(1.0);

        // set thumb text & style
        volumeXXXput.applyCss();
        volumeXXXput.layout();
        Text valueTextXXXputSlider = new Text();
        if (builder.getTheme().equals("Dark")) {
            valueTextXXXputSlider.setFill(Color.BLACK);
        } else {
            valueTextXXXputSlider.setFill(Color.WHITE);
        }
        return valueTextXXXputSlider;
    }

    /**
     * Sets the selected InputDevice (Microphone) when clicked on it in comboBox
     */
    private void onInputDeviceClicked(ActionEvent actionEvent) {
        builder.getLinePoolService().setSelectedMicrophone(this.inputDeviceComboBox.getValue());
        builder.saveSettings();

        if (builder.getAudioStreamClient() != null) {
            builder.getAudioStreamClient().setNewMicrophone();
        }
        if (senderActive) {
            refreshDevice();
        }
    }

    public void refreshDevice() {
        stopRecord();
        microphone.init();
        speaker.init();
        senderActive = true;
        soundThread = new Thread(myRunnable);
        soundThread.start();
    }

    /**
     * Sets the selected OutputDevice (Speaker) when clicked on it in comboBox
     */
    private void onOutputDeviceClicked(ActionEvent actionEvent) {
        builder.getLinePoolService().setSelectedSpeaker(this.outputDeviceComboBox.getValue());
        builder.saveSettings();

        if (builder.getAudioStreamClient() != null) {
            builder.getAudioStreamClient().setNewSpeaker();
        }
        if (senderActive) {
            refreshDevice();
        }
    }

    /**
     * starts a new thread to test the audio input and output
     */
    private void onMicrophoneTestStart(ActionEvent actionEvent) {
        if (builder.getPersonalUser() != null) {
            if (builder.getMuteHeadphones()) {
                isMuted = true;
            } else {
                isMuted = false;
                builder.muteHeadphones(true);
                builder.muteMicrophone(true);
                handleMuteHeadphones();
            }
        }
        if (microphone == null) {
            microphone = new Microphone(this.builder);
        }
        microphone.init();
        speaker = new Speaker(this.builder);
        speaker.init();
        senderActive = true;
        soundThread = new Thread(myRunnable);
        soundThread.start();

        microphoneTestChangeAction(false);
    }

    /**
     * Stops the thread of the audio test and mutes users
     */
    private void onMicrophoneTestStop(ActionEvent actionEvent) {
        senderActive = false;
        if (builder.getPersonalUser() != null) {
            if (!isMuted) {
                builder.muteHeadphones(false);
                builder.muteMicrophone(false);
                handleMuteHeadphones();
            }
        }

        stopRecord();

        microphoneTestChangeAction(true);
    }

    public void handleMuteHeadphones() {
        if (builder.getMicrophoneFirstMuted()) {
            builder.muteMicrophone(true);
        }
        if (builder.getHandleMicrophoneHeadphone() != null) {
            builder.getHandleMicrophoneHeadphone().run();
        }
    }

    private void microphoneTestChangeAction(Boolean stopTest) {
        if (stopTest) {
            startButton.setText("Start");
            startButton.setOnAction(this::onMicrophoneTestStart);
        } else {
            startButton.setText("Stop");
            startButton.setOnAction(this::onMicrophoneTestStop);
        }
    }

    public void stopRecord() {
        senderActive = false;
        while (!stopped) {
            Thread.onSpinWait();
        }
    }

    public void stop() {
        stopRecord();
        startButton.setOnAction(null);
        soundThread = null;
        myRunnable = null;
    }

    public void runMicrophoneTest() {
        if (senderActive) {
            microphone.startRecording();
            speaker.startPlayback();
            while (senderActive) {

                stopped = false;
                byte[] data = microphone.readData();
                int volumeInPer = calculateRMSLevel(data);

                if (volumeInPer >= 0 && volumeInPer <= 35) {
                    microphoneProgressBar.setId("progressBar_microphone_low");
                } else if (volumeInPer > 35 && volumeInPer <= 65) {
                    microphoneProgressBar.setId("progressBar_microphone_medium");
                } else if (volumeInPer > 65) {
                    microphoneProgressBar.setId("progressBar_microphone_high");
                }

                microphoneProgressBar.setProgress(volumeInPer * 0.01);
                speaker.writeData(data);

            }
            microphoneProgressBar.setProgress(0);
            microphone.stopRecording();
            speaker.stopPlayback();
            stopped = true;
        }
    }

    public int calculateRMSLevel(byte[] audioData) {
        double lSum = 0;
        for (byte audioDatum : audioData) {
            lSum = lSum + audioDatum;
        }
        double dAvg = lSum / audioData.length;
        double sumMeanSquare = 0;

        for (byte audioDatum : audioData) {
            sumMeanSquare += Math.pow(audioDatum - dAvg, 2);
        }

        double averageMeanSquare = sumMeanSquare / audioData.length;

        return (int) (Math.sqrt(averageMeanSquare) + 0.5);
    }

}
