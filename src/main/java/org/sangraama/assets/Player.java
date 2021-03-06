package org.sangraama.assets;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.FixtureDef;
import org.sangraama.common.Constants;
import org.sangraama.controller.PlayerPassHandler;
import org.sangraama.controller.WebSocketConnection;
import org.sangraama.coordination.staticPartition.TileCoordinator;
import org.sangraama.gameLogic.queue.BulletQueue;
import org.sangraama.gameLogic.queue.PlayerQueue;
import org.sangraama.jsonprotocols.SendProtocol;
import org.sangraama.jsonprotocols.send.DefeatMsg;
import org.sangraama.jsonprotocols.send.PlayerDelta;
import org.sangraama.jsonprotocols.send.SyncPlayer;
import org.sangraama.jsonprotocols.send.VirtualPointAccessLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * **************************************************************************
 * Player class have the main functionality of a player in the game world.
 * It is also responsible for responding to client events. Player does not have
 * body and fixture. Users of this class is suppose to extend and implement those.
 *
 * @author : Gihan Karunarathne
 * @version : v1.2
 * @email : gckarunarathne@gmail.com
 * Date : 12/5/2013 4:00 PM
 * ***************************************************************************
 */
public abstract class Player extends AbsPlayer {

    private static final Logger log = LoggerFactory.getLogger(Player.class);
    static Random generator = new Random();
    Body body;
    // Player Dynamic Parameters
    float angle;// actual angle
    float oldAngle;// actual angle
    float health;
    float score;
    int a_rate = 2;  // angle acceleration rate
    float angularVelocity;
    int imgType;// image type of the player
    int bulletType;// bullet type of the player
    /* Player moving parameters */
    // Player speed factor
    float v_rate = 2.5f;
    float bullet_v_rate = 3.5f;
    Vec2 v = new Vec2(0.0f, 0.0f);
    PlayerDelta delta;
    private float subTileEdgeX = 0.0f; // Store value of subTileOriginX + subtileWidth
    private float subTileEdgeY = 0.0f; // Store value of subTileOriginY + subtileHeight

    /**
     * Create a player
     *
     * @param userID     player user ID
     * @param x          x coordinate of the player
     * @param y          y coordinate of the player
     * @param w          width of AOI
     * @param h          height of AOI
     * @param health     current health of the player
     * @param score      current score of the player
     * @param con        web socket connection with client
     * @param imgType    player's physical view in client side
     * @param bulletType bullet's physical view in client side
     */
    public Player(long userID, float x, float y, float w, float h, float health, float score,
                  WebSocketConnection con, int imgType, int bulletType) {
        super(userID, x, y, w, h);
        super.isPlayer = 1;
        super.con = con;
        /* Set sub tile edge values without method */
        this.subTileEdgeX = (x - (x % sangraamaMap.getSubTileWidth())) + sangraamaMap.getSubTileWidth();
        this.subTileEdgeY = (y - (y % sangraamaMap.getSubTileHeight())) + sangraamaMap.getSubTileHeight();
        this.health = health;
        this.score = score;
        PlayerQueue.INSTANCE.addToPlayerQueue(this);
        this.imgType = imgType;
        this.bulletType = bulletType;
    }

    /**
     * Generate and get delta of player updates (the among of change from previous data)
     *
     * @return Player's delta updates
     */
    public PlayerDelta getPlayerDelta() {
        /*if ((this.body.getPosition().x - this.x) != 0f || (this.body.getPosition().y - this.y) != 0) {

            System.out.print("id : " + this.userID + " x:" + x * Constants.scale + " y:" +
                    y * Constants.scale + " angle:" + this.body.getAngle() + " & " +
                    this.body.getAngularVelocity() + " # ");
            System.out.println(" x_virtual:" + this.x_virtual * Constants.scale + " y_virtual:" + this.y_virtual * Constants.scale);

        }*/

        // this.delta = new PlayerDelta(this.body.getPosition().x - this.x,
        // this.body.getPosition().y - this.y, this.userID);
        this.delta = new PlayerDelta(this.body.getPosition().x, this.body.getPosition().y,
                this.body.getAngle(), this.userID, this.health, this.score, this.imgType);
        /*
         * for (Bullet bullet : this.removedBulletList) {
         * delta.getBulletDeltaList().add(bullet.getBulletDelta(2)); }
         */
        this.x = this.body.getPosition().x;
        this.y = this.body.getPosition().y;
        this.oldAngle = this.body.getAngle() % 360;
        // Check whether player is inside the tile or not
        /*
         * Gave this responsibility to client if (!this.isInsideMap(this.x, this.y)) {
         * PlayerPassHandler.INSTANCE.setPassPlayer(this); }
         */

        if (!isInsideServerSubTile(this.x, this.y)) {
            PlayerPassHandler.INSTANCE.setPassPlayer(this);
            // log.info(userID + " outside of the subtile detected");
        }
        return this.delta;
    }

