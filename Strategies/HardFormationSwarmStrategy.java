package team108.Strategies;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class HardFormationSwarmStrategy extends Strategy {

	final static int BC_SWARM_COUNT			= 0;
	final static int BC_SWARM_ID			= 1;
	final static int BC_SWARM_DIR			= 3;
	final static int BC_SWARM_LEAD_LOC		= 2;
	
	int mySwarmID;
	
	
	public HardFormationSwarmStrategy(RobotController in) { super(in); }

	public void run() {
		Direction dir = null;
		MapLocation myLoc = null;
		try {
			switch (rc.getType()) {
			case HQ:
				// Find Spawn direction
				// Check Straight
				myLoc = rc.getLocation(); 
				dir = myLoc.directionTo(rc.senseEnemyHQLocation());
				while ( !rc.canMove(dir) ) dir = dir.rotateLeft(); 
				rc.broadcast(BC_SWARM_ID, 1);
				rc.broadcast(BC_SWARM_DIR, 1);
					
				while ( true ) { 
					if ( rc.isActive() ) {
						// Spawn a soldier
						if ( rc.senseRobotCount()<GameConstants.MAX_ROBOTS ) if ( rc.canMove(dir) ) rc.spawn(dir);
					} 
					rc.yield();
				}
			
			case SOLDIER:
				mySwarmID = rc.readBroadcast(BC_SWARM_ID);
				MapLocation myTarget = new MapLocation(30,30);
				rc.broadcast(BC_SWARM_ID, mySwarmID+1);
				while ( true ) {
					if ( rc.isActive() ) {
						if ( mySwarmID == 1) {
							if ( Clock.getRoundNum() == 300 ) myTarget = new MapLocation(25,25);
							if ( Clock.getRoundNum() == 400 ) myTarget = new MapLocation(40,20);
							if ( !rc.getLocation().equals(myTarget) ) dir = takeStepTowards(myTarget);
							if ( dir != null ) rc.broadcast(BC_SWARM_DIR, directionToInt(dir)); 
							rc.broadcast(BC_SWARM_LEAD_LOC, locToInt(rc.getLocation()));
						}
						else {
							myTarget = getFormationLocation();
							if ( !rc.getLocation().equals(myTarget) ) takeStepTowards(myTarget);
						}
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
	
	
	private int directionToInt(Direction dir) {
		switch (dir) {
		case NORTH:			return 1;
		case NORTH_EAST:	return 2;
		case EAST:			return 3;
		case SOUTH_EAST:	return 4;
		case SOUTH:			return 5; 
		case SOUTH_WEST:	return 6;
		case WEST:			return 7;
		case NORTH_WEST:	return 8;
		default:			return 0;
		}
	}

	public MapLocation getFormationLocation() throws GameActionException {
		MapLocation leadLoc = intToLoc(rc.readBroadcast(BC_SWARM_LEAD_LOC));
		switch(rc.readBroadcast(BC_SWARM_DIR)) {
		case 1:
			switch(mySwarmID) {
			case 2:   return new MapLocation(leadLoc.x-1,leadLoc.y);
			case 3:   return new MapLocation(leadLoc.x,leadLoc.y+1);
			case 4:   return new MapLocation(leadLoc.x+1,leadLoc.y);
			case 5:   return new MapLocation(leadLoc.x+1,leadLoc.y+1);
			case 6:   return new MapLocation(leadLoc.x+1,leadLoc.y+2);
			case 7:   return new MapLocation(leadLoc.x,leadLoc.y+2);
			case 8:   return new MapLocation(leadLoc.x-1,leadLoc.y+1);
			default:  return new MapLocation(leadLoc.x-1,leadLoc.y+2);
			}

		case 2:
			switch(mySwarmID) {
			case 2:   return new MapLocation(leadLoc.x-1,leadLoc.y-1);
			case 3:   return new MapLocation(leadLoc.x-1,leadLoc.y);
			case 4:   return new MapLocation(leadLoc.x,leadLoc.y+1);
			case 5:   return new MapLocation(leadLoc.x+1,leadLoc.y+1);
			case 6:   return new MapLocation(leadLoc.x+1,leadLoc.y+2);
			case 7:   return new MapLocation(leadLoc.x,leadLoc.y+2);
			case 8:   return new MapLocation(leadLoc.x-1,leadLoc.y+1);
			default:  return new MapLocation(leadLoc.x-2,leadLoc.y);
			}

		case 3:
			switch(mySwarmID) {
			case 2:   return new MapLocation(leadLoc.x,leadLoc.y-1);
			case 3:   return new MapLocation(leadLoc.x-1,leadLoc.y);
			case 4:   return new MapLocation(leadLoc.x,leadLoc.y+1);
			case 5:   return new MapLocation(leadLoc.x-1,leadLoc.y+1);
			case 6:   return new MapLocation(leadLoc.x-2,leadLoc.y+1);
			case 7:   return new MapLocation(leadLoc.x-2,leadLoc.y);
			case 8:   return new MapLocation(leadLoc.x-1,leadLoc.y-1);
			default:  return new MapLocation(leadLoc.x-2,leadLoc.y-1);
			}

			
		case 4:
			switch(mySwarmID) {
			case 2:   return new MapLocation(leadLoc.x+1,leadLoc.y-1);
			case 3:   return new MapLocation(leadLoc.x,leadLoc.y-1);
			case 4:   return new MapLocation(leadLoc.x-1,leadLoc.y);
			case 5:   return new MapLocation(leadLoc.x-1,leadLoc.y+1);
			case 6:   return new MapLocation(leadLoc.x-2,leadLoc.y+1);
			case 7:   return new MapLocation(leadLoc.x-2,leadLoc.y);
			case 8:   return new MapLocation(leadLoc.x-1,leadLoc.y-1);
			default:  return new MapLocation(leadLoc.x,leadLoc.y-2);
			}

		case 5:
			switch(mySwarmID) {
			case 2:   return new MapLocation(leadLoc.x+1,leadLoc.y);
			case 3:   return new MapLocation(leadLoc.x,leadLoc.y-1);
			case 4:   return new MapLocation(leadLoc.x-1,leadLoc.y);
			case 5:   return new MapLocation(leadLoc.x-1,leadLoc.y-1);
			case 6:   return new MapLocation(leadLoc.x-1,leadLoc.y-2);
			case 7:   return new MapLocation(leadLoc.x,leadLoc.y-2);
			case 8:   return new MapLocation(leadLoc.x+1,leadLoc.y-1);
			default:  return new MapLocation(leadLoc.x+1,leadLoc.y-2);
			}

			
		case 6:
			switch(mySwarmID) {
			case 2:   return new MapLocation(leadLoc.x+1,leadLoc.y+1);
			case 3:   return new MapLocation(leadLoc.x+1,leadLoc.y);
			case 4:   return new MapLocation(leadLoc.x,leadLoc.y-1);
			case 5:   return new MapLocation(leadLoc.x-1,leadLoc.y-1);
			case 6:   return new MapLocation(leadLoc.x-1,leadLoc.y-2);
			case 7:   return new MapLocation(leadLoc.x,leadLoc.y-2);
			case 8:   return new MapLocation(leadLoc.x+1,leadLoc.y-1);
			default:  return new MapLocation(leadLoc.x+2,leadLoc.y);
			}

		case 7:
			switch(mySwarmID) {
			case 2:   return new MapLocation(leadLoc.x,leadLoc.y+1);
			case 3:   return new MapLocation(leadLoc.x+1,leadLoc.y);
			case 4:   return new MapLocation(leadLoc.x,leadLoc.y-1);
			case 5:   return new MapLocation(leadLoc.x+1,leadLoc.y-1);
			case 6:   return new MapLocation(leadLoc.x+2,leadLoc.y-1);
			case 7:   return new MapLocation(leadLoc.x+2,leadLoc.y);
			case 8:   return new MapLocation(leadLoc.x+1,leadLoc.y+1);
			default:  return new MapLocation(leadLoc.x+2,leadLoc.y+1);
			}

		case 8:
			switch(mySwarmID) {
			case 2:   return new MapLocation(leadLoc.x-1,leadLoc.y+1);
			case 3:   return new MapLocation(leadLoc.x,leadLoc.y+1);
			case 4:   return new MapLocation(leadLoc.x+1,leadLoc.y);
			case 5:   return new MapLocation(leadLoc.x+1,leadLoc.y-1);
			case 6:   return new MapLocation(leadLoc.x+2,leadLoc.y-1);
			case 7:   return new MapLocation(leadLoc.x+2,leadLoc.y);
			case 8:   return new MapLocation(leadLoc.x+1,leadLoc.y+1);
			default:  return new MapLocation(leadLoc.x,leadLoc.y+2);
			}
		}
		return null;
	}
}
