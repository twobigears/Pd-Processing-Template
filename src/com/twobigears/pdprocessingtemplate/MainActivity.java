

package com.twobigears.pdprocessingtemplate;

import java.io.File;
import java.io.IOException;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdService;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;



import processing.core.PApplet;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

public class MainActivity extends PApplet {
	
	
	public int sketchWidth() {
		return displayWidth;
	}

	public int sketchHeight() {
		return displayHeight;
	}

	public String sketchRenderer() {
		return JAVA2D;
	}
	
	private PdService pdService = null;
	private PdUiDispatcher dispatcher;
	float pd = 0;
	String TAG="Processing Template";
	
	int bgCol = color(28, 28, 28);
	
	
	/**
	 * setting up libPd as a background service the initPdService() method binds
	 * the service to the background thread. call initPdService in onCreate() to
	 * start the service.
	 */

	protected final ServiceConnection pdConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			pdService = ((PdService.PdBinder) service).getService();

			try {
				initPd();
				loadPatch();
			} catch (IOException e) {
				Log.e(TAG, e.toString());
				finish();
			}

		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// Never called

		}
	};

	/* Bind pd service */

	private void initPdService() {

		new Thread() {
			@Override
			public void run() {
				bindService(new Intent(MainActivity.this, PdService.class),
						pdConnection, BIND_AUTO_CREATE);
			}
		}.start();
	}

	/* initialise pd, also setup listeners here */
	protected void initPd() throws IOException {

		// Configure the audio glue
		int sampleRate = AudioParameters.suggestSampleRate();


		pdService.initAudio(sampleRate, 0, 2, 10.0f);
		
		pdService.startAudio(new Intent(this, MainActivity.class),
				R.drawable.icon, "Sample", "Return to Processing");

		dispatcher = new PdUiDispatcher();
		PdBase.setReceiver(dispatcher);



	}

	protected void loadPatch() throws IOException {

		if (pd == 0) {
			File dir = getFilesDir();
			IoUtils.extractZipResource(
					getResources().openRawResource(
							com.twobigears.pdprocessingtemplate.R.raw.synth), dir,
					true);
			File patchFile = new File(dir, "synth.pd");
			pd = PdBase.openPatch(patchFile.getAbsolutePath());


		}
	}

	//setup Processing stuff
	
	public void setup(){
		frameRate(30);
		background(bgCol);
	}
	
	public void draw(){
		background(bgCol);
	}


	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_main);
		initPdService();
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {


		width = displayWidth;
		height = displayHeight;
		
		//get and normalise the touch coordinates
		float x = (event.getX())/width;
		float y = (event.getY())/height;
		
		int action = event.getActionMasked();
		
		switch(action){
		case MotionEvent.ACTION_DOWN:
			PdBase.sendFloat("pd_toggle",1);
			bgCol=color(x*255,y*255,Math.abs(x-y)*255);
			break;
		
		
		case MotionEvent.ACTION_UP:
			PdBase.sendFloat("pd_toggle",0);
			bgCol=color(28,28,28);
			break;
		
		case MotionEvent.ACTION_MOVE:
			PdBase.sendFloat("pd_x", x);
			PdBase.sendFloat("pd_y", y);
			bgCol=color(x*255,y*255,Math.abs(x-y)*255);
			break;
			
		}
		return super.dispatchTouchEvent(event);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();

		// release all resources called by pdservice
		dispatcher.release();
		if (pd != 0) {
			PdBase.closePatch((int) pd);
			pd = 0;
		}
		pdService.stopAudio();
		unbindService(pdConnection);
	}



}
