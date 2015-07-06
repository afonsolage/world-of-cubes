package com.lagecompany.storage;

import com.lagecompany.util.ArrayUtils;
import java.util.ArrayList;
import java.util.List;

public class ChunkSideBuffer {

    public class ChunkData {

	private byte side;
	private short type;
	private float[] buffer;

	private ChunkData(short type, byte side) {
	    this.side = side;
	    this.type = type;
	    this.buffer = new float[0];
	}
    }
    private List<ChunkData> dataList;

    public ChunkSideBuffer() {
	dataList = new ArrayList<>();
    }

    boolean isEmpty() {
	return dataList.isEmpty();
    }

    public int size() {
	int result = 0;

	for (ChunkData data : dataList) {
	    result += data.buffer.length;
	}

	return result;
    }

    public int size(short type) {
	int result = 0;

	for (ChunkData data : dataList) {
	    if (data.type == type) {
		result += data.buffer.length;
	    }
	}

	return result;
    }

    public int size(byte side) {
	int result = 0;

	for (ChunkData data : dataList) {
	    if (data.side == side) {
		result += data.buffer.length;
	    }
	}

	return result;
    }

    public float[] get(byte side) {
	float[] result = new float[size(side)];

	int i = 0;
	for (ChunkData data : dataList) {
	    if (data.side == side && data.buffer.length > 0) {
		System.arraycopy(data.buffer, 0, result, i, data.buffer.length);
		i += data.buffer.length;
	    }
	}

	return result;
    }

    public float[] get() {
	float[] result = new float[size()];

	int i = 0;
	for (ChunkData data : dataList) {
	    System.arraycopy(data.buffer, 0, result, i, data.buffer.length);
	    i += data.buffer.length;
	}

	return result;
    }

    private ChunkData getData(short type, byte side) {
	for (ChunkData data : dataList) {
	    if (type == data.type && side == data.side) {
		return data;
	    }
	}
	return null;
    }

    private ChunkData safeGetData(short type, byte side) {
	ChunkData result = getData(type, side);

	if (result == null) {
	    result = new ChunkData(type, side);
	    dataList.add(result);
	}

	return result;
    }

    public void add(short type, byte side, float[] buffer) {
	ChunkData data = safeGetData(type, side);
	data.buffer = ArrayUtils.append(buffer, data.buffer);
    }

    public float[] get(short type, byte side) {
	ChunkData data = getData(type, side);
	return (data == null) ? null : data.buffer;
    }
}
