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

import java.io.IOException;

import ai.olami.android.uart.MicArrayControlVT6751;

public class MicArrayLEDControlHelper {

    private static MicArrayLEDControlHelper mMicrophoneArrayLEDStateHandle = null;
    private MicArrayControlVT6751 mMicArrayControlVT6751 = null;

    private VoiceRecognitionState mMicrophoneArrayLEDState = null;

    private LEDControlRunnable mLEDRunnable = null;

    private final int MaxLEDBrightness = 50;

    /**
     * Voice Recognition State. (Mapping to LED behaviors)
     *
     */
    public enum VoiceRecognitionState {
        SLEEPING,
        INITIALIZING,
        PROCESSING,
        WAITING,
        SPEAKING,
        ERROR
    }

    private MicArrayLEDControlHelper(
            MicArrayControlVT6751 micArrayControlVT6751
    ) {
        mMicArrayControlVT6751 = micArrayControlVT6751;
    }

    /**
     * Create a MicArrayLEDControlHelper instance.
     *
     * @param micArrayControlVT6751 - The MicArrayControlVT6751 instance reference.
     * @return MicArrayControlVT6751 instance.
     */
    static public MicArrayLEDControlHelper create(
            MicArrayControlVT6751 micArrayControlVT6751
    ) {
        if (mMicrophoneArrayLEDStateHandle == null) {
            mMicrophoneArrayLEDStateHandle = new MicArrayLEDControlHelper(micArrayControlVT6751);
        }
        return mMicrophoneArrayLEDStateHandle;
    }

    /**
     * Stop and release resource.
     */
    public void stopAndRelease() throws IOException {
        if (mLEDRunnable != null) {
            mLEDRunnable.terminate();
            mLEDRunnable = null;
        }

        if (mMicArrayControlVT6751 != null) {
            mMicArrayControlVT6751.closeUart();
        }
    }

    /**
     * Change state to change the LED behaviors.
     *
     * @param state - Voice Recognition State.
     *
     */
    public void changeLEDState(VoiceRecognitionState state) {
        if (mMicrophoneArrayLEDState != state) {
            mMicrophoneArrayLEDState = state;

            if (mLEDRunnable != null) {
                mLEDRunnable.terminate();
                mLEDRunnable = null;
            }
            mLEDRunnable = new LEDControlRunnable();
            new Thread(mLEDRunnable).start();
        }
    }

    private class LEDControlRunnable implements Runnable {
        private boolean mCancel = false;

        public void terminate() {
            mCancel = true;
        }

        public void run() {
            while(!mCancel) {
                if (mMicrophoneArrayLEDState == VoiceRecognitionState.INITIALIZING) {
                    mMicArrayControlVT6751.allLedFade(255, 165, 0, 3000, MaxLEDBrightness);
                } else if (mMicrophoneArrayLEDState == VoiceRecognitionState.WAITING) {
                    mMicArrayControlVT6751.allLedFade(160, 32, 240, 3000, MaxLEDBrightness);
                } else if (mMicrophoneArrayLEDState == VoiceRecognitionState.PROCESSING) {
                    mMicArrayControlVT6751.ledRotate(
                            0, 255, 0, 1500, MaxLEDBrightness, MicArrayControlVT6751.CLOCKWISE);
                } else if (mMicrophoneArrayLEDState == VoiceRecognitionState.SPEAKING) {
                    mMicArrayControlVT6751.ledRotate(
                            0, 0, 255, 1500, MaxLEDBrightness, MicArrayControlVT6751.COUNTERCLOCKWISE);
                } else if (mMicrophoneArrayLEDState == VoiceRecognitionState.ERROR) {
                    mMicArrayControlVT6751.allLedFade(255, 0, 0, 5000, MaxLEDBrightness);
                } else if (mMicrophoneArrayLEDState == VoiceRecognitionState.SLEEPING) {
                    mMicArrayControlVT6751.allLedFade(0, 255, 255, 5000, MaxLEDBrightness);
                }
            }
        }
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


