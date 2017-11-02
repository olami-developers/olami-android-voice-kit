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

package ai.olami.android.uart;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

import ai.olami.android.uart.serialport.serialPortReciver;
import ai.olami.android.uart.serialport.SerialPort;

public class MicArrayControlVT6751 {
    private static final String TAG = "MicArrayControlVT6751";

    public static final char CLOCKWISE = 82;
    public static final char COUNTERCLOCKWISE = 76;

    private static MicArrayControlVT6751 mMicArrayControlVT6751 = null;
    private IMicArrayControlVT6751Listener mArrayControlListener = null;

    private boolean mCancel = false;
    private boolean mIsReallyFinish = true;

    private Thread mSleepThread = null;
    private serialPortReciver mApplication;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private MicArrayControlVT6751(
            IMicArrayControlVT6751Listener listener,
            Context context,
            String device,
            int baudrate
    ) throws IOException {

        this.setListener(listener);
        SerialPort mSerialPort;
        mApplication = new serialPortReciver(context);
        try {
            mSerialPort = mApplication.getSerialPort(device,baudrate);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();

			// Create a receiving thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    MicArrayControlVT6751.this.readUartData();
                }
            }).start();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidParameterException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a MicArrayControlVT6751 instance.
     *
     * @param context - Android Context.
     * @param listener - The specified callback listener.
     * @return MicArrayControlVT6751 instance.
     */
    public static MicArrayControlVT6751 create(
            IMicArrayControlVT6751Listener listener,
            Context context,
            String device,
            int baudrate
    ) throws IOException {
        if(mMicArrayControlVT6751 == null) {
            mMicArrayControlVT6751 = new MicArrayControlVT6751(listener, context, device, baudrate);
        } else {
            mMicArrayControlVT6751.setListener(listener);
        }

        return mMicArrayControlVT6751;
    }

    /**
     * Set callback listener.
     *
     * @param listener The specified callback listener.
     */
    public void setListener(IMicArrayControlVT6751Listener listener) {
        this.mArrayControlListener = listener;
    }

    /**
     * Close UART conncetion to stop LED and buttons control.
     */
    public void closeUart() throws IOException {
        if(mApplication != null) {
            mApplication.closeSerialPort();
        }
    }

    /**
     * Cancel the running LED program.
     */
    public void cancelLEDControl() {
        this.mCancel = true;

        if (mSleepThread != null) {
            mSleepThread.interrupt();
        }

        while (!mIsReallyFinish) {
            sleep(1);
        }

        mCancel = false;
    }

    /**
     * Light behavior - Breathing with the specified color.
     *
     * @param R - Set RGB Red, 0~255.
     * @param G - Set RGB Green, 0~255.
     * @param B - Set RGB Blue, 0~255.
     * @param millisecondsPerTime - Breathing time in milliseconds.
     * @param maxLEDBrightness - Max LED brightness, 0~100.
     *
     */
    public void allLedFade(
            int R,
            int G,
            int B,
            int millisecondsPerTime,
            int maxLEDBrightness
    ) {
        this.cancelLEDControl();
        mIsReallyFinish = false;

        this.sleep(1000);
        this.setRGBToAllLED(4095, 'P', R, G, B);

        int i;
        for(i = 0; i < maxLEDBrightness && !this.mCancel; ++i) {
            this.setBrightnessToAllLED(i);
            this.sleep(millisecondsPerTime / (maxLEDBrightness * 2));
        }

        for(i = maxLEDBrightness; i >= 0 && !this.mCancel; --i) {
            this.setBrightnessToAllLED(i);
            this.sleep(millisecondsPerTime / (maxLEDBrightness * 2));
        }

        mIsReallyFinish = true;
    }

    /**
     * Light behavior - Rotation with the specified color.
     *
     * @param R - Set RGB Red, 0~255.
     * @param G - Set RGB Green, 0~255.
     * @param B - Set RGB Blue, 0~255.
     * @param millisecondsPerTime - Breathing time in milliseconds.
     * @param maxLEDBrightness - Max LED brightness, 0~100.
     * @param rotation - MicArrayControlVT6751.COUNTERCLOCKWISE for counterclockwise.
     *                   MicArrayControlVT6751.CLOCKWISE for clockwise.
     *
     */
    public void ledRotate(
            int R,
            int G,
            int B,
            int millisecondsPerTime,
            int maxLEDBrightness,
            char rotation
    ) {
        this.cancelLEDControl();
        mIsReallyFinish = false;

        this.setRGBToAllLED(4095, 'P', 0, 0, 0);
        this.setRGB(0, R, G, B);

        for(int i = 0; i < 12 && !this.mCancel; ++i) {
            this.setBrightnessToAllLED(maxLEDBrightness);
            this.sleep(millisecondsPerTime / 36);
            this.ledRotate(rotation);
        }

        mIsReallyFinish = true;
    }

    /**
     * Light behavior - Rotation.
     *
     * @param rotation - MicArrayControlVT6751.COUNTERCLOCKWISE for counterclockwise.
     *                   MicArrayControlVT6751.CLOCKWISE for clockwise.
     */
    public void ledRotate(char rotation) {
        byte[] buf = new byte[10];
        buf[0] = 5;
        buf[1] = (byte)rotation;
        this.sendCmds(buf, 2);
    }

    /**
     * Turn off all LEDs.
     */
    public void TurnOffAllLED() {
        this.setRGBToAllLED(4095, 'P', 0, 0, 0);
        this.setBrightnessToAllLED(0);
    }

    /**
     * Set RGB color to all LEDs.
     *
     * @param mask - Mask.
     * @param maskop - Mask Type.
     * @param R - Set RGB Red, 0~255.
     * @param G - Set RGB Green, 0~255.
     * @param B - Set RGB Blue, 0~255.
     *
     */
    public void setRGBToAllLED(int mask, char maskop, int r, int g, int b) {
        byte[] buf = new byte[]{2, (byte)maskop, (byte)(mask >> 8),
                (byte)(mask & 255), (byte)r, (byte)g, (byte)b, 0, 0, 0};
        this.sendCmds(buf, 7);
    }

    /**
     * Set RGB color to the specified LED.
     *
     * @param ledNumber - LED identify number.
     * @param R - Set RGB Red, 0~255.
     * @param G - Set RGB Green, 0~255.
     * @param B - Set RGB Blue, 0~255.
     *
     */
    public void setRGB(int ledNumber, int r, int g, int b) {
        byte[] buf = new byte[10];
        buf[0] = 3;
        buf[1] = (byte)ledNumber;
        buf[2] = (byte)r;
        buf[3] = (byte)g;
        buf[4] = (byte)b;
        this.sendCmds(buf, 5);
    }

    /**
     * set brightness to all LEDs
     *
     * @param brightness - Brightness, 0~100.
     *
     */
    public void setBrightnessToAllLED(int brightness) {
        byte[] buf = new byte[10];
        buf[0] = 4;
        buf[1] = (byte)brightness;
        this.sendCmds(buf, 2);
    }

    /**
     * Send command by UART.
     *
     * @param bff - Buffer
     * @param bfsz - Buffer Size
     *
     */
    public void sendCmds(byte[] bff, int bfsz) {
        int crc = 0;
        if(this.mOutputStream != null) {
            try {
                this.mOutputStream.write(bff, 0, bfsz);
                for(int var5 = 0; var5 < bfsz; ++var5) {
                    crc ^= unsignedToBytes(bff[var5]);
                }

                bff[0] = (byte)(crc & 255);
                this.mOutputStream.write(bff, 0, 1);
            } catch (IOException var51) {
                Log.w(TAG, "Unable to transfer data over UART(2)", var51);
            }
        }
    }


    private static String toHexadecimal(byte[] digest, int bsz) {
        String hash = "";
        byte[] var3 = digest;
        int var4 = digest.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            byte aux = var3[var5];
            if(bsz == 0) {
                break;
            }

            int b = aux & 255;
            if(b == 0) {
                break;
            }

            if(Integer.toHexString(b).length() == 1) {
                hash = hash + "0";
            }

            hash = hash + Integer.toHexString(b);
            --bsz;
        }

        return hash;
    }

    private void readUartData() {
        if(this.mInputStream != null) {
            try {
                byte[] var6 = new byte[512];

                int read;
                while(true) {
                    if (mInputStream != null) {
                        read = this.mInputStream.read(var6);
                        if (read <= 0) {
                            break;
                        }
                        try {
                            String uartInput = toHexadecimal(var6, read);
                            int arrayReturnCode = 0;
                            if (!uartInput.isEmpty()) {
                                arrayReturnCode = Integer.parseInt(toHexadecimal(var6, read));
                            }
                            switch (arrayReturnCode) {
                                case 41:
                                    this.mArrayControlListener.onButtonAClick();
                                    break;
                                case 42:
                                    this.mArrayControlListener.onButtonBClick();
                                    break;
                                case 43:
                                    this.mArrayControlListener.onButtonCClick();
                                    break;
                                case 44:
                                    this.mArrayControlListener.onButtonDClick();
                            }
                        } catch (NumberFormatException var5) {
                            var5.printStackTrace();
                        }
                    } else {
                        break;
                    }
                }
            } catch (IOException var61) {
                Log.e(TAG, "Unable to transfer data over UART", var61);
            }
        }
    }

    private static int unsignedToBytes(byte b) {
        return b & 255;
    }

    private void sleep(int milliseconds) {
        try {
            mSleepThread = new Thread();
            mSleepThread.sleep(milliseconds);
        } catch (InterruptedException var3) {
            var3.printStackTrace();
        }
    }
}
