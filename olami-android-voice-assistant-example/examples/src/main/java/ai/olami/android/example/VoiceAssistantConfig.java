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

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import ai.olami.android.KeepRecordingSpeechRecognizer;
import ai.olami.cloudService.APIConfiguration;

public class VoiceAssistantConfig {

    public final static String TAG = "OlamiConfig";

    public static String mSdcardPath = Environment.getExternalStorageDirectory() +"/";
    public static String mRecognizerEnvFilePath = "olami-assistant.env";

    // * Replace your APP KEY with this variable.
    private static String mAppKey = "e536d09357e046b1849e7029ffb8c68b";
    public static void setAppKey(String appKey) {
        mAppKey = appKey;
    }
    public static String getAppKey() {
        return mAppKey;
    }

    // * Replace your APP SECRET with this variable.
    private static String mAppSecret = "1c34468b0e8f42e3b575cc7f56732e04";
    public static void setAppSecret(String appSecret) {
        mAppSecret = appSecret;
    }
    public static String getAppSecret() {
        return mAppSecret;
    }

    // * Replace the localize option you want with this variable.
    // * - Use LOCALIZE_OPTION_SIMPLIFIED_CHINESE for China
    // * - Use LOCALIZE_OPTION_TRADITIONAL_CHINESE for Taiwan
    private static int mLocalizeOption = APIConfiguration.LOCALIZE_OPTION_SIMPLIFIED_CHINESE;
    //    private static int mLocalizeOption = APIConfiguration.LOCALIZE_OPTION_TRADITIONAL_CHINESE;
    public static void setLocalizeOption(int localizeOption) {
        mLocalizeOption = localizeOption;
    }
    public static int getLocalizeOption() {
        return mLocalizeOption;
    }


    private static int mSilenceLevelOfVadTail = 5;
    public static void setSilenceLevelOfVadTail(int silence) {
        mSilenceLevelOfVadTail = silence;
    }
    public static int getSilenceLevelOfVadTail() {
        return mSilenceLevelOfVadTail;
    }

    private static int mRecognizerTimeout = 10000;
    public static void setRecognizerTimeout(int timeout) {
        mRecognizerTimeout = timeout;
    }
    public static int getRecognizerTimeout() {
        return mRecognizerTimeout;
    }

    private static int mLengthOfVADEnd = 2000;
    public static void setLengthOfVADEnd(int length) {
        mLengthOfVADEnd = length;
    }
    public static int getLengthOfVADEnd() {
        return mLengthOfVADEnd;
    }

    private static int mSpeechUploadLength = 300;
    public static void setSpeechUploadLength(int length) {
        mSpeechUploadLength = length;
    }
    public static int getSpeechUploadLength() {
        return mSpeechUploadLength;
    }

    private static int mApiRequestTimeout = 3000;
    public static void setApiRequestTimeout(int timeout) {
        mApiRequestTimeout = timeout;
    }
    public static int getApiRequestTimeout() {
        return mApiRequestTimeout;
    }

    private static int mResultQueryFrequency = 100;
    public static void setResultQueryFrequency(int frequency) {
        mResultQueryFrequency = frequency;
    }
    public static int getResultQueryFrequency() {
        return mResultQueryFrequency;
    }

    private static String mEndUserIdentifier = "Some";
    public static void setEndUserIdentifier(String identifier) {
        mEndUserIdentifier = identifier;
    }
    public static String getEndUserIdentifier() {
        return mEndUserIdentifier;
    }

