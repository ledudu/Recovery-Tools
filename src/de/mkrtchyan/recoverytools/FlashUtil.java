package de.mkrtchyan.recoverytools;

/*
 * Copyright (c) 2013 Ashot Mkrtchyan
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights 
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.sbstrm.appirater.Appirater;

import org.rootcommands.util.RootAccessDeniedException;

import java.io.File;

import de.mkrtchyan.utils.Common;
import de.mkrtchyan.utils.Notifyer;

public class FlashUtil extends AsyncTask<Void, Void, Boolean> {

    private Context mContext;
	private static final String TAG = "FlashUtil";
    private ProgressDialog pDialog;
    final private Common mCommon = new Common();
    private Notifyer mNotifyer;
    private final DeviceHandler mDeviceHandler;
	private File file;
    private int JOB;

    public FlashUtil(Context mContext, File file, int JOB) {
        this.mContext = mContext;
        this.file = file;
        this.JOB = JOB;
        mNotifyer = new Notifyer(mContext);
		mDeviceHandler = new DeviceHandler(mContext);
    }

    protected void onPreExecute() {

	    Log.d(TAG, "Preparing to flash");
        pDialog = new ProgressDialog(mContext);

        int Title;
        if (JOB == 1) {
            Title = R.string.flashing;
        } else {
            Title = R.string.creating_bak;
        }
        pDialog.setTitle(Title);
        pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        pDialog.setMessage(file.getName());
        pDialog.setCancelable(false);
        pDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {

	    Log.i(TAG, "Flashing...");

        try {

            switch (JOB) {

                case 1:
                    if (file.exists()) {
                        if (mDeviceHandler.MTD) {
							File fflash = new File(mContext.getFilesDir(), "flash_image");
                            mCommon.chmod(fflash, "741");
                            mCommon.executeSuShell(mContext, fflash.getAbsolutePath() + " recovery " + file.getAbsolutePath());

                        } else if (!mDeviceHandler.RecoveryPath.equals(""))
                            mCommon.executeSuShell(mContext, "dd if=" + file.getAbsolutePath() + " of=" + mDeviceHandler.RecoveryPath);
                        if (mDeviceHandler.DEVICE_NAME.equals("c6603")
                                || mDeviceHandler.DEVICE_NAME.equals("montblanc")) {
                            if (mCommon.getBooleanPerf(mContext, "flash-util", "first-flash")) {
                                mCommon.mountDir(new File(mDeviceHandler.RecoveryPath), "RW");
                                mCommon.executeSuShell(mContext, "cat " + mDeviceHandler.charger.getAbsolutePath() + " >> /system/bin/" + mDeviceHandler.charger.getName());
                                mCommon.executeSuShell(mContext, "cat " + mDeviceHandler.chargermon.getAbsolutePath() + " >> /system/bin/" + mDeviceHandler.chargermon.getName());
                                if (mDeviceHandler.DEVICE_NAME.equals("c6603")) {
                                    mCommon.executeSuShell(mContext, "cat " + mDeviceHandler.ric.getAbsolutePath() + " >> /system/bin/" + mDeviceHandler.ric.getName());
                                    mCommon.chmod(mDeviceHandler.ric, "755");
                                }
                                mCommon.chmod(mDeviceHandler.charger, "755");
                                mCommon.chmod(mDeviceHandler.chargermon, "755");
                            }
                            mCommon.chmod(file, "644");
                            mCommon.mountDir(new File(mDeviceHandler.RecoveryPath), "RO");
                        }
                    }
                    break;

                case 2:
                    if (mDeviceHandler.MTD) {
						File fdump = new File(mContext.getFilesDir(), "dump_image");
                        mCommon.chmod(mDeviceHandler.fdump, "741");
                        mCommon.executeSuShell(mContext, fdump.getAbsolutePath() + " recovery " + file.getAbsolutePath());
                    } else if (!mDeviceHandler.RecoveryPath.equals(""))
                        mCommon.executeSuShell(mContext, "dd if=" + mDeviceHandler.RecoveryPath + " of=" + file.getAbsolutePath());
                    break;
            }
        } catch (RootAccessDeniedException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void onPostExecute(Boolean succes) {

	    Log.i(TAG, "Flashing finished");

        pDialog.dismiss();
        if (JOB == 1) {
            mNotifyer.createAlertDialog(R.string.tsk_end, mContext.getString(R.string.flashed) + " " + mContext.getString(R.string.reboot_recovery_now), new Runnable() {
                @Override
                public void run() {
                    try {
                        mCommon.executeSuShell("reboot recovery");
                    } catch (RootAccessDeniedException e) {
                        e.printStackTrace();
                    }
                }
            }).show();
        } else {
            mNotifyer.showToast(R.string.bak_done);
        }

        Appirater.appLaunched(mContext);

        mCommon.setBooleanPerf(mContext, "flash-util", "first-flash", false);
    }
}