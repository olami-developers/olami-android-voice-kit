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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import ai.olami.android.KeepRecordingSpeechRecognizer;

public class SettingDialogFragment extends DialogFragment {
    private static SettingDialogFragment mSettingDialogFragment = null;

    private View mSettingLayout;

    private EditText mSilenceLevelOfVadTailEdit, mRecognizerTimeoutEdit, mLengthOfVADEndEdit;
    private EditText mSpeechUploadLengthEdit, mApiRequestTimeoutEdit, mResultQueryFrequencyEdit;

    private AppCompatActivity mActivity;
    private KeepRecordingSpeechRecognizer mRecognizer;

    public static SettingDialogFragment newInstance() {
        if (mSettingDialogFragment == null) {
            mSettingDialogFragment = new SettingDialogFragment();
        }
        return mSettingDialogFragment;
    }

    public void setting(AppCompatActivity activity, KeepRecordingSpeechRecognizer recognizer) {
        mActivity = activity;
        mRecognizer = recognizer;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        mSettingLayout = (View) mActivity.getLayoutInflater()
                .inflate(R.layout.setting, null);

        mSilenceLevelOfVadTailEdit = (EditText) mSettingLayout.findViewById(R.id.silenceLevelOfVadTail);
        mRecognizerTimeoutEdit = (EditText) mSettingLayout.findViewById(R.id.RecognizerTimeout);
        mLengthOfVADEndEdit = (EditText) mSettingLayout.findViewById(R.id.LengthOfVADEnd);
        mSpeechUploadLengthEdit = (EditText) mSettingLayout.findViewById(R.id.SpeechUploadLength);
        mApiRequestTimeoutEdit = (EditText) mSettingLayout.findViewById(R.id.ApiRequestTimeout);
        mResultQueryFrequencyEdit = (EditText) mSettingLayout.findViewById(R.id.ResultQueryFrequency);

        setValueHander(mSilenceLevelOfVadTailEdit, VoiceAssistantConfig.getSilenceLevelOfVadTail() +"");
        setValueHander(mRecognizerTimeoutEdit, VoiceAssistantConfig.getRecognizerTimeout() +"");
        setValueHander(mLengthOfVADEndEdit, VoiceAssistantConfig.getLengthOfVADEnd() +"");
        setValueHander(mSpeechUploadLengthEdit, VoiceAssistantConfig.getSpeechUploadLength() +"");
        setValueHander(mApiRequestTimeoutEdit, VoiceAssistantConfig.getApiRequestTimeout() +"");
        setValueHander(mResultQueryFrequencyEdit, VoiceAssistantConfig.getResultQueryFrequency() +"");

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("Settings")
                .setView(mSettingLayout)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        int silenceLevelOfVadTail = Integer.valueOf(mSilenceLevelOfVadTailEdit.getText().toString());
                        int recognizerTimeout = Integer.valueOf(mRecognizerTimeoutEdit.getText().toString());
                        int lengthOfVADEnd = Integer.valueOf(mLengthOfVADEndEdit.getText().toString());
                        int speechUploadLength = Integer.valueOf(mSpeechUploadLengthEdit.getText().toString());
                        int apiRequestTimeout = Integer.valueOf(mApiRequestTimeoutEdit.getText().toString());
                        int resultQueryFrequency = Integer.valueOf(mResultQueryFrequencyEdit.getText().toString());

                        mRecognizer.setApiRequestTimeout(apiRequestTimeout);
                        mRecognizer.setLengthOfVADEnd(lengthOfVADEnd);
                        mRecognizer.setResultQueryFrequency(resultQueryFrequency);
                        mRecognizer.setSpeechUploadLength(speechUploadLength);
                        mRecognizer.setRecognizerTimeout(recognizerTimeout);
                        mRecognizer.setSilenceLevelOfVADTail(silenceLevelOfVadTail);

                        VoiceAssistantConfig.setSilenceLevelOfVadTail(silenceLevelOfVadTail);
                        VoiceAssistantConfig.setRecognizerTimeout(recognizerTimeout);
                        VoiceAssistantConfig.setLengthOfVADEnd(lengthOfVADEnd);
                        VoiceAssistantConfig.setSpeechUploadLength(speechUploadLength);
                        VoiceAssistantConfig.setApiRequestTimeout(apiRequestTimeout);
                        VoiceAssistantConfig.setResultQueryFrequency(resultQueryFrequency);
                        VoiceAssistantConfig.saveEnvironment();

                        Toast.makeText(mActivity, "Saved.", Toast.LENGTH_SHORT).show();

                        // Back to fullscreen.
                        mActivity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                    }
                });

        return builder.create();
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        // Back to fullscreen.
        mActivity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        try {
            manager.beginTransaction().remove(this).commit();
            super.show(manager, tag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setValueHander(final EditText edittext, final String value) {
        new Handler(mActivity.getMainLooper()).post(new Runnable(){
            public void run(){
                edittext.setText(value);
            }
        });
    }
}
