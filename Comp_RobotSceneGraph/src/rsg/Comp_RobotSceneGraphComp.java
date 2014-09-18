/**
LooCI Copyright (C) 2013 KU Leuven.
All rights reserved.

LooCI is an open-source software development kit for developing and maintaining networked embedded applications;
it is distributed under a dual-use software license model:

1. Non-commercial use:
Non-Profits, Academic Institutions, and Private Individuals can redistribute and/or modify LooCI code under the terms of the GNU General Public License version 3, as published by the Free Software Foundation
(http://www.gnu.org/licenses/gpl.html).

2. Commercial use:
In order to apply LooCI in commercial code, a dedicated software license must be negotiated with KU Leuven Research & Development.

Contact information:
  Administrative Contact: Sam Michiels, sam.michiels@cs.kuleuven.be
  Technical Contact:           Danny Hughes, danny.hughes@cs.kuleuven.be
Address:
  iMinds-DistriNet, KU Leuven
  Celestijnenlaan 200A - PB 2402,
  B-3001 Leuven,
  BELGIUM. 
 */
/*
 * Copyright (c) 2012, Katholieke Universiteit Leuven
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Katholieke Universiteit Leuven nor the names of
 *       its contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package rsg;

import java.util.ArrayList;
import java.util.HashMap;

import be.kuleuven.mech.rsg.Attribute;
import be.kuleuven.mech.rsg.Box;
import be.kuleuven.mech.rsg.HomogeneousMatrix44;
import be.kuleuven.mech.rsg.IOutputPort;
import be.kuleuven.mech.rsg.Id;
import be.kuleuven.mech.rsg.Rsg;
import be.kuleuven.mech.rsg.SceneObject;
import be.kuleuven.mech.rsg.Sphere;
import be.kuleuven.mech.rsg.jni.RsgJNI;
import looci.osgi.serv.components.Event;
import looci.osgi.serv.constants.EventTypes;
import looci.osgi.serv.impl.LoociComponent;
import looci.osgi.serv.impl.PayloadBuilder;
import looci.osgi.serv.util.Utils;

/**
 * 
 *
 * @author 
 * @version 1.0
 * @since 2012-01-01
 *
 */

public class Comp_RobotSceneGraphComp extends LoociComponent {

	Thread listenerThread = null;
	
	String logTag = "YouBotWorldModel";
	SceneObject virtualFence = null;
	SceneObject obstacle = null;
	Box fenceBox = null;
	HomogeneousMatrix44 obstaclePose = null;
	Sphere obstacleShape = null; 
	
    /**
     * Holds the parent codebase
     */
    private Comp_RobotSceneGraph _parent;



	/**
	 * LooCIComponent(<name>, <provided interfaces>, <required interfaces>);
	 */
    public Comp_RobotSceneGraphComp(Comp_RobotSceneGraph parent) {
        _parent = parent;
    }

    @Override
    public void receive(short event_id, byte[] payload) {		
        // handle receptacles
    	
    	/* Receive data */
		byte[] message = payload; // null Pointer
		String text = new String(message, 0, message.length);
		System.out.println(logTag + "message: with length" +  message.length + " = " + text);
		
		/* Process data */
		int processedBytes = RsgJNI.writeUpdateToInputPort(message, message.length);
		 
		/* Inform GUI */
		onWorldModelUpdate();
    }
    
    @Override
    public void componentStart() {
        // called by looci:activate
    	initializeWorldModel();
    }

    @Override
    public void componentStop() {
    	// called by looci:deactivate
    }
    
    public void initializeWorldModel() {    	
		Rsg.initializeWorldModel(); // always start with that one.

		WorldModelUpdatesBroadcaster outputPort = new WorldModelUpdatesBroadcaster();
		Rsg.setOutPort(outputPort);
		
		virtualFence = new SceneObject();
		obstacle = new SceneObject(); 

		fenceBox = new Box(5, 6.1, 0);//[m] 
		virtualFence.addBox(fenceBox);								
		System.out.println(logTag + "Box = " + fenceBox.getSizeX() + ", " + fenceBox.getSizeY() + ", " + fenceBox.getSizeZ());			
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
		System.out.println(logTag + "Spere = " + obstacleShape.getRadius());   


		Id fenceId = Rsg.addSceneObject(virtualFence);
		System.out.println(logTag + "Added virtualFence with ID = " + fenceId.toString());  
		Id obstacleId = Rsg.addSceneObject(obstacle);
		System.out.println(logTag + "Added obstaceId with ID = " + obstacleId.toString());  
				
		displayObstacleCoordinates();
		
		
		ArrayList<Attribute> emptyAttributes = new ArrayList<Attribute>();
		ArrayList<SceneObject> foundAllSceneOjects = Rsg.getSceneObjects(emptyAttributes);
		System.out.println(logTag + "Result (all) = found " + foundAllSceneOjects.size() + " Scene object(s)");
		printSceneObjects(foundAllSceneOjects);
	

		ArrayList<SceneObject> foundSceneOjects = Rsg.getSceneObjects(attributes);
		System.out.println(logTag + "Result = found " + foundSceneOjects.size() + " Scene object(s)");
		printSceneObjects(foundSceneOjects);

		/* Move obstacle a bit */
		HomogeneousMatrix44 obstaclePoseUpdate = new HomogeneousMatrix44(
				1, 0, 0, // rotation  
				0, 1, 0, 
				0, 0, 1,
				3.3, 4.4, 0); // translation
		Rsg.insertTransform(obstacleId, obstaclePoseUpdate);  
		
		//displayObstacleCoordinates();
		
		
		/* Move obstacle a bit */
		HomogeneousMatrix44 obstaclePoseUpdate2 = new HomogeneousMatrix44(
				1, 0, 0, // rotation  
				0, 1, 0, 
				0, 0, 1,
				3.4, 4.5, 0); // translation
		Rsg.insertTransform(obstacleId, obstaclePoseUpdate2);   
		
		foundAllSceneOjects.clear();
		foundAllSceneOjects = Rsg.getSceneObjects(emptyAttributes);
		System.out.println(logTag + "Result (all;again) = found " + foundAllSceneOjects.size() + " Scene object(s)");
		printSceneObjects(foundAllSceneOjects);
		
		byte[] testData = new byte[8];  
		testData[0] = (byte)0xDE;   
		testData[1] = (byte)0xAF;     
		testData[2] = (byte)0xBE;  
		testData[3] = (byte)0xAF;
		testData[4] = (byte)'Q';
		int processedBytes = RsgJNI.writeUpdateToInputPort(testData, testData.length);
				
		displayObstacleCoordinates();
		
		System.out.println(logTag + "Done.");
	}