    public static boolean readOlamiAppKey() {
        if (!mAppSecret.startsWith("*")) {
            return true;
        }

        String keyFileName = "olami-app-key.txt";
        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(mSdcardPath + keyFileName));
            String line = null;
            String keyStr = null;
            String valStr = null;
            while ((line = bufferedReader.readLine()) != null) {
                String str[] = line.split("=");
                if (str.length != 2) {
                    continue;
                }

                keyStr = str[0].replaceAll(" ","");
                valStr = str[1].replaceAll(" ","");;
                if (keyStr.equals("app-key")) {
                    setAppKey(valStr);
                } else if (keyStr.equals("app-secret")) {
                    setAppSecret(valStr);
                } else if (keyStr.equals("locale")) {
                    switch (valStr.toLowerCase().replaceAll(" ","")) {
                        case "tw":
                            setLocalizeOption(APIConfiguration.LOCALIZE_OPTION_TRADITIONAL_CHINESE);
                            break;
                        case "cn":
                            setLocalizeOption(APIConfiguration.LOCALIZE_OPTION_SIMPLIFIED_CHINESE);
                            break;
                        default:
                            setLocalizeOption(APIConfiguration.LOCALIZE_OPTION_TRADITIONAL_CHINESE);
                            break;
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Can not read file: "+ mSdcardPath + keyFileName);
            return false;
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return true;
    }

    /*
    * Read recognizer settings from config file 'olami-assistant.env'.
    * */
    public static void readEnvironmentFile() {
        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(mSdcardPath + mRecognizerEnvFilePath));
            String line = null;
            String keyStr = null;
            String valStr = null;
            while ((line = bufferedReader.readLine()) != null) {
                String str[] = line.split("=");
                if (str.length != 2) {
                    continue;
                }

                keyStr = str[0].replaceAll(" ","");
                valStr = str[1].replaceAll(" ","");;

                switch (keyStr) {
                    case "EndUser_Identifier":
                        setEndUserIdentifier(valStr);
                        break;
                    case "Api_Request_Timeout":
                        setApiRequestTimeout(Integer.valueOf(valStr));
                        break;
                    case "Length_Of_VAD_End":
                        setLengthOfVADEnd(Integer.valueOf(valStr));
                        break;
                    case "Result_Query_Frequency":
                        setResultQueryFrequency(Integer.valueOf(valStr));
                        break;
                    case "Speech_Upload_Length":
                        setSpeechUploadLength(Integer.valueOf(valStr));
                        break;
                    case "Recognizer_Timeout":
                        setRecognizerTimeout(Integer.valueOf(valStr));
                        break;
                    case "Silence_Level_Of_VAD_Tail":
                        setSilenceLevelOfVadTail(Integer.valueOf(valStr));
                        break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Can not read file: "+ mSdcardPath + mRecognizerEnvFilePath);
            Log.i(TAG, "Create file: "+ mSdcardPath + mRecognizerEnvFilePath);
            saveEnvironment();
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /*
    * Save recognizer settings to config file 'olami-assistant.env'.
    * */
    public static void saveEnvironment() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Set the identification to identify the End-user.\n");
        sb.append("EndUser_Identifier=").append(getEndUserIdentifier()).append("\n");
        sb.append("# Set timeout in milliseconds of each HTTP API request.\n");
        sb.append("Api_Request_Timeout=").append(getApiRequestTimeout()).append("\n");
        sb.append("# Set length of end time of the VAD in milliseconds to stop voice recording automatically.\n");
        sb.append("Length_Of_VAD_End=").append(getLengthOfVADEnd()).append("\n");
        sb.append("# Set the frequency in milliseconds of the recognition result query.\n");
        sb.append("Result_Query_Frequency=").append(getResultQueryFrequency()).append("\n");
        sb.append("# Set audio length in milliseconds to upload.\n");
        sb.append("Speech_Upload_Length=").append(getSpeechUploadLength()).append("\n");
        sb.append("# Set timeout in milliseconds of each recognize process (begin-to-end).\n");
        sb.append("Recognizer_Timeout=").append(getRecognizerTimeout()).append("\n");
        sb.append("# Set level of silence volume of the VAD to stop voice recording automatically.\n");
        sb.append("Silence_Level_Of_VAD_Tail=").append(getSilenceLevelOfVadTail());

        try {
            FileOutputStream fos = new FileOutputStream(mSdcardPath + mRecognizerEnvFilePath);
            fos.write(sb.toString().getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
