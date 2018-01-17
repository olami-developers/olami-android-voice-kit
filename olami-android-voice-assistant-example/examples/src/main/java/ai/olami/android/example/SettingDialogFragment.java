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
    static private SettingDialogFragment mSettingDialogFragment = null;

    View mSettingLayout;

    EditText mSilenceLevelOfVadTailEdit, mRecognizerTimeoutEdit, mLengthOfVADEndEdit;
    EditText mSpeechUploadLengthEdit, mApiRequestTimeoutEdit, mResultQueryFrequencyEdit;

    AppCompatActivity mActivity;
    KeepRecordingSpeechRecognizer mRecognizer;

    static SettingDialogFragment newInstance() {
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
        builder.setTitle("設定")
                .setView(mSettingLayout)
                .setPositiveButton("儲存", new DialogInterface.OnClickListener() {
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

                        Toast.makeText(mActivity, "儲存成功！", Toast.LENGTH_SHORT).show();
                        // 恢復全螢幕
                        mActivity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                    }
                });

        return builder.create();
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        // 恢復全螢幕
        mActivity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        try {
            //在每个add事务前增加一个remove事务，防止连续的add
            manager.beginTransaction().remove(this).commit();
            super.show(manager, tag);
        } catch (Exception e) {
            //同一实例使用不同的tag会异常,这里捕获一下
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
