package com.lagecompany.storage;

import java.util.concurrent.BlockingQueue;

public class AreWorker extends Thread {

    BlockingQueue<AreMessage> actionQueue;
    Are are;
    
    public AreWorker(BlockingQueue<AreMessage> queue, Are are) {
	this.actionQueue = queue;
	this.are = are;
    }

    public void postMessage(AreMessage msg) {
	if (!actionQueue.offer(msg)) {
	    System.out.println(String.format("Failed to add message: %s", msg));
	}
    }

    @Override
    public void run() {
	while (!Thread.currentThread().isInterrupted()) {
	    try {
		AreMessage msg = actionQueue.take();
		
		switch(msg.getType()) {
		    case ARE_MOVE: {
//			are.move(msg);
			break;
		    }
		}
		
	    } catch (InterruptedException ex) {
		return;
	    }
	}
    }
}
