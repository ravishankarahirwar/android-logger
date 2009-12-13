/*
 * Copyright (C) 2009  Michael Novak <mike@androidnerds.org>
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.michaelrnovak.util.logger.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

public class LogProcessor extends Service {
	
	private Thread mThread;
	private Thread mWriterThread;
	private Process mProcess;
	private static Handler mHandler;
	private String mFile;
	private Vector<String> mScrollback;
	private int mLines;
	public int MAX_LINES = 250;
	public static final int MSG_READ_FAIL = 1;
	public static final int MSG_LOG_FAIL = 2;
	public static final int MSG_NEW_LINE = 3;
	public static final int MSG_RESET_LOG = 4;
	public static final int MSG_LOG_SAVE = 5;
	
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
				
				if (mLines == MAX_LINES) {
					mScrollback.removeElementAt(0);
				}
				
				mScrollback.add(line);
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
			Thread thr = mThread;
			mThread = null;
			thr.interrupt();
			
			mLines = 0;
			mScrollback.removeAllElements();
			mThread = new Thread(worker);
			mThread.start();
		}
		
		public void run() {
			mLines = 0;
			mScrollback = new Vector<String>();
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
		
		public void write(String file) {
			mFile = file;
			mWriterThread = new Thread(writer);
			mWriterThread.start();
		}
	};
	
	Runnable writer = new Runnable() {
		public void run() {
			writeLog();
		}
	};
	
	private void writeLog() {
		
		try {			
			File f = new File("/sdcard/" + mFile);
			FileWriter w = new FileWriter(f);
			
			for (int i = 0; i < mScrollback.size(); i++) {
				w.write(mScrollback.elementAt(i) + "\n");
				i++;
			}
			
			if (!mFile.equals("tmp.log")) {
				Message.obtain(mHandler, MSG_LOG_SAVE, "saved").sendToTarget();
			} else {
				Message.obtain(mHandler, MSG_LOG_SAVE, "attachment").sendToTarget();
			}
			
		} catch (Exception e) {
			Log.e("Logger", "Error writing the log to a file. Exception: " + e.toString());
			Message.obtain(mHandler, MSG_LOG_SAVE, "error").sendToTarget();
		}
		
		Thread thr = mWriterThread;
		mWriterThread = null;
		thr.interrupt();
	}

}
