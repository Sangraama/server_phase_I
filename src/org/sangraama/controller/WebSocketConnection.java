package org.sangraama.controller;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.WsOutbound;
import org.sangraama.asserts.Player;
import org.sangraama.asserts.PlayerData;
import org.sangraama.common.Constants;

import com.google.gson.Gson;

public class WebSocketConnection extends MessageInbound {
    // Local Debug or logs
    private static boolean LL = true;
    private static boolean LD = true;
    private static final String TAG = "WebSocketConnection";

     Player player = null;
    
     public void setPlayer(Player player) {
     this.player = player;
     }

    // tmp test only
    PlayerData coord = null;

    @Override
    protected void onOpen(WsOutbound outbound) {
	//Constants.log.info("Open Connection");
	System.out.println("Open Connection");
    }

    @Override
    protected void onClose(int status) {
	Constants.log.info("Connection closed");
	System.out.println("Close connection");
    }

    @Override
    protected void onBinaryMessage(ByteBuffer byteBuffer) throws IOException {
	// log.warn("binary messages are not supported");
	System.out.println("Binary");
	throw new UnsupportedOperationException("not supported binary messages");
    }

    @Override
    protected void onTextMessage(CharBuffer charBuffer) throws IOException {
	Gson gson = new Gson();
	String user = charBuffer.toString();
	//Constants.log.debug("Received message: {}", user);
	System.out.println("REcieved msg :" + user);
	PlayerData coord = gson.fromJson(user, PlayerData.class);
	System.out.println("x:"+coord.getX()+" y:"+coord.getY());
	getWsOutbound().writeTextMessage(CharBuffer.wrap(gson.toJson(coord)));
	
	/*Player p = gson.fromJson(user, Player.class);
	this.player.setX(p.getX());
	this.player.setY(p.getY());
	this.player.se*/
	
    }

    public void sendUpdate(Player player) {
	Gson gson = new Gson();
	try {
	    getWsOutbound().writeTextMessage(
		    CharBuffer.wrap(gson.toJson(player)));
	} catch (IOException e) {
	    System.out.println(TAG + " Unable to send update");
	    Constants.log.error(TAG, e);
	}
    }

    public PlayerData getCoordinate() {
	if (coord != null) {
	    return this.coord;
	} else {
	    this.coord = new PlayerData(10,10);
	    return this.coord;
	}
    }
}