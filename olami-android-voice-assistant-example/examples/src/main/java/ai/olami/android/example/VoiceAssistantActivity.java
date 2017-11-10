/*
	Copyright 2017, VIA Technologies, Inc. & OLAMI Team.

	http://olami.ai

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package ai.olami.android.example;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioRecord;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import ai.olami.android.IKeepRecordingSpeechRecognizerListener;
import ai.olami.android.KeepRecordingSpeechRecognizer;
import ai.olami.android.hotwordDetection.HotwordDetect;
import ai.olami.android.hotwordDetection.IHotwordDetectListener;
import ai.olami.android.tts.ITtsPlayerListener;
import ai.olami.android.tts.TtsPlayer;
import ai.olami.android.uart.IMicArrayControlVT6751Listener;
import ai.olami.android.uart.MicArrayControlVT6751;
import ai.olami.cloudService.APIConfiguration;
import ai.olami.cloudService.APIResponse;
import ai.olami.cloudService.APIResponseData;
import ai.olami.cloudService.SpeechResult;
import ai.olami.nli.DescObject;
import ai.olami.nli.NLIResult;

import static java.util.Locale.CHINA;
import static java.util.Locale.TAIWAN;

public class VoiceAssistantActivity extends AppCompatActivity {
    public final static String TAG = "OlamiVoiceKit";

    private static final int REQUEST_EXTERNAL_PERMISSION = 1;
    private static final int REQUEST_MICROPHONE = 3;

    Context mContext = null;

    KeepRecordingSpeechRecognizer mRecognizer = null;
    HotwordDetect mHotwordDetect = null;

    MicArrayControlVT6751 mMicArrayControlVT6751 = null;
    MicArrayLEDControlHelper mMicArrayLEDControlHelper = null;
    private String mSerialPortDevice = "/dev/ttyAMA0";
    private int mSerialPortBaudrate = 115200;

    private TextView mSTTText;
    private TextView mTTSValue;
    private TextView mWhatCanOLAMIDo;

    ImageView mOlamiLogo = null;
    AnimationDrawable mOlamiLogoAnimation;

    private boolean mIsPlayTTS = false;
    private boolean mIsSleepMode = false;

    TtsPlayerListener mTtsListener = null;
    TtsPlayer mTtsPlayer = null;
    AudioRecord mAudioRecord = null;

    private KeepRecordingSpeechRecognizer.RecognizeState mRecognizeState;

    /**
     * OLAMI Logo Animation State
     */
    public enum OlamiLogoAnimationState {
        BOOTING,
        WAITING,
        LISTENING,
        LISTENED,
        LOADING,
        WAKEUP_START,
        WAKEUP_WAITING_TO_TALK,
        WAKEUP_FINISH
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "checkPermission = "+ checkDeviceResourcePermissions());

        //
        // * Tell the DateTimeManagerService to sync NTP time.
        //   --------------------------------------------------------------------------------------
        //   Sometimes it can not be automatically synchronized in the AndroidThings platform,
        //   so we need to do this manually.
        //
        //   You may not need to do this step in the normal Android platform.
        //
        Intent inent = new Intent();
        inent.setComponent(new ComponentName("ai.olami.android.util",
                "ai.olami.android.util.DateTimeManagerService"));
        startService(inent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "checkPermission = "+ checkDeviceResourcePermissions());
        if (checkDeviceResourcePermissions()) {

            mContext = VoiceAssistantActivity.this.getApplicationContext();

            // Initial TTS player
            if (mTtsPlayer == null) {
                mTtsListener = new TtsPlayerListener();
                mTtsPlayer = new TtsPlayer(mContext, mTtsListener);
                mTtsPlayer.setSpeed(1.1f);
                mTtsPlayer.setVolume(100);
            }

            // Initial microphone array helper
            if (mMicArrayLEDControlHelper == null) {
                try {
                    mMicArrayControlVT6751 = MicArrayControlVT6751.create(
                            new MicArrayVT6751Listener(),
                            mContext,
                            mSerialPortDevice,
                            mSerialPortBaudrate);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mMicArrayLEDControlHelper = MicArrayLEDControlHelper.create(
                        mMicArrayControlVT6751);
            }

            //
            // * Check and load OLAMI developer's APP KEY and APP SECRET information.
            // ----------------------------------------------------------------------------------
            // You can :
            // 1. directly replace your APP KEY and APP SECRET into VoiceAssistantConfig.java,
            // 2. or put them into a text file on the path: /sdcard/olami-app-key.txt
            //
            // The 'olami-app-key.txt' content example:
            // locale=tw|cn
            // app-key=your-app-key
            // app-secret=your-app-secret
            //
            // e.g. (LOCALIZE_OPTION_TRADITIONAL_CHINESE):
            // locale=tw
            // app-key=123456789
            // app-secret=123456789
            //
            // e.g. (LOCALIZE_OPTION_SIMPLIFIED_CHINESE):
            // locale=cn
            // app-key=123456789
            // app-secret=123456789
            //
            while (true) {
                boolean hasAppKey = VoiceAssistantConfig.readOlamiAppKey();
                if (hasAppKey) {
                    // Set locale by localize option.
                    if (VoiceAssistantConfig.getLocalizeOption() == APIConfiguration
                            .LOCALIZE_OPTION_TRADITIONAL_CHINESE) {
                        switchLocale("taiwan");
                    } else if (VoiceAssistantConfig.getLocalizeOption() == APIConfiguration
                            .LOCALIZE_OPTION_SIMPLIFIED_CHINESE) {
                        switchLocale("china");
                    }

                    break;
                } else {
                    String TTSStr = getString(R.string.GetAppKeyFail);
                    mTtsPlayer.playText(TTSStr, false);
                    Log.i(TAG, TTSStr);

                    if (mMicArrayLEDControlHelper != null) {
                        mMicArrayLEDControlHelper.changeLEDState(
                                MicArrayLEDControlHelper.VoiceRecognitionState.ERROR);
                    }

                    sleep(15000);
                }
            }

            setContentView(R.layout.activity_speech_input_keeprecording);
            getSupportActionBar().hide();
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

            mOlamiLogo = (ImageView) findViewById(R.id.olamiLogo);
            mSTTText = (TextView) findViewById(R.id.sttResult);
            mTTSValue = (TextView) findViewById(R.id.ttsValue);
            mOlamiLogo = (ImageView) findViewById(R.id.olamiLogo);
            mWhatCanOLAMIDo = (TextView) findViewById(R.id.whatCanOLAMIDo);

            //
            // * Set Chinese fonts by localize option.
            //   --------------------------------------------------------------------------------------
            //   AndroidThings platform does not support Chinese fonts, so we need to do this manually.
            //
            //   You may not need to do this step in the normal Android platform.
            //
            Typeface customFont = null;
            if (VoiceAssistantConfig.getLocalizeOption() == APIConfiguration
                    .LOCALIZE_OPTION_TRADITIONAL_CHINESE) {
                customFont = Typeface.createFromAsset(getAssets(), "NotoSansTC-Light.otf");
            } else if (VoiceAssistantConfig.getLocalizeOption() == APIConfiguration
                    .LOCALIZE_OPTION_SIMPLIFIED_CHINESE) {
                customFont = Typeface.createFromAsset(getAssets(), "NotoSansCJKsc-Light.otf");
            }
            mSTTText.setTypeface(customFont);
            mTTSValue.setTypeface(customFont);
            mWhatCanOLAMIDo.setTypeface(customFont);

            mOlamiLogo.setOnClickListener(new recordButtonListener());

            init();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        release();
    }

    protected class recordButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (!mIsSleepMode) {
                startRecognize();
            }
        }
    }

    /**
     * Release all resources
     */
    private void release() {
        cancelRecognize();

        if (mHotwordDetect != null) {
            mHotwordDetect.stopDetection();
            mHotwordDetect.release();
            mHotwordDetect = null;
        }

        if (mRecognizer != null) {
            // * Release the recognizer when program stops or exits.
            mRecognizer.stopRecordingAndReleaseResources();
            mRecognizer = null;
        }

        if (mMicArrayLEDControlHelper != null) {
            try {
                mMicArrayLEDControlHelper.stopAndRelease();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            mMicArrayLEDControlHelper = null;
            mMicArrayControlVT6751 = null;
        }

        if (mTtsPlayer != null) {
            mTtsPlayer.destroy();
            mTtsPlayer = null;
        }
    }


    /**
     * Initial OLAMI services and related features.
     */
    private void init() {
        OlamiLogoChangeHandler(OlamiLogoAnimationState.BOOTING);

        new Thread(new Runnable() {
            public void run() {

                // * Step 1: Check platform and system environment, including network status.
                checkSystemEnvironment();

                // * Step 2: Configure your key and localize option.
                APIConfiguration config = new APIConfiguration(
                        VoiceAssistantConfig.getAppKey(),
                        VoiceAssistantConfig.getAppSecret(),
                        VoiceAssistantConfig.getLocalizeOption()
                );

                try {
                    // * Step 3: Create the microphone recording speech recognizer.
                    //           ----------------------------------------------------------
                    //           You should implement the IRecorderSpeechRecognizerListener
                    //           to get all callbacks and assign the instance of your
                    //           listener class into this recognizer.
                    if (mRecognizer == null) {
                        mRecognizer = KeepRecordingSpeechRecognizer.create(
                                new KeepRecordingSpeechRecognizerListener(),
                                config);

                        // * Optional steps: Setup some other configurations.
                        //                   You can use default settings without bellow steps.
                        mRecognizer.setEndUserIdentifier("Someone");
                        mRecognizer.setApiRequestTimeout(3000);

                        // * Advanced setting example.
                        //   These are also optional steps, so you can skip these
                        //   (or any one of these) to use default setting(s).
                        // ------------------------------------------------------------------
                        // * You can set the length of end time of the VAD in milliseconds
                        //   to stop voice recording automatically.
                        mRecognizer.setLengthOfVADEnd(2500);
                        // * You can set the frequency in milliseconds of the recognition
                        //   result query, then the recognizer client will query the result
                        //   once every milliseconds you set.
                        mRecognizer.setResultQueryFrequency(300);
                        // * You can set audio length in milliseconds to upload, then
                        //   the recognizer client will upload parts of audio once every
                        //   milliseconds you set.
                        mRecognizer.setSpeechUploadLength(300);
                        // * You can set the timeout of each recognize process (begin-to-end).
                        //   The recognize process will be cancelled if timeout and reset the state.
                        mRecognizer.setRecognizerTimeout(10000);
                        // ------------------------------------------------------------------

                        mRecognizer.startRecording();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // * Initial hotword detection and start detecting.
                initializeHotwordDetection();
                startHotwordDetection();

            }
        }).start();
    }

    /**
     * Initial hotword detection
     */
    private void initializeHotwordDetection() {

        if (mAudioRecord == null) {
            mAudioRecord = mRecognizer.getAudioRecord();
        }

        try {
            // * Create HotwordDetect instance by the specified AudioRecord object.
            //   ------------------------------------------------------------------------------
            //   You should implement the IHotwordDetectListener to get all callbacks
            //   and assign the instance of your listener class into HotwordDetect object.

            mHotwordDetect = HotwordDetect.create(
                    mAudioRecord,
                    this.getApplicationContext(),
                    new HotwordDetectListener()
            );
            mHotwordDetect.setResourceControlMode(HotwordDetect.RESOURCE_CONTROL_MODE_ALWAYS_ON);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Start hotword detection process
     */
    private void startHotwordDetection() {
        if (mHotwordDetect != null) {
            try {
                mHotwordDetect.startDetection();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            initializeHotwordDetection();
            startHotwordDetection();
        }
    }

    /**
     * Start recognizing user's speech input. (after awake)
     * This must be started after the state has been changed to STOPPED.
     */
    private void startRecognize() {
        if (mRecognizer != null && mHotwordDetect != null) {
            TTSChangeHandler(getString(R.string.canIHelpYou));
            STTChangeHandler("");

            OlamiLogoChangeHandler(OlamiLogoAnimationState.WAKEUP_WAITING_TO_TALK);

            if (mMicArrayLEDControlHelper != null) {
                mMicArrayLEDControlHelper.changeLEDState(
                        MicArrayLEDControlHelper.VoiceRecognitionState.PROCESSING);
            }

            // Get current voice recording state.
            mRecognizeState = mRecognizer.getRecognizeState();

            // * Check to see if speech recognition has been stopped, then we can do next run.
            if (mRecognizeState == KeepRecordingSpeechRecognizer.RecognizeState.STOPPED) {
                // * Request to start voice recording and recognition.
                try {
                    mHotwordDetect.stopDetection();
                    mRecognizer.startRecognizing();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Stop the speech recognition and then wait for the final recognition result.
     */
    private void stopRecognize() {
        if (mRecognizer != null) {
            mRecognizer.stopRecognizing();
        }
    }

    /**
     * Cancel the current speech recognition and go to the next run.
     */
    private void cancelRecognize() {
        if (mRecognizer != null) {
            mRecognizer.cancelRecognizing();

            if (mMicArrayLEDControlHelper != null) {
                mMicArrayLEDControlHelper.changeLEDState(
                        MicArrayLEDControlHelper.VoiceRecognitionState.WAITING);
            }

            OlamiLogoChangeHandler(OlamiLogoAnimationState.WAITING);

            startHotwordDetection();
        }
    }

    /**
     * Change to sleep mode or resume.
     * If it has been change to sleep mode, the system and voice recorder will still running,
     * but will not recognize anything.
     */
    private void sleepModeToggle() {
        if (!mIsSleepMode) {
            mIsSleepMode = true;
            mTtsPlayer.playText(getString(R.string.OlamiGoToSleep), false);

            TTSChangeHandler(getString(R.string.HowToWakeUp));
            STTChangeHandler("");

            if (mMicArrayLEDControlHelper != null) {
                mMicArrayLEDControlHelper.changeLEDState(
                        MicArrayLEDControlHelper.VoiceRecognitionState.SLEEPING);
            }

            mHotwordDetect.stopDetection();
        } else {
            mTtsPlayer.playText(getString(R.string.HowCanIHelpYou), false);
            TTSChangeHandler(getString(R.string.HelloIAmOlami));
            STTChangeHandler("");

            if (mMicArrayLEDControlHelper != null) {
                mMicArrayLEDControlHelper.changeLEDState(
                        MicArrayLEDControlHelper.VoiceRecognitionState.WAITING);
            }

            OlamiLogoChangeHandler(OlamiLogoAnimationState.WAITING);

            mIsSleepMode = false;

            startHotwordDetection();
        }
    }

    private class HotwordDetectListener implements IHotwordDetectListener {
        /**
         * Callback when the detect engine is initializing (not ready yet).
         */
        @Override
        public void onInitializing() {
            if (mMicArrayLEDControlHelper != null) {
                mMicArrayLEDControlHelper.changeLEDState(
                        MicArrayLEDControlHelper.VoiceRecognitionState.INITIALIZING);
            }
        }

        /**
         * Callback when the detect engine is initialized (ready for detection).
         */
        @Override
        public void onInitialized() {
            if (mMicArrayLEDControlHelper != null) {
                mMicArrayLEDControlHelper.changeLEDState(
                        MicArrayLEDControlHelper.VoiceRecognitionState.WAITING);
            }

            String str = getString(R.string.HelloIAmOlami);
            mTtsPlayer.playText(str, false);

            OlamiLogoChangeHandler(OlamiLogoAnimationState.WAITING);
        }

        /**
         * Callback when the detect engine is start detecting.
         *
         */
        @Override
        public void onStartDetect() {
            if (!mIsPlayTTS) {
                if (mMicArrayLEDControlHelper != null) {
                    mMicArrayLEDControlHelper.changeLEDState(
                            MicArrayLEDControlHelper.VoiceRecognitionState.WAITING);
                }

                OlamiLogoChangeHandler(OlamiLogoAnimationState.WAITING);
            }
        }

        /**
         * Callback when the specified hotword/wake-word has been detected.
         *
         * @param hotwordID - The specified hotword/wake-word.
         */
        @Override
        public void onHotwordDetect(int hotwordID) {

            // * Play TTS when hotword has been detected. (Optional)
            //   ---------------------------------------------------------------------------------
            //   You should disable this if you are not using the AEC (Acoustic Echo Cancelling)
            //   enabled device.
            //   Otherwise, the awakened response (TTS) will be also recognized as speech input.
            mTtsPlayer.playText("是", false);

            // * Start speech recognizing.
            startRecognize();
        }
    }

    /**
     * This is a callback listener example.
     *
     * You should implement the IMicArrayControlVT6751Listener
     * to get all callbacks and assign the instance of your listener class
     * into the instance of MicArrayControlVT6751.
     */
    private class MicArrayVT6751Listener implements IMicArrayControlVT6751Listener {
        /**
         * Callback when the physical button A has been clicked.
         */
        @Override
        public void onButtonAClick() {

            // In this example, we used button A to manually wake up the device
            // and start the speech recognition.
            if (!mIsSleepMode) {
                startRecognize();
            }
        }

        /**
         * Callback when the physical button B has been clicked.
         */
        @Override
        public void onButtonBClick() {

            // In this example, we used button B to manually stop the speech recognition.
            if (!mIsSleepMode) {
                stopRecognize();
            }
        }

        /**
         * Callback when the physical button C has been clicked.
         */
        @Override
        public void onButtonCClick() {

            // In this example, we used button C to manually cancel the speech recognition.
            if (!mIsSleepMode) {
                cancelRecognize();
            }
        }

        /**
         * Callback when the physical button D has been clicked.
         */
        @Override
        public void onButtonDClick() {

            // In this example, we used button D to manually toggle the device to sleep or resume.
            sleepModeToggle();
        }
    }

    /**
     * This is a callback listener example.
     *
     * You should implement the ITtsPlayerListener
     * to get all callbacks and assign the instance of your listener class
     * into the TTS player instance of TtsPlayer.
     */
    public class TtsPlayerListener implements ITtsPlayerListener {
        /**
         * Callback when the TTS playback is finished.
         *
         */
        @Override
        public void onPlayEnd() {
            mIsPlayTTS = false;

            if (mMicArrayLEDControlHelper != null) {
                mMicArrayLEDControlHelper.changeLEDState(
                        MicArrayLEDControlHelper.VoiceRecognitionState.WAITING);
            }

            OlamiLogoChangeHandler(OlamiLogoAnimationState.WAITING);
        }

        /**
         * Callback when the TTS playback is stopping.
         *
         */
        @Override
        public void onStop() {
            mIsPlayTTS = false;
        }

        /**
         * Callback when the TTS is playing.
         *
         * @param TTSString - The text being played.
         *
         */
        @Override
        public void onPlayingTTS(String TTSString) {
            mIsPlayTTS = true;

            if (mMicArrayLEDControlHelper != null) {
                mMicArrayLEDControlHelper.changeLEDState(
                        MicArrayLEDControlHelper.VoiceRecognitionState.SPEAKING);
            }

            OlamiLogoChangeHandler(OlamiLogoAnimationState.LOADING);

            TTSChangeHandler(TTSString);
        }
    }

    /**
     * This is a callback listener example.
     *
     * You should implement the IKeepRecordingSpeechRecognizerListener
     * to get all callbacks and assign the instance of your listener class
     * into the recognizer instance of KeepRecordingSpeechRecognizer.
     */
    private class KeepRecordingSpeechRecognizerListener implements IKeepRecordingSpeechRecognizerListener {
        // * Implement override method to get callback when the recognize
        //   process state changes.
        @Override
        public void onRecognizeStateChange(KeepRecordingSpeechRecognizer.RecognizeState state) {
            mRecognizeState = state;

            if (state == KeepRecordingSpeechRecognizer.RecognizeState.STOPPED) {

                startHotwordDetection();

                if (!mIsPlayTTS) {
                    if (mMicArrayLEDControlHelper != null) {
                        mMicArrayLEDControlHelper.changeLEDState(
                                MicArrayLEDControlHelper.VoiceRecognitionState.WAITING);
                    }

                    OlamiLogoChangeHandler(OlamiLogoAnimationState.WAITING);
                }
            } else if (state == KeepRecordingSpeechRecognizer.RecognizeState.PROCESSING) {

            } else if (state == KeepRecordingSpeechRecognizer.RecognizeState.COMPLETED) {

            } else if (state == KeepRecordingSpeechRecognizer.RecognizeState.ERROR) {
                errorStateHandler("Error!");
                startHotwordDetection();
            }
        }

        // * Implement override method to get callback when the results
        //   of speech recognition changes.
        @Override
        public void onRecognizeResultChange(APIResponse response) {
            // * Get recognition results.
            //   In this example, we only handle the speech-to-text result.
            Log.i(TAG, response.toString());

            try {
                SpeechResult sttResult = response.getData().getSpeechResult();
                if (sttResult.complete()) {
                    // 'complete() == true' means returned text is final result.
                    // --------------------------------------------------
                    // * It also means you can get NLI/IDS results if included.
                    //   So you can handle or process NLI/IDS results here ...
                    //
                    //   For example:
                    //   NLIResult[] nliResults = response.getData().getNLIResults();
                    //
                    // * See also :
                    //   - OLAMI Java Client SDK & Examples
                    //   - ai.olami.nli.NLIResult.
                    // --------------------------------------------------
                    STTChangeHandler(sttResult.getResult());

                    APIResponseData apiResponseData = response.getData();

                    NLIResult nliResults[] = apiResponseData.getNLIResults();
                    if (nliResults == null) {
                        mTtsPlayer.playText(getString(R.string.SayAgain), true);
                    } else {
                        for (int i = 0; i < nliResults.length; i++) {
                            if (nliResults[i].hasDataObjects()) {
                                String content = DumpIDSDataExample.dumpIDSData(nliResults[i]);
                                if (content != null) {
                                    mTtsPlayer.playText(content, true);
                                }
                            } else if (nliResults[i].hasDescObject()) {
                                DescObject nliDescObj = nliResults[i].getDescObject();
                                mTtsPlayer.playText(nliDescObj.getReplyAnswer(), true);
                            }
                        }
                    }
                } else {
                    // Recognition has not yet been completed.
                    // The text you get here is not a final result.
                    if (sttResult.getStatus() == SpeechResult.STATUS_RECOGNIZE_OK) {
                        STTChangeHandler(sttResult.getResult());
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                cancelRecognize();
            }
        }

        @Override
        public void onRecordVolumeChange(int volumeValue) {
            // Do something here when you get the changed volume.
            voiceVolumeChangeHandler(volumeValue);
        }

        // * Implement override method to get callback when server error occurs.
        @Override
        public void onServerError(APIResponse response) {
            Log.e(TAG, "Server error code: "+ response.getErrorCode()
                    +", Error message: " + response.getErrorMessage());
            errorStateHandler("onServerError Code: "+ response.getErrorCode());
        }

        // * Implement override method to get callback when error occurs.
        @Override
        public void onError(KeepRecordingSpeechRecognizer.Error error) {
            Log.e(TAG, "Error code:"+ error.name());
            errorStateHandler("KeepRecordingSpeechRecognizer.Error: "+ error.name());
        }

        // * Implement override method to get callback when exception occurs.
        @Override
        public void onException(Exception e) {
            e.printStackTrace();
            init();
        }
    }

    private void voiceVolumeChangeHandler(final int volume) {
        new Handler(this.getMainLooper()).post(new Runnable(){
            public void run(){
                if (volume >= 1) {
                    OlamiLogoChangeHandler(OlamiLogoAnimationState.LISTENING);
                }
            }
        });
    }

    private void OlamiLogoChangeHandler(final OlamiLogoAnimationState state) {
        new Handler(this.getMainLooper()).post(new Runnable(){
            public void run(){
                if (mOlamiLogoAnimation != null) {
                    if (mOlamiLogoAnimation.isRunning()) {
                        mOlamiLogoAnimation.stop();
                    }
                }

                if (state == OlamiLogoAnimationState.BOOTING) {
                    mOlamiLogo.setBackgroundResource(R.drawable.olami_booting);
                } else if (state == OlamiLogoAnimationState.WAITING) {
                    mOlamiLogo.setBackgroundResource(R.drawable.olami_waiting);
                } else if (state == OlamiLogoAnimationState.LISTENING) {
                    mOlamiLogo.setBackgroundResource(R.drawable.olami_listening);
                } else if (state == OlamiLogoAnimationState.LISTENED) {
                    mOlamiLogo.setBackgroundResource(R.drawable.olami_listened);
                } else if (state == OlamiLogoAnimationState.LOADING) {
                    mOlamiLogo.setBackgroundResource(R.drawable.olami_loading);
                } else if (state == OlamiLogoAnimationState.WAKEUP_START) {
                    mOlamiLogo.setBackgroundResource(R.drawable.olami_wakeupstart);
                } else if (state == OlamiLogoAnimationState.WAKEUP_WAITING_TO_TALK) {
                    mOlamiLogo.setBackgroundResource(R.drawable.olami_wakeupwaitingtotalk);
                } else if (state == OlamiLogoAnimationState.WAKEUP_FINISH) {
                    mOlamiLogo.setBackgroundResource(R.drawable.olami_wakeupfinish);
                }

                mOlamiLogoAnimation = (AnimationDrawable) mOlamiLogo.getBackground();
                mOlamiLogoAnimation.start();
            }
        });
    }

    private void STTChangeHandler(final String STTStr) {
        new Handler(this.getMainLooper()).post(new Runnable(){
            public void run(){
                mSTTText.setText(STTStr);
            }
        });
    }

    private void TTSChangeHandler(final String TTSString) {
        new Handler(this.getMainLooper()).post(new Runnable(){
            public void run(){
                mTTSValue.setText(TTSString);
            }
        });
    }

    private void errorStateHandler(final String errorString) {
        new Handler(this.getMainLooper()).post(new Runnable(){
            public void run(){
                Toast.makeText(getApplicationContext(),
                        errorString,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check platform and system environment.
     */
    private void checkSystemEnvironment() {
        final int delayTime = 15000;

        // Check network status.
        while (!isNetworkConnected()) {
            String TTSStr = getString(R.string.NetworkError);
            mTtsPlayer.playText(TTSStr, false);
            Log.i(TAG, TTSStr);

            if (mMicArrayLEDControlHelper != null) {
                mMicArrayLEDControlHelper.changeLEDState(
                        MicArrayLEDControlHelper.VoiceRecognitionState.ERROR);
            }

            sleep(delayTime);
        }

        if (mMicArrayLEDControlHelper != null) {
            mMicArrayLEDControlHelper.changeLEDState(
                    MicArrayLEDControlHelper.VoiceRecognitionState.INITIALIZING);
        }

        // Check with NTP time.
        checkSystemTime(0);

        // Test http connection
        while (!isConnectedToServer("http://olami.ai", 5000)) {
            String TTSStr = getString(R.string.ConnectNetworkError);
            mTtsPlayer.playText(TTSStr, false);
            Log.i(TAG, TTSStr);

            if (mMicArrayLEDControlHelper != null) {
                mMicArrayLEDControlHelper.changeLEDState(
                        MicArrayLEDControlHelper.VoiceRecognitionState.ERROR);
            }

            sleep(delayTime);
        }
    }

    private boolean isNetworkConnected(){
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    private boolean isConnectedToServer(String url, int timeout) {
        try{
            URL myUrl = new URL(url);
            URLConnection connection = myUrl.openConnection();
            connection.setConnectTimeout(timeout);
            connection.connect();
            return true;
        } catch (Exception e) {
            // Handle your exceptions
            return false;
        }
    }

    private void checkSystemTime(int ntpServerIndex) {
        mTtsPlayer.playText(getString(R.string.OlamiIsInitializing), false);

        try {
            // ntp server list
            String[] ntpServerList = {};
            if (VoiceAssistantConfig.getLocalizeOption()
                    == APIConfiguration.LOCALIZE_OPTION_TRADITIONAL_CHINESE) {
                ntpServerList = new String[]{
                        "time.stdtime.gov.tw",
                        "clock.stdtime.gov.tw",
                        "0.pool.ntp.org",
                        "1.pool.ntp.org",
                        "2.pool.ntp.org",
                        "3.pool.ntp.org",
                        "time.google.com",
                        "time1.google.com",
                        "time2.google.com"
                };

                if (ntpServerIndex > ntpServerList.length) {
                    ntpServerIndex = 0;
                }
            } else if (VoiceAssistantConfig.getLocalizeOption()
                    == APIConfiguration.LOCALIZE_OPTION_SIMPLIFIED_CHINESE) {
                ntpServerList = new String[]{
                        "0.cn.pool.ntp.org",
                        "1.cn.pool.ntp.org",
                        "2.cn.pool.ntp.org",
                        "3.cn.pool.ntp.org",
                        "0.pool.ntp.org",
                        "1.pool.ntp.org",
                        "2.pool.ntp.org",
                        "3.pool.ntp.org",
                        "ntp1.aliyun.com",
                        "ntp2.aliyun.com",
                        "ntp3.aliyun.com"
                };

                if (ntpServerIndex > ntpServerList.length) {
                    ntpServerIndex = 0;
                }
            }

            TimeZone zone = TimeZone.getTimeZone("GMT+8");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
            sdf.setTimeZone(zone);

            Log.i(TAG, "Get NTP server time from "+ ntpServerList[ntpServerIndex]);

            long returnNTPTime = getNTPServerTime(ntpServerList[ntpServerIndex])
                    .getMessage()
                    .getTransmitTimeStamp()
                    .getTime();

            String NTPServerCurrentYear = sdf.format(new Date(returnNTPTime));
            String deviceCurrentYear = sdf.format(new Date(System.currentTimeMillis()));
            Log.i(TAG, "NTPServerCurrentYear: "+ NTPServerCurrentYear +", deviceCurrentYear: "+ deviceCurrentYear);

            if (!deviceCurrentYear.equals(NTPServerCurrentYear)) {
                sleep(7000);
                checkSystemTime(++ntpServerIndex);
            }

        } catch (Exception ex) {
            Log.i(TAG, "Retry another NTP Server...");
            sleep(7000);
            checkSystemTime(++ntpServerIndex);
        }
    }

    private TimeInfo getNTPServerTime(String hostname) {
        NTPUDPClient timeClient;
        InetAddress inetAddress;
        TimeInfo timeInfo = null;

        try {
            timeClient = new NTPUDPClient();
            timeClient.setDefaultTimeout(1000);
            inetAddress = InetAddress.getByName(hostname);
            timeInfo = timeClient.getTime(inetAddress);
        } catch (UnknownHostException uhe) {
            uhe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return timeInfo;
    }

    private void switchLocale(String language) {
        Resources resources = getResources();
        android.content.res.Configuration config = resources.getConfiguration();
        DisplayMetrics dm = resources.getDisplayMetrics();
        if (language.equals("taiwan")) {
            config.setLocale(TAIWAN);
        } else {
            config.setLocale(CHINA);
        }
        mContext.getResources().updateConfiguration(config, dm);
    }

    /**
     * Check hardware resource permissions.
     */
    private boolean checkDeviceResourcePermissions() {
        // Check if the user agrees to access the microphone
        boolean hasMicrophonePermission = checkPermissions(
                Manifest.permission.RECORD_AUDIO,
                REQUEST_MICROPHONE);
        boolean hasWriteExternalStorage = checkPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                REQUEST_EXTERNAL_PERMISSION);

        if (hasMicrophonePermission && hasWriteExternalStorage) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check the specified hardware permission.
     */
    private boolean checkPermissions(String permissionStr, int requestCode) {
        // Check to see if we have permission to access something,
        // such like the microphone.
        int permission = ActivityCompat.checkSelfPermission(
                VoiceAssistantActivity.this,
                permissionStr);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We can not access it, request authorization from the user.
            ActivityCompat.requestPermissions(
                    VoiceAssistantActivity.this,
                    new String[] {permissionStr},
                    requestCode
            );
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_MICROPHONE:
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    int grantResult = grantResults[i];
                    if (permission.equals(Manifest.permission.RECORD_AUDIO)) {
                        if(grantResult == PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(
                                    this,
                                    getString(R.string.GetMicrophonePermission),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this,
                                    getString(R.string.GetMicrophonePermissionDenied),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
            case REQUEST_EXTERNAL_PERMISSION:
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    int grantResult = grantResults[i];
                    if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        if(grantResult == PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(
                                    this,
                                    getString(R.string.GetWriteStoragePermission),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this,
                                    getString(R.string.GetWriteStoragePermissionDenied),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
        }
    }
}