    public void printSceneObjects(ArrayList<SceneObject> sceneOjects) {
		for (SceneObject sceneObject : sceneOjects) {

			/* Just print everything */
			System.out.println(logTag + "	Scene Object has ID = " + sceneObject.id);
			System.out.println(logTag + "	Scene Object has parentId = " + sceneObject.parentId);

			System.out.println(logTag + "	Scene Object has position (x,y,z) = (" 
					+ sceneObject.getTransform().getX() + ", " 
					+ sceneObject.getTransform().getY() + ", " 
					+ sceneObject.getTransform().getZ() + ")");

			if(sceneObject.getBox() != null) {
				System.out.println(logTag + "	Scene Object has a box shape (x,y,z) =  (" 
						+ sceneObject.getBox().getSizeX() + ", "
						+ sceneObject.getBox().getSizeY() + ", "
						+ sceneObject.getBox().getSizeZ() + ")");
			} else if (sceneObject.getSphere() != null) {
				System.out.println(logTag + "	Scene Object has a shere shape (radius) =  (" 
						+ sceneObject.getSphere().getRadius() + ")");
			} else {
				System.out.println(logTag + "	Scene Object has unkonwn shape.");
			}

			for (Attribute a : sceneObject.getAttributes()) {
				System.out.println(logTag + "	Scene Object has a attribute: " + a.toString()); 
			}
			
			System.out.println(logTag + "	------------");
		}
	}
    
    public void displayObstacleCoordinates() {
		double x = -1.0;
		double y = -1.0;
		ArrayList<Attribute> queryAttributes = new ArrayList<Attribute>();
		queryAttributes.add(new Attribute("name", "obstacle"));
		ArrayList<SceneObject> foundSceneOjects = Rsg.getSceneObjects(queryAttributes);
		System.out.println("displayObstacleCoordinates: Result (obsatcles) = found " + foundSceneOjects.size() + " Scene object(s)");
		
		if(foundSceneOjects.size() > 0) {
			SceneObject obstacle = foundSceneOjects.get(0);
			x = obstacle.getTransform().getX();
			y = obstacle.getTransform().getY();
			System.out.println("displayObstacleCoordinates: (x,y) = (" + x + ", " + y + ")");
		}
		
		//xValueText.setText(String.format("%2.2f", x));
		//yValueText.setText(String.format("%2.2f", y));
	
		queryAttributes.clear();
		ArrayList<SceneObject> allFoundSceneOjects = Rsg.getSceneObjects(queryAttributes);
		//numberOfObjectsText.setText(String.format("%d", allFoundSceneOjects.size()));
	}
    
    /* Callback for changes of the world model */
	public void onWorldModelUpdate() {
		System.out.println(logTag + "onWorldModelUpdate()");  

		/* Get all curretn scene objects */
		ArrayList<Attribute> emptyAttributes = new ArrayList<Attribute>();
		ArrayList<SceneObject> foundSceneOjects = Rsg.getSceneObjects(emptyAttributes);
		System.out.println(logTag + "There are " + foundSceneOjects.size() + " Scene object(s).");
		printSceneObjects(foundSceneOjects);
		
		/* Display (some) values on GUI */
		//numberOfObjectsText.setText(String.format("%d", foundSceneOjects.size()));

		
	}
    
    /**
	 * ZMQ based communication mechanism for receiving world model update messages.
	 */
	
	public class WorldModelUpdatesBroadcaster /*extends AsyncTask*/ implements IOutputPort {
				
		public WorldModelUpdatesBroadcaster() {
			
		}
				
		@Override
		public int write(byte[] dataBuffer, int dataLength) {
			Event e = new Event(EventTypes.STRING_EVENT,dataBuffer);
			publish(e);
			
			return 0;
		}	
	}
}
