package team108.Strategies;

import java.util.LinkedList;

import team108.Graph.MapRender;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class SwarmAndAttackStrategy extends Strategy {

	// This channel defines the state of the process used to decide on the Squad leader
	// 0 = Need a new leader
	// 1 = Deciding on leader
	// 2 = Leader is chosen
	final int BC_Leader_Check 	= 0;
	
	// This channel holds the unique ID of the squad leader
	final int BC_Leader_ID 		= 1;
	
	// This channel holds the HP of the squad leader.  The soldier with the lowest health is leader.
	final int BC_Leader_HP 		= 2;

	final int BC_Leader_Loc 	= 4;

	
	// 
	final int BC_Target_Enemy	= 3;
	
	public SwarmAndAttackStrategy(RobotController in) { super(in); }

	public void run() {
		Direction dir;
		MapLocation myLoc, center = new MapLocation(rc.getMapWidth()/2,rc.getMapHeight()/2);
		double myHealth = rc.getHealth();
		int myID;
		try {	
			myID = rc.getRobot().getID();
			switch (rc.getType()) {
			case HQ:
				// Find Spawn direction
				// Check Straight
				myLoc = rc.getLocation(); 
				dir = myLoc.directionTo(rc.senseEnemyHQLocation());
				
				// Spawn the first bot
				rc.spawn(dir);
				
				// Construct a map render
				MapRender mren = new MapRender(rc);
				mren.init();
				
				LinkedList<PointPair> pps = getCornerPointPairsForMapRender(mren);

				while ( true ) {
					// Check the leader-selection status;
					if ( rc.readBroadcast(BC_Leader_Check) == 2 ) {
						// Check on the status of the leader.  
						// If the leader is alive, he will respond by setting this channel back to 2.
						// If not, next turn the process to start leader selection will begin.
						rc.broadcast(BC_Leader_Check, 0); 
					}
					else if ( rc.readBroadcast(BC_Leader_Check) == 0 ) {
						// Start the leader-selection process.
						// This turn, all the bots will put in their candidacy as leader.
						// Next turn, the vote will be locked and the best candidate will be leader.
						rc.broadcast(BC_Leader_Check, 1); 
						rc.broadcast(BC_Leader_HP, 999999999); 
					}
					else if ( rc.readBroadcast(BC_Leader_Check) == 1 ) {
						rc.broadcast(BC_Leader_Check, 2); 
					}
					
 
					if ( rc.isActive() ) {
						// Spawn a soldier
						if ( rc.senseRobotCount()<GameConstants.MAX_ROBOTS ) if ( rc.canMove(dir) ) rc.spawn(dir);
					} 
					rc.setIndicatorString(0,"Leader Status:   "+rc.readBroadcast(BC_Leader_Check));
					
					rc.yield();
				}
			
			case SOLDIER:
				int leaderCheck;
				boolean amLeader = false;
				myHealth = rc.getHealth();
				while ( true ) {
					amLeader = (rc.readBroadcast(BC_Leader_ID) == myID);
					myLoc = rc.getLocation();
					// 1.   Check the status of the leader
					leaderCheck = rc.readBroadcast(BC_Leader_Check);
					if ( leaderCheck == 1 ) {
						// Check to see if i qualify for leader
						if ( ((int)myHealth*1000) < rc.readBroadcast(BC_Leader_HP) ) {
							// If my health is less than the current leader, I do qualify.
							// Additionally, it is a strict less than; for candidates whose health ties (or are full), leader is the oldest (lowest ID).  This isnt enforced strictly, but rather implicitly as robots process in the ordr in which they spawn. 
							rc.broadcast(BC_Leader_HP, ((int)myHealth*1000));
							rc.broadcast(BC_Leader_ID, myID);
						}
					}
					// Shooting
					if ( rc.isActive() ) {
						if ( amLeader ) {
							Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
							if(enemyRobots.length>0){//if there are enemies
								Robot bestTarget = null; 
								boolean inRange = false;
								// Process the enemies
								for ( Robot n : enemyRobots ) {
									if ( rc.senseRobotInfo(n).type == RobotType.HQ ) { continue; } // Ignore HQ's
									if ( bestTarget == null ) {
										bestTarget = n;
										if ( isInShootingRange(n) ) inRange = true;
									}
									else {
										if ( rc.senseRobotInfo(bestTarget).health > rc.senseRobotInfo(n).health ) {
											if ( inRange ) {
												if ( isInShootingRange(n) ) bestTarget = n;
											}
											else {
												bestTarget = n;
												if ( isInShootingRange(n) ) inRange = true;
											}
											
										}
									}
								}
								
								// Determine what to do with this target
								
								if ( bestTarget == null ) { if ( rc.readBroadcast(BC_Target_Enemy) != 0 ) rc.broadcast(BC_Target_Enemy, 0);} // Do nothing
								else if ( inRange ) {
									// The target is in shooting range, should probably fire, also signal the squad to fire.
									rc.attackSquare(rc.senseRobotInfo(bestTarget).location);
									rc.broadcast(BC_Target_Enemy, rc.senseRobotInfo(bestTarget).robot.getID());
								}
								else {
									takeStepTowards(rc.senseRobotInfo(bestTarget).location);
									rc.broadcast(BC_Leader_Loc, locToInt(rc.getLocation()));

								}
								
								
							}
						}
						else { // I am not leader, so wait for the leaders orders 
							int myTarget = rc.readBroadcast(BC_Target_Enemy);
							if ( myTarget == 0 ) { } // Do nothing this turn, wait for orders
							else { // There is a designated target
								Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
								if(enemyRobots.length>0){//if there are enemies
									Robot bestTarget = null;
									for ( Robot n : enemyRobots ) {
										if ( n.getID() == myTarget ) {
											bestTarget = n;
										}
									}
									if ( bestTarget == null ) { } // I dont detect the target, so do nothing
									else if ( isInShootingRange(bestTarget) ) {
										// Shoot it
										rc.attackSquare(rc.senseRobotInfo(bestTarget).location);
									}
									else { 
										// Move towards it
										takeStepTowards(rc.senseRobotInfo(bestTarget).location); 
									} 
								}
								else { } // Do nothing, the target is not in range

							}
						}
					}
					
					// Moving
					if ( rc.isActive() ) {
						if ( amLeader ) {
							// If there is a distress call, prioritize that.
							
							// If not, if there are any enemy Pastr's, prioritize those.
							MapLocation[] enemyPastrs = rc.sensePastrLocations(rc.getTeam().opponent());
							if ( enemyPastrs.length > 0 ) {
								MapLocation pastrTarget = findClosestTo(myLoc,enemyPastrs);
								takeStepTowards(pastrTarget);
							}
							
							// If not, take defensive refuge in the center. 
							if ( !myLoc.equals(center) ) if ( rc.isActive() ) takeStepTowards(center);
							
							// In any case, broadcast my position. 
							rc.broadcast(BC_Leader_Loc, locToInt(rc.getLocation()));
						}
						else {
							// Move towards the leader
							MapLocation leaderPos = intToLoc(rc.readBroadcast(BC_Leader_Loc));
							if ( myLoc.equals(leaderPos) ) { } // Do nothing
							else { takeStepTowards(leaderPos); }
						}
					}
					
					// Check health status
					if ( rc.getHealth() < myHealth ) {
						myHealth = rc.getHealth();
						if ( amLeader ) {
							rc.broadcast(BC_Leader_HP, ((int)myHealth*1000));
						}
						else {
							// See if i qualify for leader
							if ( rc.readBroadcast(BC_Leader_HP) > ((int)1000*myHealth) ) {
								// Take leadership next turn
								rc.broadcast(BC_Leader_ID, myID);
								rc.broadcast(BC_Leader_HP, ((int)myHealth*1000));
							}
						}
					}
					else {
						myHealth = rc.getHealth();
					}

					
					if ( amLeader ) {
						//System.out.println("I am Leader!");
						rc.setIndicatorString(0,"I am Leader!");
						rc.broadcast(BC_Leader_Check, 2);
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
	
	private LinkedList<PointPair> getCornerPointPairsForMapRender(MapRender mren) {

		LinkedList<PointPair> ppL = new LinkedList<PointPair>();
		MapLocation start,finish;
		int fin,height = rc.getMapHeight(),width = rc.getMapWidth();
		
		if ( mren.terrainMatrix[0][0] == 99 ) { } // Skip
		else {
			start = new MapLocation(1,1);

			// Check Corner 0,0 Right
			fin = 0;
			while ( fin < width-1 && mren.terrainMatrix[fin+1][0] != 99 ) { fin++; }
			finish = new MapLocation(fin-1,1);
			ppL.add(new PointPair(start,finish));

			// Check Corner 0,0 Down
			fin = 0;
			while ( fin < height-1 && mren.terrainMatrix[0][fin+1] != 99 ) { fin++; }
			finish = new MapLocation(1,fin-1);
			ppL.add(new PointPair(start,finish));
		}

		
		if ( mren.terrainMatrix[0][height-1] == 99 ) { } // Skip
		else {
			start = new MapLocation(1,height-2);

			// Check Corner 0,H Right
			fin = 0;
			while ( fin < width-1 && mren.terrainMatrix[fin+1][height-1] != 99 ) { fin++; }
			finish = new MapLocation(fin-1,height-2);
			ppL.add(new PointPair(start,finish));

			// Check Corner 0,H Up
			fin = height-1;
			while ( fin > 0 && mren.terrainMatrix[0][fin-1] != 99 ) { fin--; }
			finish = new MapLocation(1,fin+1);
			ppL.add(new PointPair(start,finish));
		}

		
		if ( mren.terrainMatrix[width-1][0] == 99 ) { } // Skip
		else {
			start = new MapLocation(width-2,1);

			// Check Corner W,0 Left
			fin = width-1;
			while ( fin > 0 && mren.terrainMatrix[fin-1][0] != 99 ) { fin--; }
			finish = new MapLocation(fin+1,1);
			ppL.add(new PointPair(start,finish));

			// Check Corner W,0 Down
			fin = 0;
			while ( fin < height-1 && mren.terrainMatrix[width-1][fin+1] != 99 ) { fin++; }
			finish = new MapLocation(width-2,fin-1);
			ppL.add(new PointPair(start,finish));
		}

		
		if ( mren.terrainMatrix[width-1][height-1] == 99 ) { } // Skip
		else {
			start = new MapLocation(width-2,height-2);

			// Check Corner W,H Left
			fin = width-1;
			while ( fin > 0 && mren.terrainMatrix[fin-1][height-1] != 99 ) { fin--; }
			finish = new MapLocation(fin+1,height-2);
			ppL.add(new PointPair(start,finish));

			// Check Corner W,H Up
			fin = height-1;
			while ( fin > 0 && mren.terrainMatrix[width-1][fin-1] != 99 ) { fin--; }
			finish = new MapLocation(width-2,fin+1);
			ppL.add(new PointPair(start,finish));
		}
		
		// Sort the pairs by length
		LinkedList<PointPair> sortedList = new LinkedList<PointPair>();
		while ( !ppL.isEmpty() ) {
			int sLen = 0;
			PointPair tempPair = null;
			for ( PointPair n : ppL ) {
				if ( n.p1.distanceSquaredTo(n.p2) > sLen ) {
					sLen = n.p1.distanceSquaredTo(n.p2);
					tempPair = n;
				}
			}
			ppL.remove(tempPair);
			// Put the Pastr at the endpoint closest to the HQ
			if ( tempPair.p2.distanceSquaredTo(rc.senseHQLocation()) < tempPair.p1.distanceSquaredTo(rc.senseHQLocation())) { tempPair = new PointPair(tempPair.p2,tempPair.p1); }
			if ( sortedList.contains(tempPair) ) { } // Skip it, a copy of the pair is already in the list.
			else { sortedList.add(tempPair); }
		}

		return sortedList;
	}

	private boolean isInShootingRange(Robot r) throws GameActionException {
		if(rc.senseRobotInfo(r).location.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared) return true;
		return false;
	}
	
	public class PointPair {
		
		MapLocation p1;
		MapLocation p2;
		
		public PointPair(MapLocation s, MapLocation f) {
			p1 = s;
			p2 = f;
		}
		
		@Override
		public boolean equals(Object e) {
			if ( e == null ) return false;
			else if ( !(e instanceof PointPair) ) return false;
			else {
				PointPair p = (PointPair)e;
				return (p1.equals(p.p1) && p2.equals(p.p2));
			}
		}
	}
}
