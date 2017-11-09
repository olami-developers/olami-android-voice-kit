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
import java.io.FileReader;
import java.io.IOException;

import ai.olami.cloudService.APIConfiguration;

public class VoiceAssistantConfig {

    public final static String TAG = "OlamiConfig";

    public static String mSdcardPath = Environment.getExternalStorageDirectory() +"/";

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
                if (str.length < 2) {
                    continue;
                }

                keyStr = str[0].toLowerCase().replaceAll(" ","");
                valStr = str[1];
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
            Log.e(TAG, "Can not read file: "+ mSdcardPath + keyFileName + "\n" + e.toString());
            e.printStackTrace();
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
}
