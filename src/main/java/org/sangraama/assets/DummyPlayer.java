package org.sangraama.assets;

import org.sangraama.controller.WebSocketConnection;
import org.sangraama.coordination.staticPartition.TileCoordinator;
import org.sangraama.gameLogic.queue.DummyQueue;
import org.sangraama.jsonprotocols.SendProtocol;
import org.sangraama.jsonprotocols.send.SyncPlayer;
import org.sangraama.jsonprotocols.transfer.ScoreChangeTransferReq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * **************************************************************************
 * Player class have the main functionality of getting updates of the game world.
 * It is not authenticate for responding to client events. Dummy Player does
 * not have body and fixture.
 *
 * @version : v1.2
 * @author: Gihan Karunarathne
 * @email: gckarunarathne@gmail.com
 * Date: 12/5/2013 4:00 PM
 * ***************************************************************************
 */
public class DummyPlayer extends AbsPlayer {

    private static final Logger log = LoggerFactory.getLogger(DummyPlayer.class);
    private DummyQueue dummyQueue;

    /**
     * Create a dummy player in order to get updates to fulfill the player's AOI in client side
     *
     * @param userID userID of the player in server side
     * @param w      width of player's AOI
     * @param h      height of player's AOI
     * @param con    web socket Connection
     */
    public DummyPlayer(long userID, float w, float h, WebSocketConnection con) {
        super(userID, 0.0f, 0.0f, w, h);
        isPlayer = 2;
        this.con = con;
        this.dummyQueue = DummyQueue.INSTANCE;
        this.dummyQueue.addToDummyQueue(this);
    }

    /*
     * This method isn't secure. Have to inherit from a interface both this and WebSocketConnection
     */
    public void removeWebSocketConnection() {
        this.con = null;
        this.dummyQueue.addToRemoveDummyQueue(this);
    }

    /**
     * Check whether given location is inside current tile (map of current server)
     *
     * @param x Player's current x coordination
     * @param y Player's current y coordination
     * @return if inside tile return true, else false
     */
    private boolean isInsideMap(float x, float y) {
        return (sangraamaMap.getOriginX() <= x && x <= sangraamaMap.getEdgeX()
                && sangraamaMap.getOriginY() <= y && y <= sangraamaMap.getEdgeY());
    }

    /**
     * Check whether player is inside current sub-tile
     *
     * @param x Player's current x coordination
     * @param y Player's current y coordination
     * @return if inside sub-tile return true, else false
     */
    private boolean isInsideServerSubTile(float x, float y) {
        boolean insideServerSubTile = true;
        float subTileOriX = x - (x % sangraamaMap.getSubTileWidth());
        float subTileOriY = y - (y % sangraamaMap.getSubTileHeight());
        // System.out.println(TAG + currentSubTileOriginX + ":" + currentSubTileOriginY + " with "
        // + subTileOriX + ":" + subTileOriY);
        if (currentSubTileOriginX != subTileOriX || currentSubTileOriginY != subTileOriY) {
            currentSubTileOriginX = subTileOriX;
            currentSubTileOriginY = subTileOriY;
            if (!sangraamaMap.getHost().equals(TileCoordinator.INSTANCE.getSubTileHost(x, y))) {
                insideServerSubTile = false;
                // log.info("player is not inside a subtile of " + sangraamaMap.getHost());
            }
        }

        return insideServerSubTile;
    }

    public void reqInterestIn(float x, float y) {
        /*
         * Just ignore
         */
    }

    public void sendUpdate(List<SendProtocol> deltaList) {
        if (this.con != null) {
            try {
                this.con.sendUpdate(deltaList);
            } catch (IOException e) {
                this.dummyQueue.addToRemoveDummyQueue(this);
                this.isPlayer = 0;
                e.printStackTrace();
            }

        } else if (this.isPlayer == 2) {
            this.dummyQueue.addToRemoveDummyQueue(this);
            this.isPlayer = 0;
            log.warn("Unable to send updates,coz con :" + con + ". Add to remove queue.");
        } else {
            log.error("waiting for remove");
        }
    }

    public void sendPassConnectionInfo(SendProtocol transferReq) {
        /* dummy can't pass the player : no implementation */
    }

