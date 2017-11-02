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

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;

import ai.olami.android.R;
import ai.olami.android.uart.IMicArrayControlVT6751Listener;
import ai.olami.android.uart.MicArrayControlVT6751;

public class VT6751Activity extends AppCompatActivity {

    MicArrayControlVT6751 mMicArrayControlVT6751 = null;
    MicArrayLEDControlHelper mMicrArrayLEDControlHelper = null;
    private String serialPortDevice = "/dev/ttyAMA0";
    private int serialPortBaudrate = 115200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // * Initial MicArrayControlVT6751 object.
        if (mMicrArrayLEDControlHelper == null) {
            try {
                mMicArrayControlVT6751 = MicArrayControlVT6751.create(
                        new MicArrayVT6751Listener(),
                        VT6751Activity.this,
                        serialPortDevice,
                        serialPortBaudrate);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Initial mMicArrayLEDControlHelper
            mMicrArrayLEDControlHelper = MicArrayLEDControlHelper.create(mMicArrayControlVT6751);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mMicrArrayLEDControlHelper != null) {
            try {
                mMicrArrayLEDControlHelper.stopAndRelease();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            mMicrArrayLEDControlHelper = null;
            mMicArrayControlVT6751 = null;
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
        @Override
        public void onButtonAClick() {
            mMicrArrayLEDControlHelper.changeLEDState(
                    MicArrayLEDControlHelper.VoiceRecognitionState.WAITING);
        }

        @Override
        public void onButtonBClick() {
            mMicrArrayLEDControlHelper.changeLEDState(
                    MicArrayLEDControlHelper.VoiceRecognitionState.ERROR);
        }

        @Override
        public void onButtonCClick() {
            mMicrArrayLEDControlHelper.changeLEDState(
                    MicArrayLEDControlHelper.VoiceRecognitionState.PROCESSING);
        }

        @Override
        public void onButtonDClick() {
            mMicArrayControlVT6751.TurnOffAllLED();
        }
    }
}
