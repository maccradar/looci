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
package broker;

import org.zeromq.ZMQ;

public class Broker0mqListener extends Thread{

	private int port = 30333;
	private Broker0mqComp inst;
	
	public Broker0mqListener(Broker0mqComp inst){
		this.inst = inst;
	}
	
	@Override
	public void run() {
		ZMQ.Context context = ZMQ.context(1);

        //  Socket to talk to clients
        ZMQ.Socket socket = context.socket(ZMQ.REP);
        System.out.println("Binding to socket tcp://*:"+String.valueOf(this.port));
        socket.bind ("tcp://*:"+String.valueOf(this.port));

        while (!Thread.currentThread ().isInterrupted ()) {
        	System.out.println("Waiting for data...");
            byte[] reply = socket.recv(0);
            System.out.println("Received " + ": [" + new String(reply, ZMQ.CHARSET) + "]");

            //  Create a "Hello" message.
            String request = "world" ;
            // Send the message
            socket.send(request.getBytes (ZMQ.CHARSET), 0);

            try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} //  Do some 'work'
        }
        
        socket.close();
        context.term();
    }
		
	public synchronized void publishEvent(String data){
		
	}
	
	public synchronized void close(){
		
		
	}	
	
}
