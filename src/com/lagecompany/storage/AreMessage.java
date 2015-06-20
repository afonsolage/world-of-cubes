package com.lagecompany.storage;

public enum AreMessage {

    MOVE;
    private Object data;

    public Object getData() {
	return data;
    }

    public void setData(Object data) {
	this.data = data;
    }
}