/*
 * Copyright (C) 2009 Michael Novak <mike@androidnerds.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.michaelrnovak.util.logger.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LogProcessor extends Service {
	
	private Thread mThread;
	private Process mProcess;
	private static Handler mHandler;
	private int mLines;
	public static final int MSG_READ_FAIL = 1;
	public static final int MSG_LOG_FAIL = 2;
	public static final int MSG_NEW_LINE = 3;
	public static final int MSG_RESET_LOG = 4;
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.i("Logger", "Logger Service has hit the onStart method.");
	}

	Runnable worker = new Runnable() {
		public void run() {
			runLog();
		}
	};
	
	private void runLog() {
		try {
			mProcess = Runtime.getRuntime().exec("/system/bin/logcat");
		} catch (IOException e) {
			communicate(MSG_LOG_FAIL);
		}
		
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));
			
			String line;
			
			while ((line = reader.readLine()) != null) {
				logLine(line);
				mLines++;
			}
			
		} catch (IOException e) {
			communicate(MSG_READ_FAIL);
		}
	}
	
	private void communicate(int msg) {
		Message.obtain(mHandler, msg, "error").sendToTarget();
	}
	
	private void logLine(String line) {
		Message.obtain(mHandler, MSG_NEW_LINE, line).sendToTarget();
	}
	
	public static void setHandler(Handler handler) {
		mHandler = handler;
	}
	
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Thread tmp = mThread;
		mThread = null;
		tmp.interrupt();
		stopSelf();
		
		return false;
	}
	
	private final ILogProcessor.Stub mBinder = new ILogProcessor.Stub() {
		public void reset() {
			mThread.interrupt();
			mLines = 0;
			
			mThread = new Thread(worker);
			mThread.start();
		}
		
		public void run() {
			mLines = 0;
			mThread = new Thread(worker);
			mThread.start();
		}
		
		public void stop() {
			Log.i("Logger", "stop() method called in service.");
			Thread tmp = mThread;
			mThread = null;
			tmp.interrupt();
			stopSelf();
		}
	};

}
