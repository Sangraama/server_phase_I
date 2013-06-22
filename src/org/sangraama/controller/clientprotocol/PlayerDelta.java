package org.sangraama.controller.clientprotocol;

public class PlayerDelta {
    private int type=1;
    private float dx, dy;
    private long userID;

    public PlayerDelta(float dx, float dy, long userID) {
	this.dx = dx;
	this.dy = dy;
	this.userID = userID;
    }
    
    public long getUserID(){
	return this.userID;
    }

    public float getDx() {
	return dx;
    }

    public void setDx(int dx) {
	this.dx = dx;
    }

    public float getDy() {
	return dy;
    }

    public void setDy(int dy) {
	this.dy = dy;
    }
}
