package com.lagecompany.storage;

import java.util.Comparator;

/**
 *
 * @author afonsolage
 */
public class AreMessageComparator implements Comparator<AreMessage> {

    @Override
    public int compare(AreMessage msg1, AreMessage msg2) {
	if (msg1.getType() == msg2.getType() && msg1.getType() == AreMessage.AreMessageType.PROCESS_CHUNK) {
	    Chunk c1 = (Chunk) msg1.getData();
	    Chunk c2 = (Chunk) msg2.getData();

	    return c1.getFlag() - c2.getFlag();
	} else {
	    return msg1.getType().getPriority() - msg2.getType().getPriority();
	}
    }
}
