/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lagecompany.woc.storage.light;

/**
 *
 * @author TI.Afonso
 */
public class LightData {

    private static final int VERTEX_COUNT = 4;
    public static final LightData EMPTY = new LightData(-1);

    public int index;
    public float[] data;

    public LightData(int index) {
        this.index = index;
        data = new float[24]; //6 sides x 4 vertex;
    }

    public void setVertexData(int side, int vertex, float value) {
        this.data[side * VERTEX_COUNT + vertex] = value;
    }

    public float getVertexData(int side, int vertex) {
        return this.data[side * VERTEX_COUNT + vertex];
    }

    public void getSide(int side, float[] buffer) {
        System.arraycopy(data, side * VERTEX_COUNT, buffer, 0, VERTEX_COUNT); //6 sides
    }

    public boolean compare(LightData other, int side) {
        int offset = side * VERTEX_COUNT;
        for (int i = 0; i < VERTEX_COUNT; i++) {
            if (data[offset + i] != other.data[offset + i]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + this.index;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LightData other = (LightData) obj;
        if (this.index != other.index) {
            return false;
        }
        return true;
    }
}
