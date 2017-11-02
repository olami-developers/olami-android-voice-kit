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

package ai.olami.android.util;

import android.app.AlarmManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeManagerService extends Service {
    public final static String TAG = "DateTimeManagerService";

    public class LocalBinder extends Binder
    {
        DateTimeManagerService getService()
        {
            return  DateTimeManagerService.this;
        }
    }
    private LocalBinder mLocBin = new LocalBinder();

    @Override
    public IBinder onBind(Intent arg0)
    {
        // TODO Auto-generated method stub
        return mLocBin;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        // TODO Auto-generated method stub

        new Thread(new Runnable() {
            public void run() {
                // Initial time, check to see if auto set from the internet.
                checkDeviceTime(0);
                sleep(3000);
                // Show current time
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm yyyy");
                sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                String deviceCurrentTime = sdf.format(new Date(System.currentTimeMillis()));
                Log.e(TAG, "Time (Now):"+ deviceCurrentTime);
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        // TODO Auto-generated method stub
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        // TODO Auto-generated method stub
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        // TODO Auto-generated method stub
    }

    /**
     * Check system time and sync to NTP server
     *
     * @param ntpServerIndex - Server index number.
     */
    private void checkDeviceTime(int ntpServerIndex) {
        try {

            String[] ntpServerList = {};
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
                    "ntp3.aliyun.com",
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

            SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
            SimpleDateFormat sdfMonth = new SimpleDateFormat("MM");
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd");
            SimpleDateFormat sdfHour = new SimpleDateFormat("HH");
            SimpleDateFormat sdfMin = new SimpleDateFormat("mm");

            sdfHour.setTimeZone(TimeZone.getTimeZone("GMT+8"));

            Log.i(TAG, "Get NTP server time from "+ ntpServerList[ntpServerIndex]);

            long returnNTPTime = getNTPServerTime(ntpServerList[ntpServerIndex])
                    .getMessage()
                    .getTransmitTimeStamp()
                    .getTime();

            String NTPServerCurrentYear = sdfYear.format(new Date(returnNTPTime));
            String deviceCurrentYear = sdfYear.format(new Date(System.currentTimeMillis()));
            Log.i(TAG, "NTPServerCurrentYear: "+ NTPServerCurrentYear +", deviceCurrentYear: "+ deviceCurrentYear);

            Log.e(TAG, "sdfYear = "+ Integer.parseInt(sdfYear.format(new Date(returnNTPTime))));
            Log.e(TAG, "sdfMonth = "+ Integer.parseInt(sdfMonth.format(new Date(returnNTPTime))));
            Log.e(TAG, "sdfDate = "+ Integer.parseInt(sdfDate.format(new Date(returnNTPTime))));
            Log.e(TAG, "sdfHour = "+ Integer.parseInt(sdfHour.format(new Date(returnNTPTime))));
            Log.e(TAG, "sdfMin = "+ Integer.parseInt(sdfMin.format(new Date(returnNTPTime))));

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));
            calendar.set(
                    Integer.parseInt(sdfYear.format(new Date(returnNTPTime))),
                    Integer.parseInt(sdfMonth.format(new Date(returnNTPTime))) - 1, // 0 to 11
                    Integer.parseInt(sdfDate.format(new Date(returnNTPTime))),
                    Integer.parseInt(sdfHour.format(new Date(returnNTPTime))),
                    Integer.parseInt(sdfMin.format(new Date(returnNTPTime))),
                    0
            );
            AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            am.setTime(calendar.getTimeInMillis());
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.i(TAG, "Retry another NTP Server...");
            sleep(5000);
            checkDeviceTime(++ntpServerIndex);
        }
    }

    /**
     * Get NTP time
     *
     * @param hostname - NTP host name
     */
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

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
