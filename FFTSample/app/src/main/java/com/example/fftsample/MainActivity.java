package com.example.fftsample;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.fftsample.R;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.Manifest;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.widget.ToggleButton;

import weka.classifiers.Classifier;
import weka.core.converters.ArffSaver;
import weka.core.Instances;
import weka.classifiers.trees.J48;
import weka.classifiers.Evaluation;
import weka.core.converters.CSVLoader;

import com.emotiv.insight.IEdk;
import com.emotiv.insight.IEdkErrorCode;
import com.emotiv.insight.IEdk.IEE_DataChannel_t;
import com.emotiv.insight.IEdk.IEE_Event_t;
import com.github.ybq.android.spinkit.SpinKitView;;

public class MainActivity extends Activity {
	audiogenerator generator = new audiogenerator();
	WekaClassifier classifier = new WekaClassifier();

	private Thread processingThread;
	private static final int REQUEST_ENABLE_BT = 1;
	private static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 0;
	private BluetoothAdapter mBluetoothAdapter;
	private boolean lock = false;
	private boolean isEnablGetData = false;
	private boolean isEnableWriteFile = false;
	int userId;
	private BufferedWriter motion_writer;
	Button Start_button,Stop_button;
	IEE_DataChannel_t[] Channel_list = {IEE_DataChannel_t.IED_AF3, IEE_DataChannel_t.IED_T7,IEE_DataChannel_t.IED_Pz,
			IEE_DataChannel_t.IED_T8,IEE_DataChannel_t.IED_AF4, IEE_DataChannel_t.IED_F7, IEE_DataChannel_t.IED_F3, IEE_DataChannel_t.IED_FC5, IEE_DataChannel_t.IED_P7,
			IEE_DataChannel_t.IED_O1, IEE_DataChannel_t.IED_O2, IEE_DataChannel_t.IED_P8, IEE_DataChannel_t.IED_FC6, IEE_DataChannel_t.IED_F4,IEE_DataChannel_t.IED_F8};
	String[] Name_Channel = {"AF3","T7","Pz","T8","AF4", "F7", "F3", "FC5", "P7", "O1", "O2", "P8", "FC6", "F4", "F8"};
	private volatile double[] musicarray= {0,0,0,0,0};
	private double[][] sample;
	private Calendar c;
	private TextView time;
	private int moe = 1;
	volatile Boolean stopped = false;
	public volatile Boolean brainworking=false, eye=false, brainworking_final = false, eye_final = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final ToggleButton toggle = (ToggleButton)findViewById(R.id.toggleButton);
		final SpinKitView spinner =  (SpinKitView)findViewById(R.id.spin_kit);

		//Create a Threadpool manager
		final ExecutorService service = Executors.newFixedThreadPool(2);

