package team108.Strategies;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ____RobotStrategyTemplate__ extends Strategy {

	public ____RobotStrategyTemplate__(RobotController in) { super(in); }

	public void run() {
		Direction dir;
		MapLocation myLoc;
		try {
			switch (rc.getType()) {
			case HQ:
				// Find Spawn direction
				// Check Straight
				myLoc = rc.getLocation(); 
				dir = myLoc.directionTo(rc.senseEnemyHQLocation());
				// Rotate until i hit a valid spawn point
				while ( !rc.canMove(dir) ) dir = dir.rotateLeft(); 

				while ( true ) { 
					if ( rc.isActive() ) {
						// Spawn a soldier
						if ( rc.senseRobotCount()<GameConstants.MAX_ROBOTS ) if ( rc.canMove(dir) ) rc.spawn(dir);
					} 
					rc.yield();
				}
			
			case SOLDIER:
				while ( true ) {
					if ( rc.isActive() ) {
					}
					rc.yield();
				}
				
			case NOISETOWER:
				while ( true ) {
					if ( rc.isActive() ) {
					}
					rc.yield();
				}

			case PASTR:
				while ( true ) {
					if ( rc.isActive() ) {
					}
					rc.yield();
				}

			default:
				break;
			}
		} catch (Exception e) {
			rc.breakpoint();
			e.printStackTrace();
		}
	}	
}