    /**
     * Apply new player events to the game world object
     */
    public void applyUpdate() {
        this.body.setLinearVelocity(this.getV());
        this.body.setAngularVelocity(0.0f);
        if (this.angularVelocity == 0) {
            this.body.setTransform(this.body.getPosition(), this.angle);
        } else {
            this.body.setTransform(this.body.getPosition(), this.oldAngle + this.angularVelocity);
        }

    }

    public void removeWebSocketConnection() {
        con = null;
    }

    /**
     * Check whether player is inside current tile
     *
     * @param x Player's current x coordination
     * @param y Player's current y coordination
     * @return if inside tile return true, else false
     */
    private boolean isInsideMap(float x, float y) {
        // System.out.println(TAG + userID + " is inside "+x+":"+y);
        if (sangraamaMap.getOriginX() <= x && x <= sangraamaMap.getEdgeX()
                && sangraamaMap.getOriginY() <= y && y <= sangraamaMap.getEdgeY()) {
            return true;
        } else {
            log.info(userID + " Outside of map : " + sangraamaMap.getEdgeX() + ":"
                    + sangraamaMap.getEdgeY());
            return false;
        }
    }

    /**
     * Check whether player is inside current sub-tile
     *
     * @param x Player's current x coordination
     * @param y Player's current y coordination
     * @return if inside sub-tile return true, else false
     */
    protected boolean isInsideServerSubTile(float x, float y) {
        if (currentSubTileOriginX <= x && x <= this.subTileEdgeX && currentSubTileOriginY <= y
                && y <= this.subTileEdgeY) { // true if player is in current sub tile
            return true;
        } else { // execute when player isn't in the current sub tile
            // Assign new sub tile origin coordinates
            currentSubTileOriginX = x - (x % sangraamaMap.getSubTileWidth());
            currentSubTileOriginY = y - (y % sangraamaMap.getSubTileHeight());
            this.setSubTileEgdeValues(); // update edge values
            // check whether players coordinates are in current map
            if (!sangraamaMap.getHost().equals(TileCoordinator.INSTANCE.getSubTileHost(x, y))) {
                log.info(userID + " player is not inside a sub tile of " + sangraamaMap.getHost());
                return false;
            }
            return true;
        }
    }

    public void reqInterestIn(float x, float y) {
        if (!isInsideServerSubTile(x, y) && isInsideTotalMap(x, y)) {
            PlayerPassHandler.INSTANCE.setPassConnection(x, y, this);
        }
    }

    public void sendUpdate(List<SendProtocol> deltaList) {
        if (this.con != null) {
            try {
                con.sendUpdate(deltaList);
            } catch (IOException e) {
                PlayerQueue.INSTANCE.addToRemovePlayerQueue(this);
                this.isPlayer = 0;
                e.printStackTrace();
            }
        } else if (this.isPlayer == 1) {
            PlayerQueue.INSTANCE.addToRemovePlayerQueue(this);
            this.isPlayer = 0;
            log.warn(userID + " Unable to send updates,coz con :" + this.con
                    + ". waiting for remove.");
        } else {
            log.error(userID + "  waiting for remove (1) id:" + userID + " player type:"
                    + super.isPlayer);
            PlayerQueue.INSTANCE.addToRemovePlayerQueue(this);
        }
    }

    /**
     * Send New connection Address and other details to Client
     *
     * @param transferReq Object of Client transferring protocol
     */
    public void sendPassConnectionInfo(SendProtocol transferReq) {
        if (super.con != null) {
            ArrayList<SendProtocol> transferReqList = new ArrayList<>();
            transferReqList.add(transferReq);
            con.sendNewConnection(transferReqList);
            /* Changed player type into dummy player and remove from the world */
            PlayerQueue.INSTANCE.addToRemovePlayerQueue(this);
            con.setDummyPlayer(new DummyPlayer(userID, screenWidth, screenHeight, con));
        } else if (super.isPlayer == 1) {
            PlayerQueue.INSTANCE.addToRemovePlayerQueue(this);
            super.isPlayer = 0;
            log.warn(userID + " Unable to send new connection,coz con :" + super.con
                    + ". Add to remove queue.");
        } else {
            log.error(userID + "  waiting for remove (2) id:" + userID + " player type:"
                    + super.isPlayer);
        }
    }

