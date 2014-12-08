package be.kuleuven.mech.rsg.looci.android.zmq;

import java.util.ArrayList;



// RSG imports
import be.kuleuven.mech.rsg.*;
import be.kuleuven.mech.rsg.jni.RsgJNI;

// LooCI imports
import looci.osgi.serv.components.Event;
import looci.osgi.serv.impl.LoociComponent;
import looci.osgi.serv.impl.PayloadBuilder;
import looci.osgi.serv.impl.property.PropertyString;

import org.osgi.framework.ServiceReference;



// Android imports
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.os.StrictMode;


//import org.jeromq.ZMQ; depends on used version of JeroMQ
import org.zeromq.ZMQ;

/* _  _ */


public class RsgComponent extends LoociComponent {
	private PropertyString inputPortProperty;
	private PropertyString outputPortProperty;
	
	private Context app_context; // Reference to use Android API.
	
  	// Event for receiving data and showing in the display.
 	public static final short IN_EVENT = (short) 501;
 	// Generate an event based on the button action.
 	public static final short OUT1_EVENT = (short) 402;
 	// Generate an event based on a position updates.
 	public static final short OUT2_EVENT = (short) 403;
  	
 	public static final String DEFAULT_INPUT_PORT = "tcp://192.168.1.101:11511";
 	public static final String DEFAULT_OUTPUT_PORT = "tcp://*:11411";
 	private static final int STOP_LISTENING = 0;
 	private static final int START_LISTENING = 1;
 	
	Thread listenerThread = null;
	
	String logTag = "YouBotWorldModel";
	SceneObject virtualFence = null;
	SceneObject obstacle = null;
	Box fenceBox = null;
	HomogeneousMatrix44 obstaclePose = null;
	Sphere obstacleShape = null; 
	
	public RsgComponent() {
		inputPortProperty = new PropertyString((short)4, "input_port", DEFAULT_INPUT_PORT); 
		addProperty(inputPortProperty);
	}

	public void componentAfterProperty(short propertyId){
		if(propertyId == inputPortProperty.getPropertyId()){
			System.out.println("Input port updated!");
		} else	if(propertyId == outputPortProperty.getPropertyId()){
			System.out.println("Output port updated!");
		}
	}
	
 	@SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void componentStart() {
 		System.out.println("[RsgComponent] componentStart begin");
 		// Obtain the reference to access to Android API.
        ServiceReference ref = getCodebase().getBundleContext()
                        .getServiceReference(Context.class.getName());
        if (ref != null) {
            app_context = (Context) getCodebase().getBundleContext()
                            .getService(ref);

            // For listening the action from the button.
            app_context.registerReceiver(broadcastReceiverStart,
                            new IntentFilter("rsg:start"));
            
            // For listening for position updates.
            app_context.registerReceiver(broadcastReceiverPosition,
                            new IntentFilter("rsg:position"));
            
        }
        System.out.println("[RsgComponent] componentStart end");
       //initializeWorldModel();
    }	

	@Override
    public void receive(short eventID, byte[] payload) {

        // Get the information from the event.
        Event event = getReceptionEvent();

        // Analyze the eventID.
        if (event.getEventID() == IN_EVENT) {
            // Receive an event and show the payload on the display.
            PayloadBuilder pb = new PayloadBuilder(event.getPayload());
            int value = pb.getIntegerAt(0);
            //broadcastinfo("value", value);
            if(value == START_LISTENING) {
            	System.out.println("[RsgComponent] Event IN_EVENT received with start listening command");
            	initializeWorldModel();
            } else if(value == STOP_LISTENING) {
            	System.out.println("[RsgComponent] Event IN_EVENT received with stop listening command");
            	cleanup();
            }
        }
    }

    // For receiving the broadcast event.
    private BroadcastReceiver broadcastReceiverStart = new
        BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        // Publish the event when the button is clicked.
        int value = intent.getIntExtra("action", 0);
        double x = intent.getDoubleExtra("x", 0.0);
        double y = intent.getDoubleExtra("y", 0.0);
        PayloadBuilder pb = new PayloadBuilder();
        pb.addString("goal update: (" + x + ", " + y + ")");
        

        // Publish the event.
        publish(new Event(OUT1_EVENT, pb.getPayload()));

