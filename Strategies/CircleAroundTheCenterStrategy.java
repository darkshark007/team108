package team108.Strategies;

import team108.Broadcast.*;
import team108.Path.*;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class CircleAroundTheCenterStrategy extends Strategy {
	
	I_Broadcast bc = new Broadcast(rc);
	
	final int BC_CHECK_PASTR = 0;
	

	public CircleAroundTheCenterStrategy(RobotController in) { super(in); }

	public void run() {
		Direction dir;
		try {
			switch (rc.getType()) {
			case HQ:
				// Find Spawn direction
				// Check Straight
				MapLocation myLoc = rc.getLocation(); 
				dir = myLoc.directionTo(rc.senseEnemyHQLocation());
				while ( true ) { 
					if ( rc.isActive() ) {
						// Spawn a soldier
						if ( rc.canMove(dir) ) rc.spawn(dir);
					} 
					// Null/Zero out the broadcast-counters
					bc.broadcast(BC_CHECK_PASTR, 0);
					rc.yield();
				}
			
			case SOLDIER:
				MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
				double myDistLim = 150.0;
				boolean amCircling = false;
				double myDelayInit = 0.0;
				while ( true ) {
					rc.setIndicatorString(0, "My Delay Incurred:  "+(rc.getActionDelay()-myDelayInit+1));
					myDelayInit = rc.getActionDelay();
					myLoc = rc.getLocation();
					if ( rc.isActive() ) {
						
						// Check to see if there is a central Pastr
						if ( bc.readBroadcast(BC_CHECK_PASTR) == 1 ) {
							
							while ( amCircling == false ) {
								if ( rc.isActive() ) takeStepTowards(center);
								rc.yield();
								if ( rc.getLocation().distanceSquaredTo(center) < 10 ) amCircling = true;
							}
							
							System.out.println("CIRCLING!!!\t\tD: "+myLoc.distanceSquaredTo(center));
							
							if ( myDistLim < 2 ) myDistLim = 150.0;
							
							// Circle around it

							// Get a direction perpendicular to the center
							dir = myLoc.directionTo(center).rotateRight().rotateRight();
							
							// Check to see if there is a cow build-up
							
							if ( rc.senseCowsAtLocation(myLoc.add(dir.rotateRight())) > 2500 ) {
								MapLocation target = myLoc.add(dir.rotateRight()).add(dir.rotateRight().rotateRight());
								rc.attackSquare(target);
								continue;
							}
							else if ( rc.senseCowsAtLocation(myLoc.add(dir.rotateRight().rotateRight())) > 2500 ) {
								MapLocation target = myLoc.add(dir.rotateRight().rotateRight()).add(dir.rotateRight().rotateRight());
								rc.attackSquare(target);
								continue;
							}
							else if ( rc.senseCowsAtLocation(myLoc.add(dir.rotateRight().rotateRight().rotateRight())) > 2500 ) {
								MapLocation target = myLoc.add(dir.rotateRight().rotateRight().rotateRight()).add(dir.rotateRight().rotateRight());
								rc.attackSquare(target);
								continue;
							}
							
							if ( myLoc.distanceSquaredTo(center) > myDistLim ) {
								// If we need to get closer, rotate in a little bit.
								dir = dir.rotateLeft();
							}
							else if ( myLoc.distanceSquaredTo(center) < myDistLim ) {
								// If we need to get closer, rotate in a little bit.
								dir = dir.rotateRight();
							}
							
							
							if ( rc.canMove(dir)) rc.move(dir);
							else {
								dir = dir.rotateLeft();
								if ( rc.canMove(dir)) rc.move(dir);
								else {
									dir = dir.rotateLeft();
									if ( rc.canMove(dir)) rc.move(dir);
								}
							}
							
							myDistLim = myDistLim * 0.95;
						}
						else {
							// If there is no central Pastr, set one up
							if (myLoc.equals(center)) {
								rc.construct(RobotType.PASTR);
							}
							takeStepTowards(center);	
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
						bc.broadcast(BC_CHECK_PASTR, 1);
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