    /**
     * Send update server connection Address and other details to Client to fulfill the AOI
     *
     * @param transferReq Object of Client transferring protocol
     */
    public void sendUpdateConnectionInfo(SendProtocol transferReq) {
        if (this.con != null) {
            ArrayList<SendProtocol> transferReqList = new ArrayList<>();
            transferReqList.add(transferReq);
            con.sendNewConnection(transferReqList);
        } else if (super.isPlayer == 1) {
            PlayerQueue.INSTANCE.addToRemovePlayerQueue(this);
            super.isPlayer = 0;
            log.warn(userID + " Unable to send new connection,coz con :" + super.con
                    + ". Add to remove queue.");
        } else {
            log.error(userID + "  waiting for remove (3) id:" + userID + " player type:"
                    + super.isPlayer);
        }
    }

    /**
     * This method is used to send the information of the transferring object to the neighbor
     * server.
     *
     * @param transferReq message which contains the information of the transferring object
     */
    public void sendTransferringGameObjectInfo(SendProtocol transferReq) {
        if (this.con != null) {
            ArrayList<SendProtocol> transferReqList = new ArrayList<>();
            transferReqList.add(transferReq);
            con.sendPassGameObjInfo(transferReqList);
        }
    }

    public void sendSyncData(List<SendProtocol> syncData) {
        if (this.con != null) {
            try {
                con.sendUpdate(syncData);
            } catch (IOException e) {
                PlayerQueue.INSTANCE.addToRemovePlayerQueue(this);
                this.isPlayer = 0;
                e.printStackTrace();
            }
        } else if (super.isPlayer == 1) {
            PlayerQueue.INSTANCE.addToRemovePlayerQueue(this);
            this.isPlayer = 0;
            log.warn(userID + " Unable to send syncdata,coz con :" + this.con
                    + ". Add to remove queue.");
        } else {
            log.error(userID + "  waiting for remove (4) id:" + userID + " player type:"
                    + super.isPlayer);
        }
    }

    public void shoot(float s) {
        float r = 3;
        if (s == 1) {
            float x = this.body.getPosition().x;
            float y = this.body.getPosition().y;
            float bulletAngle = this.angle % 360;
            if (bulletAngle < 0) {
                bulletAngle += 360;
            }

            if (0 <= bulletAngle && bulletAngle <= 90) {
                float ang = bulletAngle * Constants.TO_RADIANS;
                float rX = (float) (r * Math.cos(ang));
                float rY = (float) (r * Math.sin(ang));
                x = x + rX;
                y = y + rY;
            } else if (90 <= bulletAngle && bulletAngle <= 180) {
                float ang = (180 - bulletAngle) * Constants.TO_RADIANS;
                float rX = (float) (r * Math.cos(ang));
                float rY = (float) (r * Math.sin(ang));

                x = x - rX;
                y = y + rY;
            } else if (180 <= bulletAngle && bulletAngle <= 270) {
                float ang = (bulletAngle - 180) * Constants.TO_RADIANS;
                float rX = (float) (r * Math.cos(ang));
                float rY = (float) (r * Math.sin(ang));

                x = x - rX;
                y = y - rY;
            } else if (270 <= bulletAngle && bulletAngle <= 360) {
                float ang = (360 - bulletAngle) * Constants.TO_RADIANS;
                float rX = (float) (r * Math.cos(ang));
                float rY = (float) (r * Math.sin(ang));

                x = x + rX;
                y = y - rY;
            }
            long id = (long) (generator.nextInt(10000));
            Vec2 bulletVelocity = new Vec2((x - this.body.getPosition().x) * this.bullet_v_rate,
                    (y - this.body.getPosition().y) * this.bullet_v_rate);
            Bullet bullet = new Bullet(id, this.userID, x, y, bulletVelocity,
                    this.body.getPosition().x, this.body.getPosition().y, this.getScreenWidth(),
                    this.getScreenHeight(), this.bulletType);
            BulletQueue.INSTANCE.addToBulletQueue(bullet);
            // log.info(userID + " : Added a new bullet");

        }
    }

    /**
     * Body definition of player
     *
     * @return Body definition
     */
    public abstract BodyDef getBodyDef();

    /**
     * Fixture definition of player
     *
     * @return fixture definition
     */
    public abstract FixtureDef getFixtureDef();

    /**
     * *****************************
     * Getters and Setters         *
     * *****************************
     */

