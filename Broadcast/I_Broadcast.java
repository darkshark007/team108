package team108.Broadcast;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public interface I_Broadcast {
	
	public void broadcast(int index, int value) throws GameActionException;
	
	public int readBroadcast(int index) throws GameActionException;

}
