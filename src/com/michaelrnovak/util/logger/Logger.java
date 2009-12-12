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
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.michaelrnovak.util.logger.service.LogProcessor;

import java.util.HashMap;

public class Logger extends Activity {
	private ScrollView mScrollView;
	private LinearLayout mLines;
	public int MAX_LINES = 250;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mScrollView = (ScrollView) findViewById(R.id.scrollView);
        mLines = (LinearLayout) findViewById(R.id.lines);
        
        startLogging();
    }
    
    public void startLogging() {
    	Intent serviceIntent = new Intent(Logger.this, LogProcessor.class);
    	
    	ComponentName service = startService(serviceIntent);
    	
    	assert(service != null);
    	
    	LogProcessor.setHandler(mHandler);
    }
    
    private void handleLogMessage(String line) {
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
    				mScrollView.scrollTo(mLines.getBottom(), mScrollView.getHeight());
    			}
    		}
    	});
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