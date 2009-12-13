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
package com.michaelrnovak.util.logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.michaelrnovak.util.logger.service.ILogProcessor;
import com.michaelrnovak.util.logger.service.LogProcessor;

import java.util.HashMap;

public class Logger extends Activity {
	private ILogProcessor mService;
	private ScrollView mScrollView;
	private LinearLayout mLines;
	private AlertDialog mDialog;
	private int mFilter = -1;
	private boolean mServiceRunning = false;
	public int MAX_LINES = 250;
	public static final int DIALOG_FILTER_ID = 1;
	public static final int FILTER_OPTION = Menu.FIRST;
	public static final int EMAIL_OPTION = Menu.FIRST + 1;
	final CharSequence[] items = {"Debug", "Error", "Info", "Verbose", "Warn", "All"};
	final char[] mFilters = {'D', 'E', 'I', 'V', 'W'};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mScrollView = (ScrollView) findViewById(R.id.scrollView);
        mLines = (LinearLayout) findViewById(R.id.lines);
        
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	bindService(new Intent(this, LogProcessor.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	unbindService(mConnection);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	
    	menu.add(0, FILTER_OPTION, 1, "Filter Log").setIcon(android.R.drawable.ic_menu_view);
    	menu.add(0, EMAIL_OPTION, 1, "Email Log").setIcon(android.R.drawable.ic_menu_send);
    	
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case FILTER_OPTION:
    		onCreateDialog(DIALOG_FILTER_ID);
    		break;
    	case EMAIL_OPTION:
    		break;
    	}
    	
    	return false;
    }
    
    protected Dialog onCreateDialog(int id) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	
    	switch (id) {
    	case DIALOG_FILTER_ID:
    		builder.setTitle("Select a filter level");
    		builder.setSingleChoiceItems(items, mFilter, mClickListener);
    		mDialog = builder.create();
    		break;
    	}
    	
    	mDialog.show();
    	return mDialog;
    }
    
    DialogInterface.OnClickListener mClickListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (which == 5) {
				mFilter = -1;
			} else {
				mFilter = which;
			}
			
			updateFilter();
		}
	};

    public void stopLogging() {
    	unbindService(mConnection);
    	mServiceRunning = false;
    	
    	if (mServiceRunning) {
    		Log.d("Logger", "mServiceRunning is still TRUE");
    	}
    }
    
    public void startLogging() {
    	bindService(new Intent(this, LogProcessor.class), mConnection, Context.BIND_AUTO_CREATE);
    	
    	try {
    		mService.run();
    		mServiceRunning = true;
    	} catch (RemoteException e) {
    		Log.e("Logger", "Could not start logging");
    	}
    }
    
    private void handleLogMessage(String line) {
    	if (mFilter != -1 && line.charAt(0) != mFilters[mFilter]) {
    		return;
    	}
    	
    	TextView lineView = new TextView(this);
    	lineView.setTypeface(Typeface.MONOSPACE);
    	lineView.setText(new LogFormattedString(line));
    	
    	final boolean autoscroll = 
            (mScrollView.getScrollY() + mScrollView.getHeight() >= mLines.getBottom()) ? true : false;
    	
    	mLines.addView(lineView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
    	
    	if (mLines.getChildCount() > MAX_LINES) {
    		mLines.removeViewAt(0);
    	}
    	
    	mScrollView.post(new Runnable() {
    		public void run() {
    			if (autoscroll == true) {
    				mScrollView.scrollTo(0, mLines.getBottom() - mScrollView.getHeight());
    			}
    		}
    	});
    }
    
    private void updateFilter() {
    	mLines.removeAllViews();
    	
    	try {
    		mService.reset();
    	} catch (RemoteException e) {
    		Log.e("Logger", "Service is gone...");
    	}
    	
    	mDialog.dismiss();
    }
    
    public Handler mHandler = new Handler() {
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
    		case LogProcessor.MSG_READ_FAIL:
    			Log.d("Logger", "MSG_READ_FAIL");
    			break;
    		case LogProcessor.MSG_LOG_FAIL:
    			Log.d("Logger", "MSG_LOG_FAIL");
    			break;
    		case LogProcessor.MSG_NEW_LINE:
    			handleLogMessage((String) msg.obj);
    			break;
    		default:
    			super.handleMessage(msg);
    		}
    	}
    };
    
    private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = ILogProcessor.Stub.asInterface((IBinder)service);
			LogProcessor.setHandler(mHandler);
			
			try {
				mService.run();
				mServiceRunning = true;
			} catch (RemoteException e) {
				Log.e("Logger", "Could not start logging");
			}
		}
		
		public void onServiceDisconnected(ComponentName className) {
			Log.i("Logger", "onServiceDisconnected has been called");
			mService = null;
		}
	};
    
    private static class LogFormattedString extends SpannableString {
    	public static final HashMap<Character, Integer> LABEL_COLOR_MAP;
    	
    	public LogFormattedString(String line) {
    		super(line);
    		
    		try {
    			
    			if (line.length() < 4) {
    				throw new RuntimeException();
    			}
    			
    			if (line.charAt(1) != '/') {
    				throw new RuntimeException();
    			}
    			
    			Integer labelColor = LABEL_COLOR_MAP.get(line.charAt(0));
    			
    			if (labelColor == null) {
    				labelColor = LABEL_COLOR_MAP.get('E');
    			}
    			
    			setSpan(new ForegroundColorSpan(labelColor), 0, 1, 0);
    			setSpan(new StyleSpan(Typeface.BOLD), 0, 1, 0);
    			
    			int leftIdx;
    			
    			if ((leftIdx = line.indexOf(':', 2)) >= 0) {
    				setSpan(new ForegroundColorSpan(labelColor), 2, leftIdx, 0);
    				setSpan(new StyleSpan(Typeface.ITALIC), 2, leftIdx, 0);
    			}
    			
    		} catch (Exception e) {
    			setSpan(new ForegroundColorSpan(0xffddaacc), 0, length(), 0);
    		}
    	}
    	
    	static {
    		LABEL_COLOR_MAP = new HashMap<Character, Integer>();
    		LABEL_COLOR_MAP.put('D', 0xff9999ff);
    		LABEL_COLOR_MAP.put('V', 0xffcccccc);
    		LABEL_COLOR_MAP.put('I', 0xffeeeeee);
    		LABEL_COLOR_MAP.put('E', 0xffff9999);
    		LABEL_COLOR_MAP.put('W', 0xffffff99);
    	}
    }
}