	   //Create Bluetooth Manager
		final BluetoothManager bluetoothManager =
				(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		c = Calendar.getInstance();

		/***************************************************/
		//Request Permissions for bluetooth access
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			/***Android 6.0 and higher need to request permission*****/
			if (ContextCompat.checkSelfPermission(this,
					Manifest.permission.ACCESS_FINE_LOCATION)
					!= PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
						MY_PERMISSIONS_REQUEST_BLUETOOTH);
			}
			else{	checkConnect();		}
		}else {	checkConnect();		}
		/***************************************************/

		toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
				if(isChecked){
					Log.e("FFTSample","Start Write File");
					spinner.setVisibility(View.VISIBLE);
					stopped = false;
					service.submit(new Tone());
					isEnableWriteFile = true;
				}else{
					Log.e("FFTSample","Stop Write File");
					spinner.setVisibility(View.GONE);
					stopped = true;
					isEnableWriteFile = false;
				}
			}
		});


		processingThread=new Thread()
		{
			@Override
			public void run() {
				// TODO Auto-generated method stub
				//System.out.println(c.get(Calendar.MILLISECOND));
				super.run();
				int loopcounter = 1;
				while(!stopped)
				{
					try
					{
						//Get Next Event from Headset
						handler.sendEmptyMessage(0);
						//Double Check Device Still connected
						handler.sendEmptyMessage(1);
						//Get data into CSV file
						if(isEnablGetData && isEnableWriteFile)handler.sendEmptyMessage(2);
						if(loopcounter%200==0) {
							//Every ten loops do classification process
							handler.sendEmptyMessage(3);
						}
						Thread.sleep(5);
						loopcounter++;
					}
					
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
			}
		};
		processingThread.start();

	}

	public class Tone implements Callable<Object>{

		@Override
		public Object call() throws Exception {
			while(!stopped) {
				generator.playsound(musicarray,brainworking_final,eye_final);

			}
			return null;
		}
	}


	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case 0:
				int state = IEdk.IEE_EngineGetNextEvent();
				if (state == IEdkErrorCode.EDK_OK.ToInt()) {
					int eventType = IEdk.IEE_EmoEngineEventGetType();
				    userId = IEdk.IEE_EmoEngineEventGetUserId();
					if(eventType == IEE_Event_t.IEE_UserAdded.ToInt()){
						Log.e("SDK","User added");
						IEdk.IEE_FFTSetWindowingType(userId, IEdk.IEE_WindowsType_t.IEE_BLACKMAN);
						isEnablGetData = true;
					}
					if(eventType == IEE_Event_t.IEE_UserRemoved.ToInt()){
						Log.e("SDK","User removed");		
						isEnablGetData = false;
					}
				}
				
				break;

			case 1:

				/*Connect device with Epoc Plus headset*/
				int number = IEdk.IEE_GetEpocPlusDeviceCount();
				if(number != 0) {
					if(!lock){
						lock = true;
						IEdk.IEE_ConnectEpocPlusDevice(0,false);
					}
				}

				else lock = false;
				break;

			case 2:

				int count1=0,count2=0,count3=0,count4 =0;

				for(int i=0; i < Channel_list.length; i++)
				{
					double[] data = IEdk.IEE_GetAverageBandPowers(Channel_list[i]);

					//Pass first set of values to put music generator
					if(i==0&&data.length==5){
						for(int k=0; k< data.length;k++) {
							musicarray[k] = data[k];
							System.out.println(musicarray[k]);
						}
					}

					if(data.length == 5){
						try{
							double result = classifier.classify(data);
							if (result == 0){
								count1++;
							}else if (result ==1){
								count2++;
							}else if (result ==2){
								count3++;
							}else if(result ==3){
								count4++;
							}

						} catch (Exception e) {
							Log.e("","Exception"+ e.getMessage());
						}

					}
				}
				int max = Math.max(Math.max(Math.max(count1,count2),count3),count4);
				if(count1==max){
					brainworking = false;
					eye = false;
				}else if(count2 ==max){
					brainworking = false;
					eye = true;
				}else if(count3 == max){
					brainworking = true;
					eye = false;
				}else if(count4 ==max){
					brainworking = true;
					eye = true;
				}

			case 3:
				brainworking_final = brainworking;
				System.out.println("Brain working final is" + brainworking_final);
				eye_final = eye;
				System.out.println("Eye final is" + eye_final);
				break;
			}

		}

	};

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUEST_BLUETOOTH: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					checkConnect();

				} else {

					// permission denied, boo! Disable the
					// functionality that depends on this permission.
					Toast.makeText(this, "App can't run without this permission", Toast.LENGTH_SHORT).show();
				}
				return;
			}

		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == REQUEST_ENABLE_BT) {
			if(resultCode == Activity.RESULT_OK){
				//Connect to emoEngine
				IEdk.IEE_EngineConnect(this,"");
			}
			if (resultCode == Activity.RESULT_CANCELED) {
				Toast.makeText(this, "You must be turn on bluetooth to connect with Emotiv devices"
						, Toast.LENGTH_SHORT).show();
			}
		}
	}


	private void checkConnect(){
		if (!mBluetoothAdapter.isEnabled()) {
			/****Request turn on Bluetooth***************/
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		else
		{
			//Connect to emoEngine
			IEdk.IEE_EngineConnect(this,"");
		}
	}


}
