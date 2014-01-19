package team108.Broadcast;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

/**
 * Basic broadcast class, uses the default broadcast methods available to the robot controller
 * @author Stephen Bush
 *
 */
public class Broadcast implements I_Broadcast {

	RobotController rc;

	public Broadcast(RobotController in) { rc = in; }
	public void broadcast(int index, int value) throws GameActionException { rc.broadcast(index, value); }
	public int readBroadcast(int index) throws GameActionException { return rc.readBroadcast(index); }

}
