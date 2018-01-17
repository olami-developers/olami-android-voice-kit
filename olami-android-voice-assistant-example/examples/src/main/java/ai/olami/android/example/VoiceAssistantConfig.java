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

    private static int mLengthOfVADEnd = 2500;
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

    private static int mResultQueryFrequency = 300;
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

    // * Setting localize option
    public static int SAMPLE_LOCALIZE_OPTION = APIConfiguration.LOCALIZE_OPTION_TRADITIONAL_CHINESE;

    // * Replace your APP KEY with this variable.
    private static String mAppKey = "*****your-app-key*****";
    public static void setAppKey(String appKey) {
        mAppKey = appKey;
    }
    public static String getAppKey() {
        return mAppKey;
    }

    // * Replace your APP SECRET with this variable.
    private static String mAppSecret = "*****your-app-secret*****";
    public static void setAppSecret(String appSecret) {
        mAppSecret = appSecret;
    }
    public static String getAppSecret() {
        return mAppSecret;
    }

    private static int mLocalizeOption = SAMPLE_LOCALIZE_OPTION;
    public static void setLocalizeOption(int localizeOption) {
        mLocalizeOption = localizeOption;
    }
    public static int getLocalizeOption() {
        return mLocalizeOption;
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
    * The 'olami-assistant.env' content example:
    *
    * # 設定使用者辨識碼
    * EndUser_Identifier=Someone
    * # 設定api伺服器請求的逾時時間，單位毫秒
    * Api_Request_Timeout=3000
    * # 設定自動停止錄音的結束時間，單位毫秒
    * Length_Of_VAD_End=2500
    * # 設定每次和伺服器請求的時間，單位毫秒
    * Result_Query_Frequency=300
    * # 設定每次上傳至伺服器，進行語音辨識的聲音長度，單位毫秒
    * Speech_Upload_Length=300
    * # 設定每次對話的逾時時間，單位毫秒
    * Recognizer_Timeout=10000
    * # 設定安靜音量的門檻值，單位音量大小
    * Silence_Level_Of_VAD_Tail=5
    *
    * */
    public static void readEnvironmentFile() {
       String envFilePath = "olami-assistant.env";
        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(mSdcardPath + envFilePath));
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
            Log.e(TAG, "Can not read file: "+ mSdcardPath + envFilePath);
            Log.i(TAG, "Create file: "+ mSdcardPath + envFilePath);
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

    public static void saveEnvironment() {
        String mSdcardPath = Environment.getExternalStorageDirectory() +"/";
        String envFilePath = "olami-assistant.env";

        String fileContent = "# 設定使用者辨識碼\n" +
                "EndUser_Identifier="+ getEndUserIdentifier() +"\n" +
                "# 設定api伺服器請求的逾時時間，單位毫秒\n" +
                "Api_Request_Timeout="+ getApiRequestTimeout() +"\n" +
                "# 設定自動停止錄音的結束時間，單位毫秒\n" +
                "Length_Of_VAD_End="+ getLengthOfVADEnd() +"\n" +
                "# 設定每次和伺服器請求的時間，單位毫秒\n" +
                "Result_Query_Frequency="+ getResultQueryFrequency() +"\n" +
                "# 設定每次上傳至伺服器，進行語音辨識的聲音長度，單位毫秒\n" +
                "Speech_Upload_Length="+ getSpeechUploadLength() +"\n" +
                "# 設定每次對話的逾時時間，單位毫秒\n" +
                "Recognizer_Timeout="+ getRecognizerTimeout() +"\n" +
                "# 設定安靜音量的門檻值，單位音量大小\n" +
                "Silence_Level_Of_VAD_Tail="+ getSilenceLevelOfVadTail();

        try {
            FileOutputStream fos = new FileOutputStream(mSdcardPath + envFilePath);
            fos.write(fileContent.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
