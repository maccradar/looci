package be.kuleuven.mech.rsg.gui;

import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Button;



/* _  _ */



public class RsgApp extends Activity implements OnSeekBarChangeListener {
	String logTag = "RSG GUI";
	
	/* GUI elements */
	
	private SeekBar xSeekBar;
	private SeekBar ySeekBar;
	private TextView xValueText;
	private TextView yValueText;
	private TextView numberOfObjectsText;
	private Button startButton;
		
	// For controlling state.
	private Boolean isSaved = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		xSeekBar = (SeekBar)findViewById(R.id.xSeekBar); 
		xSeekBar.setOnSeekBarChangeListener(this); 
		ySeekBar = (SeekBar)findViewById(R.id.ySeekBar); 
		ySeekBar.setOnSeekBarChangeListener(this); 
		
		xValueText = (TextView) findViewById(R.id.textViewXValue);
		xValueText.setText("0");
		yValueText = (TextView) findViewById(R.id.TextViewYValue);
		yValueText.setText("0");
		numberOfObjectsText = (TextView) findViewById(R.id.textViewNumberOfObjects);
		numberOfObjectsText.setText("0");
		
		startButton = (Button) findViewById(R.id.startButton);
		// Establish the action for startButton.
		startButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent("rsg:start");
				intent.putExtra("action", 0);
				intent.putExtra("x", Double.parseDouble(xValueText.getText().toString()));
				intent.putExtra("y", Double.parseDouble(yValueText.getText().toString()));
				sendBroadcast(intent);
			}
		});
	}	
		
	/* App life cycle callbacks */
	
	@Override
	protected void onStop() {
		Log.i(logTag, "onStop()");
		super.onStop();
		
	}
	
	@Override
	protected void onDestroy() {
		Log.i(logTag, "onDestroy()");
		super.onDestroy();
		
	}
	
	@Override
	protected void onPause() {
		Log.i(logTag, "onPause()");
		super.onPause();
		
	}
	// Executed each time the activity is resumed.
	@Override
	public void onResume() {

		super.onResume();

		// Clean the flag.
		isSaved = false;

		// Start listening the information broadcasted.
		registerReceiver(display, new IntentFilter("rsg:display"));
		registerReceiver(display_position, new IntentFilter("rsg:display:position"));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    	
    }
	
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    	seekBar.setSecondaryProgress(seekBar.getProgress());
    	//yValueText.setText("starting to track touch");   	
    }
    
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    	int progress = seekBar.getProgress();
    	
    	seekBar.setSecondaryProgress(progress);
    	  	
    	double x = Double.parseDouble(xValueText.getText().toString());
    	double y = Double.parseDouble(yValueText.getText().toString());
    	
    	if(seekBar.getId() == xSeekBar.getId()) {
    		//yValueText.setText("changing x");
    		x = (progress-50.0) / 10.0;
    	} else if (seekBar.getId() == ySeekBar.getId()) {
    		//yValueText.setText("changing y");
    		y = (progress-50.0) / 10.0;
		} else {
			return;
		}
    	xValueText.setText(String.valueOf(x));
    	yValueText.setText(String.valueOf(y));
    	String id = "";
    	
    	Intent intent = new Intent("rsg:position");
		intent.putExtra("entity", id);
		intent.putExtra("x", x);
		intent.putExtra("y", y);
		sendBroadcast(intent);
    	    	
    }
    
    // For receiving the broadcasted information.
 	private BroadcastReceiver display = new BroadcastReceiver() {
 		@Override
 		public void onReceive(Context context, Intent intent) {

 			String value = intent.getStringExtra("value");

 			if (value != null) {
 				Log.i(logTag, "Received: " + value);
 			}
 		}
 	};

 	// For receiving the broadcasted information (accelerometer).
 	private BroadcastReceiver display_position = new BroadcastReceiver() {
 		@Override
 		public void onReceive(Context context, Intent intent) {

 			double valueX = intent.getDoubleExtra("x", 0.0);
 			double valueY = intent.getDoubleExtra("y", 0.0);
 			int valueNrObstacles = intent.getIntExtra("nrObstacles", 0);

 			xValueText.setText(String.valueOf(valueX));
 			yValueText.setText(String.valueOf(valueX));
 			numberOfObjectsText.setText(String.valueOf(valueX));
 		}
 	};
    
}































