    public boolean setVirtualPoint(float x_vp, float y_vp) {
        /*
         * Validate data before set virtual point. Idea: Virtual point can't go beyond edges of Full
         * map (the map which divide into sub tiles) with having half of the size of AOI. Then
         * possible virtual point setting will validate by server side. #gihan
         */
        // System.out.println(TAG + userID + "  want to set vp x:" + x_vp + " y:" + y_vp);

        /*
         * If asking for same virtual point, then ignore it
         */
        /*if (this.x_virtual == x_vp && this.y_virtual == y_vp)
            return false;*/

        this.x_virtual = x_vp;
        this.y_virtual = y_vp;

        /**
         * Check whether player is in permitted area [check by server]
         */
        VirtualPointAccessLevel vp_al = new VirtualPointAccessLevel();

        if (!isInsideTotalMap(x_vp, y_vp)) {
            if (x_vp < totOrgX) {
                this.x_virtual = totOrgX;
                /* X level restriction at origin Or left */
                vp_al.setVirtualPointAccessLevel('x', 1);
            }
            if (y_vp < totOrgY) {
                this.y_virtual = totOrgY;
                /* Y level restriction at origin or upper */
                vp_al.setVirtualPointAccessLevel('y', 1);
            }
            if (totEdgeX < x_vp) {
                this.x_virtual = totEdgeX;
                /* X level restriction at edge Or right */
                vp_al.setVirtualPointAccessLevel('x', 2);
            }
            if (totEdgeY < y_vp) {
                this.y_virtual = totEdgeY;
                /* Y level restriction at edge Or lower */
                vp_al.setVirtualPointAccessLevel('y', 2);
            }
            // log.info(userID + "  But set as vp x:" + x_vp + " y:" + y_vp);
        }

        List<SendProtocol> data = new ArrayList<>();
        data.add(new SyncPlayer(userID, x, y, x_virtual, y_virtual, angle, screenWidth,
                screenHeight, vp_al));
        // log.info(userID + " set Virtual point x:" + x_virtual + " y:" + y_virtual);
        this.sendSyncData(data);

        // Update values
        this.x_vp_l = x_virtual - halfAOIWidth;
        this.x_vp_r = x_virtual + halfAOIWidth;
        this.y_vp_u = y_virtual - halfAOIHieght;
        this.y_vp_d = y_virtual + halfAOIHieght;
        return true;
    }

    /**
     * Set values of the corner opposite to origin of subtile
     * Note: Efficient retrieve of data. Avoid recalculation
     */
    private void setSubTileEgdeValues() {
        this.subTileEdgeX = currentSubTileOriginX + sangraamaMap.getSubTileWidth();
        this.subTileEdgeY = currentSubTileOriginY + sangraamaMap.getSubTileHeight();
    }

    /**
     * Get body of player
     *
     * @return Body definition
     */
    public Body getBody() {
        return this.body;
    }

    /**
     * Set body of the player
     *
     * @param body body of the player
     */
    public void setBody(Body body) {
        this.body = body;
    }

    /**
     * Get velocity of the player
     *
     * @return velocity of player
     */
    public Vec2 getV() {
        return this.v;
    }

    public void setV(float x, float y) {
        // Fixed: if client send x value greater than 1
        this.v.set(x % 2 * v_rate, y % 2 * v_rate);
        // log.info(TAG + userID + "  set V :" + this.v.x + ":" + this.v.y);
    }

    public void setAngularVelocity(float da) {
        this.angularVelocity = da % 2 * a_rate;
        // log.info(TAG + userID + "  set angular velocity : " + da + " > "
        // + this.angularVelocity);
    }

    /**
     * Get absolute angle of player
     *
     * @return absolute angle of player in degrees
     */
    public float getAngle() {
        return angle;
    }

    public void setAngle(float a) {
        this.angle = a % 360;
        // log.info(TAG + userID + "  set angle : " + a + " > " + this.angle);
    }

    public float getHealth() {
        if (this.health < 0)
            return 0;
        else
            return this.health;
    }

    /**
     * Set health of the player
     *
     * @param healthChange health level
     */
    public void setHealth(float healthChange) {
        if ((this.health + healthChange) > 0) {
            this.health += healthChange;
        } else {
            this.health = 0;
            this.setScore(-200);
            PlayerQueue.INSTANCE.addToDefaetList(this);
        }
    }

    public float getScore() {
        if (this.score > 0) {
            return this.score;
        } else {
            return 0;
        }
    }

    /**
     * Set current score of the player
     *
     * @param scoreChange score value
     */
    public void setScore(float scoreChange) {
        if ((this.score + scoreChange) > 0) {
            this.score += scoreChange;
        } else {
            this.score = 0;
        }
    }

    /**
     * Get player's physical shape in client side
     *
     * @return value that indicate client physical view
     */
    public int getType() {
        return imgType;
    }

    /**
     * Generate and get defeat message to inform players
     *
     * @return Details about defeated player
     */
    public DefeatMsg getDefeatMsg() {
        return new DefeatMsg(this.userID, this.body.getPosition().x, this.body.getPosition().y,
                this.body.getAngle(), this.score, this.imgType);
    }

}