package com.lagecompany.woc.storage;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.lagecompany.woc.storage.voxel.Voxel;
import com.lagecompany.woc.util.ArrayUtils;

public class ChunkSideBuffer {

    public static final float[] EMPTY_FLOAT_BUFFER = new float[]{};
    public static final int[] EMPTY_INT_BUFFER = new int[]{};

    void clear() {
        dataList.clear();
    }

    public class ChunkData {

        private final byte side;
        private final short type;
        private final float[] lightData;
        private float[] buffer;

        private ChunkData(short type, float[] lightData, byte side) {
            this.side = side;
            this.type = type;
            this.lightData = new float[4];
            this.buffer = new float[0];

            System.arraycopy(lightData, 0, this.lightData, 0, 4); //4 Vertex per side.
        }
    }
    private final List<ChunkData> dataList;

    public ChunkSideBuffer() {
        dataList = new CopyOnWriteArrayList<>();
    }

    public void reset() {
        dataList.clear();
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

        if (i == 0) {
            System.out.println("Returning empty buffer.");
        }

        return result;
    }

    private ChunkData getData(short type, float[] lightData, byte side) {
        for (ChunkData data : dataList) {
            if (type == data.type && side == data.side && Arrays.equals(lightData, data.lightData)) {
                return data;
            }
        }
        return null;
    }

    private ChunkData safeGetData(short type, float[] lightData, byte side) {
        ChunkData result = getData(type, lightData, side);

        if (result == null) {
            result = new ChunkData(type, lightData, side);
            dataList.add(result);
        }

        return result;
    }

    public void add(short type, float[] lightData, byte side, float[] buffer) {
        ChunkData data = safeGetData(type, lightData, side);
        data.buffer = ArrayUtils.append(buffer, data.buffer);
    }

    public float[] get(short type, float[] lightData, byte side) {
        ChunkData data = getData(type, lightData, side);
        return (data == null) ? null : data.buffer;
    }

    /**
     * Generate the vertex normal list.
     *
     * @return
     */
    public float[] getNormalList() {
        if (isEmpty()) {
            return EMPTY_FLOAT_BUFFER;
        }

        float[] r = new float[size()]; //Each vertex needs a normal;
        int n = 0;

        for (ChunkData data : dataList) {
            for (int i = 0, size = data.buffer.length; i < size;) {
                switch (data.side) {
                    case Voxel.VS_FRONT: {
                        r[n++] = 0;
                        r[n++] = 0;
                        r[n++] = 1;
                        break;
                    }
                    case Voxel.VS_RIGHT: {
                        r[n++] = 1;
                        r[n++] = 0;
                        r[n++] = 0;
                        break;
                    }
                    case Voxel.VS_BACK: {
                        r[n++] = 0;
                        r[n++] = 0;
                        r[n++] = -1;
                        break;
                    }
                    case Voxel.VS_LEFT: {
                        r[n++] = -1;
                        r[n++] = 0;
                        r[n++] = 0;
                        break;
                    }
                    case Voxel.VS_TOP: {
                        r[n++] = 0;
                        r[n++] = 1;
                        r[n++] = 0;
                        break;
                    }
                    case Voxel.VS_DOWN: {
                        r[n++] = 0;
                        r[n++] = -1;
                        r[n++] = 0;
                        break;
                    }
                }
                i += 3;
            }
        }
        return r;
    }

    /**
     * Generate the vertex index list.
     *
     * @return vertex index list.
     */
    public int[] getIndexList() {
        if (isEmpty()) {
            return EMPTY_INT_BUFFER;
        }

        //Each side has 4 vertex, with 3 floats each which makes 12 floats.
        //We need 6 index for each side, so need the half size.
        int[] result = new int[(int) (size() / 2)];
        int n = 0;

        /*  Vertexes are built using the counter-clockwise, we just need to follow this index pattern:
         *		     3		2   2
         *		     +--------+    + 
         *		     |       /   / |
         *		     |     /   /   |
         *		     |   /   /     |
         *		     | /   /	   |
         *		     +   +---------+
         *		    0    0	    1
         */
        for (int i = 0, size = result.length; i < size;) {
            result[i++] = n; //0
            result[i++] = n + 1; //1
            result[i++] = n + 2; //2
            result[i++] = n + 2;   //2
            result[i++] = n + 3; //3
            result[i++] = n; //0

            n += 4;
        }

        return result;
    }

    public float[] getTextCoord() {
        if (isEmpty()) {
            return ChunkSideBuffer.EMPTY_FLOAT_BUFFER;
        }

        //A vertex is made of 3 floats.
        int vertexCount = size() / 3;
        float[] r = new float[vertexCount * 2]; //Each vertex needs a UV text coord;

        float x1, y1, z1;
        float x2, y2, z2;
        float x4, y4, z4;

        float xTile;
        float yTile;
        int j = 0;
        for (ChunkData data : dataList) {
            for (int i = 0, size = data.buffer.length; i < size;) {
                x1 = data.buffer[i++];
                y1 = data.buffer[i++];
                z1 = data.buffer[i++];

                x2 = data.buffer[i++];
                y2 = data.buffer[i++];
                z2 = data.buffer[i++];

                //skip v3
                i += 3;

                x4 = data.buffer[i++];
                y4 = data.buffer[i++];
                z4 = data.buffer[i++];

                xTile = Math.abs(x1 - x2) + Math.abs(y1 - y2) + Math.abs(z1 - z2);
                yTile = Math.abs(x1 - x4) + Math.abs(y1 - y4) + Math.abs(z1 - z4);

                r[j++] = xTile;
                r[j++] = 0f;
                r[j++] = 0f;
                r[j++] = 0f;
                r[j++] = 0f;
                r[j++] = yTile;
                r[j++] = xTile;
                r[j++] = yTile;
            }
        }

        return r;
    }

    public float[] getTexColor() {
        if (isEmpty()) {
            return EMPTY_FLOAT_BUFFER;
        }

        //Since each vertex, which has 3 floats, needs a color, we need 4 floats per vertex.
        float[] r = new float[(int) (size() * 1.33333333333f)];
        int offset = 0;//, vertexIndex, aoFactor;
        int vertexCount = 0;
        float vertexLightFactor;

        for (ChunkData data : dataList) {
            for (int i = 0, size = data.buffer.length; i < size;) {
                System.arraycopy(Voxel.getColor(data.type, data.side), 0, r, offset, 4);

                if (data.lightData != null) {
                    vertexLightFactor = data.lightData[vertexCount % 4] * 0.10f;
                } else {
                    vertexLightFactor = 0;
                }

                r[offset] *= vertexLightFactor;
                r[offset + 1] *= vertexLightFactor;
                r[offset + 2] *= vertexLightFactor;

                vertexCount++;
                i += 3;
                offset += 4;
            }
        }
        return r;
    }

    public float[] getTileCoord() {
        if (isEmpty()) {
            return EMPTY_FLOAT_BUFFER;
        }

        //A vertex is made of 3 floats.
        float[] tmp = get();
        int vertexCount = tmp.length / 3;

        float[] r = new float[vertexCount * 2]; //Each vertex needs a UV tile coord;
        int offset = 0;

        for (ChunkData data : dataList) {
            for (int i = 0, size = data.buffer.length; i < size;) {
                System.arraycopy(Voxel.getTile(data.type, data.side), 0, r, offset, 2);
                i += 3;
                offset += 2;
            }
        }
        return r;
    }
}