    /**
     * TODO: Need refactoring code
     */
    public void sendUpdateConnectionInfo(SendProtocol transferReq) {
        if (this.con != null) {
            ArrayList<SendProtocol> transferReqList = new ArrayList<>();
            transferReqList.add(transferReq);
            this.con.sendNewConnection(transferReqList);
        } else if (isPlayer == 2) {
            this.dummyQueue.addToRemoveDummyQueue(this);
            this.isPlayer = 0;
            log.warn("Unable to send new connection,coz con :" + con + ". Add to remove queue.");
        } else {
            log.error("waiting for remove");
        }
    }

    public void sendSyncData(List<SendProtocol> syncData) {
        if (this.con != null) {
            try {
                this.con.sendUpdate(syncData);
            } catch (IOException e) {
                this.dummyQueue.addToRemoveDummyQueue(this);
                this.isPlayer = 0;
                e.printStackTrace();
            }
        } else if (this.isPlayer == 2) {
            this.dummyQueue.addToRemoveDummyQueue(this);
            this.isPlayer = 0;
            log.warn("Unable to send sync data,coz con :" + con + ". Add to remove queue.");
        } else {
            log.error("waiting for remove");
        }
    }

    /**
     * ***************************
     * Getter Setter methods     *
     * ***************************
     */

    public boolean setVirtualPoint(float x_vp, float y_vp) {
        /*
         * Validate data before set virtual point. Idea: Virtual point can't go beyond edges of Full
         * map (the map which divide into sub tiles) with having half of the size of AOI. Then
         * possible virtual point setting will validate by server side. #gihan
         */
        // log.info("want to set vp x:" + x_vp + " y:" + y_vp);

        /*if (this.x_virtual == x_vp && this.y_virtual == y_vp)
            return false;*/
        this.x_virtual = x_vp;
        this.y_virtual = y_vp;

        /**
         * Check whether AOI is inside the map or not (4 corners are inside current map)
         */
        if (isInsideMap(x_vp - halfAOIWidth, y_vp - halfAOIHieght)
                || isInsideMap(x_vp + halfAOIWidth, y_vp - halfAOIHieght)
                || isInsideMap(x_vp - halfAOIWidth, y_vp + halfAOIHieght)
                || isInsideMap(x_vp + halfAOIWidth, y_vp + halfAOIHieght)) {
            // if one of point is located in server, set virtual point

            List<SendProtocol> data = new ArrayList<>();
            // Send updates which are related/interest to dummy player
            data.add(new SyncPlayer(userID, x_virtual, y_virtual, screenWidth, screenHeight));
            // log.info("set Virtual point x" + x_vp + " y" + y_vp);
            this.sendSyncData(data);

            // Update values
            this.x_vp_l = x_virtual - halfAOIWidth;
            this.x_vp_r = x_virtual + halfAOIWidth;
            this.y_vp_u = y_virtual - halfAOIHieght;
            this.y_vp_d = y_virtual + halfAOIHieght;

        } else { // Otherwise drop the connection of getting updates
            List<SendProtocol> data = new ArrayList<>();
            // Send updates which are related/interest to closing a dummy player
            data.add(new SyncPlayer(userID));
            // log.info("Virtual point x" + x_vp + " y" + y_vp +
            // " is out from this map. Closing ... ");
            this.sendSyncData(data);
            con.closeConnection();
            this.dummyQueue.addToRemoveDummyQueue(this);
        }
        return false;
    }

    /**
     * Send score changed of player
     *
     * @param scoreChangeReq Score change transfer request
     */
    public void sendScoreChange(ScoreChangeTransferReq scoreChangeReq) {
        if (this.con != null) {
            ArrayList<SendProtocol> scoreChangeReqList = new ArrayList<>();
            scoreChangeReqList.add(scoreChangeReq);
            con.sendScoreChangeReq(scoreChangeReqList);
        }
    }

    /**
     * Setter methods which are not relevant to dummy player (but inherits)
     */

    public void setV(float x, float y) {
        /* Don't implement. Not relevant to dummy player */
    }

    public void setAngle(float angle) {
        /* Don't implement. Not relevant to dummy player */
    }

    public void setAngularVelocity(float da) {
        /* Don't implement. Not relevant to dummy player */
    }

    public void shoot(float s) {
        /* Don't implement. Not relevant to dummy player */
    }

}