        // For logging.
        System.out.println("[RsgComponent] Received goal position: (" + x + ", " + y + ")");
        }
    };
    
    // For receiving the broadcast event.
    private BroadcastReceiver broadcastReceiverPosition = new
        BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        // Publish the event when the button is clicked.
        double x = intent.getDoubleExtra("x", 0.0);
        double y = intent.getDoubleExtra("y", 0.0);
        PayloadBuilder pb = new PayloadBuilder();
        pb.addString("position update: (" + x + ", " + y + ")");
        

        // Publish the event.
        publish(new Event(OUT2_EVENT, pb.getPayload()));
        
        ArrayList<Attribute> queryAttributes = new ArrayList<Attribute>();
		queryAttributes.add(new Attribute("name", "obstacle"));
		ArrayList<SceneObject> foundSceneObjects = Rsg.getSceneObjects(queryAttributes);
		Log.i("onProgressChanged", "Result (obsatcles) = found " + foundSceneObjects.size() + " Scene object(s)");
    	
		if(foundSceneObjects.size() > 0) {
			SceneObject obstacle = foundSceneObjects.get(0);
	    	
			/* Move obstacle a bit */
			HomogeneousMatrix44 obstaclePoseUpdate = new HomogeneousMatrix44(
					1, 0, 0, // rotation  
					0, 1, 0, 
					0, 0, 1,
					x, y, 0); // translation
			Rsg.insertTransform(obstacle.id, obstaclePoseUpdate);  
	    	
			displayObstacleCoordinates();			
			onWorldModelUpdate();
    	}
        // For logging.
        System.out.println("[RsgComponent] Received position update: (" + x + ", " + y + ")");
        }
    };

    // For broadcasting the information.
    private void broadcastinfo(String tag, int value) {
        Intent intent = new Intent("rsg:display");

        // Put all the extra information.
        intent.putExtra(tag, Integer.toString(value));

        // Broadcast it and the display receives it.
        app_context.sendBroadcast(intent);

        // For logging.
        System.out.println("[RsgComponent] Values for the display sent.");
    }
	
	@Override
	public void componentStop() {
	 	// Stop listening the button.
        app_context.unregisterReceiver(broadcastReceiverStart);
		cleanup();
	}
	
	public void initializeWorldModel() {
		System.out.println("[RsgComponent] initializeWorldModel begin");
	
		Rsg.initializeWorldModel(); // always start with that one.
		System.out.println("[RsgComponent] Rsg.initializeWorldModel done");
//		WorldModelUpdatesBroadcaster outputPort = new WorldModelUpdatesBroadcaster("tcp://192.168.1.101:11411");
		WorldModelUpdatesBroadcaster outputPort = new WorldModelUpdatesBroadcaster(outputPortProperty.getVal());
		Rsg.setOutPort(outputPort);
		Log.i(logTag, "output port created with value " + outputPortProperty.getVal());
		virtualFence = new SceneObject();
		obstacle = new SceneObject(); 

		fenceBox = new Box(5, 6.1, 0);//[m] 
		virtualFence.addBox(fenceBox);								
		Log.i(logTag, "Box = " + fenceBox.getSizeX() + ", " + fenceBox.getSizeY() + ", " + fenceBox.getSizeZ());			
		ArrayList<Attribute> attributes = new ArrayList<Attribute>();
		attributes.add(new Attribute("name", "virtual_fence"));
		virtualFence.addAttributes(attributes);

		displayObstacleCoordinates();
		
		obstacleShape = new Sphere(0.35); //[m] 
		obstacle.addSphere(obstacleShape);	
		obstaclePose = new HomogeneousMatrix44(
				1, 0, 0, // rotation  
				0, 1, 0, 
				0, 0, 1,
				2.1, 1.5, 0); // translation
		obstacle.addTransform(obstaclePose);
		attributes.clear();
		attributes.add(new Attribute("name", "obstacle"));
		obstacle.addAttributes(attributes);
		Log.i(logTag, "Spere = " + obstacleShape.getRadius());   


		Id fenceId = Rsg.addSceneObject(virtualFence);
		Log.i(logTag, "Added virtualFence with ID = " + fenceId.toString());  
		Id obstacleId = Rsg.addSceneObject(obstacle);
		Log.i(logTag, "Added obstaceId with ID = " + obstacleId.toString());  
				
		displayObstacleCoordinates();
		
		
		ArrayList<Attribute> emptyAttributes = new ArrayList<Attribute>();
		ArrayList<SceneObject> foundAllSceneOjects = Rsg.getSceneObjects(emptyAttributes);
		Log.i(logTag, "Result (all) = found " + foundAllSceneOjects.size() + " Scene object(s)");
		printSceneObjects(foundAllSceneOjects);
	

		ArrayList<SceneObject> foundSceneOjects = Rsg.getSceneObjects(attributes);
		Log.i(logTag, "Result = found " + foundSceneOjects.size() + " Scene object(s)");
		printSceneObjects(foundSceneOjects);

		/* Move obstacle a bit */
		HomogeneousMatrix44 obstaclePoseUpdate = new HomogeneousMatrix44(
				1, 0, 0, // rotation  
				0, 1, 0, 
				0, 0, 1,
				3.3, 4.4, 0); // translation
		Rsg.insertTransform(obstacleId, obstaclePoseUpdate);  
		
		displayObstacleCoordinates();
		
		
		/* Move obstacle a bit */
		HomogeneousMatrix44 obstaclePoseUpdate2 = new HomogeneousMatrix44(
				1, 0, 0, // rotation  
				0, 1, 0, 
				0, 0, 1,
				3.4, 4.5, 0); // translation
		Rsg.insertTransform(obstacleId, obstaclePoseUpdate2);   
		
		foundAllSceneOjects.clear();
		foundAllSceneOjects = Rsg.getSceneObjects(emptyAttributes);
		Log.i(logTag, "Result (all;again) = found " + foundAllSceneOjects.size() + " Scene object(s)");
		printSceneObjects(foundAllSceneOjects);
		
		byte[] testData = new byte[8];  
		testData[0] = (byte)0xDE;   
		testData[1] = (byte)0xAF;     
		testData[2] = (byte)0xBE;  
		testData[3] = (byte)0xAF;
		testData[4] = (byte)'Q';
		int processedBytes = RsgJNI.writeUpdateToInputPort(testData, testData.length);
		
//		listenerThread = new Thread(new WorldModelUpdatesListener());
//		listenerThread = new Thread(new WorldModelUpdatesListener("tcp://192.168.1.101:11511"));
		listenerThread = new Thread(new WorldModelUpdatesListener("tcp://192.168.1.101:11511"));
		listenerThread.start();
		
		displayObstacleCoordinates();
		
		Log.i(logTag, "RSG init done.");
	}


	public void printSceneObjects(ArrayList<SceneObject> sceneOjects) {
		for (SceneObject sceneObject : sceneOjects) {

			/* Just print everything */
			Log.i(logTag, "	Scene Object has ID = " + sceneObject.id);
			Log.i(logTag, "	Scene Object has parentId = " + sceneObject.parentId);

			Log.i(logTag, "	Scene Object has position (x,y,z) = (" 
					+ sceneObject.getTransform().getX() + ", " 
					+ sceneObject.getTransform().getY() + ", " 
					+ sceneObject.getTransform().getZ() + ")");

			if(sceneObject.getBox() != null) {
				Log.i(logTag, "	Scene Object has a box shape (x,y,z) =  (" 
						+ sceneObject.getBox().getSizeX() + ", "
						+ sceneObject.getBox().getSizeY() + ", "
						+ sceneObject.getBox().getSizeZ() + ")");
			} else if (sceneObject.getSphere() != null) {
				Log.i(logTag, "	Scene Object has a shere shape (radius) =  (" 
						+ sceneObject.getSphere().getRadius() + ")");
			} else {
				Log.w(logTag, "	Scene Object has unkonwn shape.");
			}

			for (Attribute a : sceneObject.getAttributes()) {
				Log.i(logTag, "	Scene Object has a attribute: " + a.toString()); 
			}
			
			Log.i(logTag, "	------------");
		}
	}
	
	public void displayObstacleCoordinates() {
		double x = -1.0;
		double y = -1.0;
		ArrayList<Attribute> queryAttributes = new ArrayList<Attribute>();
		queryAttributes.add(new Attribute("name", "obstacle"));
                ArrayList<SceneObject> foundSceneOjects = Rsg.getSceneObjects(queryAttributes);
		Log.i("displayObstacleCoordinates", "Result (obsatcles) = found " + foundSceneOjects.size() + " Scene object(s)");
		
		if(foundSceneOjects.size() > 0) {
			SceneObject obstacle = foundSceneOjects.get(0);
			x = obstacle.getTransform().getX();
			y = obstacle.getTransform().getY();
			Log.i("displayObstacleCoordinates", " (x,y) = (" + x + ", " + y + ")");
		}

		queryAttributes.clear();
		ArrayList<SceneObject> allFoundSceneOjects = Rsg.getSceneObjects(queryAttributes);
		
		// Use Android Intent & Broadcast channel
		Intent intent = new Intent("rsg:display:position");

        // Put all the extra information.
        intent.putExtra("x", String.format("%2.2f", x));
        intent.putExtra("y", String.format("%2.2f", y));
        intent.putExtra("nrObstacles", String.format("%d", allFoundSceneOjects.size()));

        // Broadcast it and the display receives it.
        app_context.sendBroadcast(intent);
		
	}
	
	/* Callback for changes of the world model */
	public void onWorldModelUpdate() {
		Log.i(logTag, "onWorldModelUpdate()");  

		/* Get all current scene objects */
		ArrayList<Attribute> emptyAttributes = new ArrayList<Attribute>();
		ArrayList<SceneObject> foundSceneOjects = Rsg.getSceneObjects(emptyAttributes);
		Log.i(logTag, "There are " + foundSceneOjects.size() + " Scene object(s).");
		printSceneObjects(foundSceneOjects);
		
		
		double x = -1.0;
		double y = -1.0;
		if(foundSceneOjects.size() > 0) {
			SceneObject obstacle = foundSceneOjects.get(0);
			x = obstacle.getTransform().getX();
			y = obstacle.getTransform().getY();
		}
		
		/* Display (some) values on GUI */
        // Use Android Intent & Broadcast channel
		Intent intent = new Intent("rsg:display:position");

        // Put all the extra information.
        intent.putExtra("x", String.format("%2.2f", x));
        intent.putExtra("y", String.format("%2.2f", y));
        intent.putExtra("nrObstacles", String.format("%d", foundSceneOjects.size()));

        // Broadcast it and the display receives it.
        app_context.sendBroadcast(intent);		
	}
	
	void cleanup() {
		if(listenerThread != null) {
			listenerThread.interrupt();
			listenerThread = null;
		}
		Rsg.cleanupWorldModel();
	}
    
	/**
	 * ZMQ based communication mechanism for receiving world model update messages.
	 */
	public class WorldModelUpdatesListener implements Runnable {
		
//		private String text;
//	    private byte[] message = null;

		private ZMQ.Context context = null;
	    private ZMQ.Socket subscriber = null;				// mechanism
		private String zmqInputConnectionSpecification;		// policy
	    	   
		public WorldModelUpdatesListener() {
			this("tcp://localhost:11411");
		}
		
		public WorldModelUpdatesListener(String zmqInputConnectionSpecification) {
			this.zmqInputConnectionSpecification = zmqInputConnectionSpecification;
			
	        context = ZMQ.context(1);
	        subscriber = context.socket(ZMQ.SUB);
	        subscriber.connect(zmqInputConnectionSpecification);
	        
//	        subscriber->setsockopt(ZMQ_SUBSCRIBE, "1", 0);
//	        subscriber.subscribe(null); // ?
	        String filter = "";
	        subscriber.subscribe(filter.getBytes(ZMQ.CHARSET));
	        
		    int BUFLEN = 20000; // For HDF5 messages longer than this we need some other transport mechanism.
			 					// In this demo all upatates are around 10k. Thus, this buffer is sufficient.
//		    message = new byte[BUFLEN];	    
			Log.i(logTag, "Starting WorldModelUpdatesListener.");
		}
		
		@Override
		public void run() {			
			while(true) {
				Log.i(logTag, "WorldModelUpdatesListener: Waiting for incomming message.");
				try {
					
					/* Receive data */
					byte[] message = subscriber.recv(); // null Pointer
					String text = new String(message, 0, message.length);
					Log.d(logTag, "message: with length" +  message.length + " = " + text);
					
					/* Process data */
					int processedBytes = RsgJNI.writeUpdateToInputPort(message, message.length);
					 
					/* Inform GUI */
					onWorldModelUpdate();
					
				} catch (Exception e) {
					Log.i(logTag, "Shutting down WorldModelUpdatesListener interrupted.");
					e.printStackTrace();
					cleanup();	
					break;
				}
			}
		}

		protected void cleanup() {
			Log.i(logTag, "Shutting down WorldModelUpdatesListener.");
			if (subscriber != null) {
				subscriber.close();	
			}

		}
	}
	
	public class WorldModelUpdatesBroadcaster /*extends AsyncTask*/ implements IOutputPort {
		
		private String zmqConnectionSpecification;
		private ZMQ.Socket publisher = null;
		
		public WorldModelUpdatesBroadcaster() {
			this("tcp://*:11411");
		}
		
		public WorldModelUpdatesBroadcaster(String zmqConnectionSpecification) {
			this.zmqConnectionSpecification = zmqConnectionSpecification;
			/* workaround for android.os.NetworkOnMainThreadException; better refactor towards AsyncTask */
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy); //Min API = 9 (was 8)
			
	        ZMQ.Context context = ZMQ.context(1); // create globally?!?
	        publisher = context.socket(ZMQ.PUB);
	        publisher.bind(this.zmqConnectionSpecification);
		}
		
		@Override
		public int write(byte[] dataBuffer, int dataLength) {

			try {
				publisher.send(dataBuffer);				
			} catch (Exception e) {
				Log.w("WorldModelUpdatesBroadcaster", e);
			}
			return 0;
		}	
	}

}
