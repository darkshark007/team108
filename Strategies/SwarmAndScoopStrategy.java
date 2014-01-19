package team108.Strategies;

import java.util.LinkedList;

import team108.Graph.MapRender;
import team108.Path.*;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class SwarmAndScoopStrategy extends Strategy {

	
	// This channel defines the state of the process used to decide on the Squad leader
	// 0 = Need a new leader
	// 1 = Deciding on leader
	// 2 = Leader is chosen
	final int BC_Leader_Check 		= 0;
	
	// This channel holds the unique ID of the squad leader
	final int BC_Leader_ID 			= 1;
	
	// This channel holds the HP of the squad leader.  The soldier with the lowest health is leader.
	final int BC_Leader_HP 			= 2;

	final int BC_Leader_Loc 		= 3;

	final int BC_Leader_Dest_Loc		= 4;
	final int BC_Leader_Dest_Loc_Flag	= 41;

	
	// 
	final int BC_Target_Enemy		= 5;

	final int BC_ORDERS_ID			= 6;
	final int BC_ORDERS_PATH_CHAN	= 9;
	
	final int BC_Pastr_Convert_Flag		= 10;
	final int BC_Pastr_Convert_Loc		= 11;
	
	final int BC_RALLY					= 12;

	
	int myOrders;
	MapLocation myLoc;
	MapLocation center = null;
	int myID;
	int myPathChannel;
	int myPathPosition;
	
	public SwarmAndScoopStrategy(RobotController in) { super(in); }

	public void run() {
		Direction dir;
		try {	
			myID = rc.getRobot().getID();
			switch (rc.getType()) {
			case HQ:
				int pathChan = 300;
				boolean nextRobotIsAPastrScoop = false;
				LinkedList<Integer> PastrScoopQueue = new LinkedList<Integer>();
				LinkedList<MapLocation> pastrsBuilt = new LinkedList<MapLocation>();

				rc.broadcast(BC_ORDERS_PATH_CHAN, 200); // Soldiers will use channel 200, always
				PointPair tempPair = null;
				// Find Spawn direction
				// Check Straight
				myLoc = rc.getLocation(); 
				dir = myLoc.directionTo(rc.senseEnemyHQLocation());
				
				// Spawn the first bot
				rc.broadcast(BC_ORDERS_ID, 1); // Spawn Soldiers
				rc.spawn(dir);
				
				// Construct a map render
				DirectWithOptimizedBuggingPathGenerator gr = new DirectWithOptimizedBuggingPathGenerator(rc);
				
				// Adjust the rally point
				
				center = rc.senseEnemyHQLocation();
				/* Old rally Point, changing it up slightly before the sprint tournament to be more agressive
				center = new MapLocation(rc.getMapWidth()/2,rc.getMapHeight()/2); // Rally Point
				if ( gr.getMapRender().terrainMatrix[center.x][center.y] == 99 ) {
					// Get a new rally Point
					// Try
					center = new MapLocation((int)(rc.getMapWidth()*(3.0/4.0)),rc.getMapHeight()/4); // Rally Point
					System.out.println("Rally Change #1"+center.toString());
					if ( gr.getMapRender().terrainMatrix[center.x][center.y] == 99 ) {
						center = new MapLocation(rc.getMapWidth()/4,(int)(rc.getMapHeight()*(3.0/4.0))); // Rally Point	
						System.out.println("Rally Change #2"+center.toString());
					}
				}
				/* */
				rc.broadcast(BC_RALLY,locToInt(center));

								
				LinkedList<PointPair> pps = null;

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
						if ( pps == null ) {
						 	pps = getCornerPointPairsFromMapRender(gr.getMapRender());
							if ( pps.size() == 0 ) {
								// Alternative point-pair strategy
								pps = getAbstractPointPairsFromMapRender(gr.getMapRender());
							}
						}
					}
					
					// Check the PastrConversion flag
					if ( rc.readBroadcast(BC_Pastr_Convert_Flag) == 1) {
						// One of the bots is ready to convert into a pastr, so let's set him up with a location and a path to get there.
						if ( !pps.isEmpty() ) {
							tempPair = pps.removeFirst();
							while ( pastrsBuilt.contains(tempPair.p1) ) {
								// This pastr is already built, no need to build another.
								// But we will need to assign a new scoop to this pair.
								rc.broadcast(pathChan+1, locToInt(tempPair.p1));
								rc.broadcast(pathChan+2, locToInt(tempPair.p2));
								PastrScoopQueue.add(new Integer(pathChan));
								pathChan += 100;															
								
								// Will also need to get a new pair.
								if ( !pps.isEmpty() ) tempPair = pps.removeFirst();
								else {
									tempPair = null;
									rc.broadcast(BC_Pastr_Convert_Flag, 3); // Indicate to the convertee that there are no valid Pastrs remaining
								}
							}
							if ( tempPair != null ) {
								pastrsBuilt.add(tempPair.p1);
								nextRobotIsAPastrScoop = true;
								
								// Flag that the request to convert is permissible
								rc.broadcast(BC_Pastr_Convert_Flag, 2);
								
								// First give out orders concerning the scoop targets
								rc.broadcast(pathChan+1, locToInt(tempPair.p1));
								rc.broadcast(pathChan+2, locToInt(tempPair.p2));
								
								// Send them the next path channel
								rc.broadcast(BC_ORDERS_PATH_CHAN, pathChan);
								// Calculate a path for them to follow
								postPathToPathChannel(gr.getPath(intToLoc(rc.readBroadcast(BC_Pastr_Convert_Loc)),tempPair.p1),pathChan);
								// Increment the channel
								pathChan += 100;


								// Reserve a channel for the scooper
								rc.broadcast(pathChan+1, locToInt(tempPair.p1));
								rc.broadcast(pathChan+2, locToInt(tempPair.p2));
								PastrScoopQueue.add(new Integer(pathChan));
								pathChan += 100;															
							}
						}
						else rc.broadcast(BC_Pastr_Convert_Flag, 3); // Indicate to the convertee that there are no valid Pastrs remaining
					}
					
					// Spawn a soldier 
					if ( rc.isActive() ) {
						if ( rc.senseRobotCount()<GameConstants.MAX_ROBOTS ) if ( rc.canMove(dir) ) {
							if ( debugLevel >= 3 ) System.out.println(">>> RUA: "+rc.roundsUntilActive());
							rc.spawn(dir);
							if ( debugLevel >= 3 ) System.out.println(">>> RUA: "+rc.roundsUntilActive());
							if ( ( Clock.getRoundNum() >= 200 && pastrsBuilt.size() == 0 ) || ( Clock.getRoundNum() >= 300 && pastrsBuilt.size() <= 1 ) || ( Clock.getRoundNum() >= 400 && pastrsBuilt.size() <= 2 ) ) {
								// Manually spawn a Pastr, to ensure that we're gathering Milk and dont lose to tiebreakers
								if ( !pps.isEmpty() ) {
									tempPair = pps.removeFirst();
									while ( pastrsBuilt.contains(tempPair.p1) ) {
										// This pastr is already built, no need to build another.
										// But we will need to assign a new scoop to this pair.
										rc.broadcast(pathChan+1, locToInt(tempPair.p1));
										rc.broadcast(pathChan+2, locToInt(tempPair.p2));
										PastrScoopQueue.add(new Integer(pathChan));
										pathChan += 100;															
										
										// Will also need to get a new pair.
										if ( !pps.isEmpty() ) tempPair = pps.removeFirst();
										else {
											tempPair = null;
											rc.broadcast(BC_Pastr_Convert_Flag, 3); // Indicate to the convertee that there are no valid Pastrs remaining
										}
									}
									if ( tempPair != null ) {
										pastrsBuilt.add(tempPair.p1);
										nextRobotIsAPastrScoop = true;
										
										// Flag that the request to convert is permissible
										rc.broadcast(BC_ORDERS_ID, 2);
										
										// First give out orders concerning the scoop targets
										rc.broadcast(pathChan+1, locToInt(tempPair.p1));
										rc.broadcast(pathChan+2, locToInt(tempPair.p2));
										
										// Send them the next path channel
										rc.broadcast(BC_ORDERS_PATH_CHAN, pathChan);
										// Calculate a path for them to follow
										if ( debugLevel >= 1 ) System.out.println(">>> Creating Path [0] From  "+myLoc.add(dir).toString()+" -> "+tempPair.p1.toString()+"  For "+pathChan);
										postPathToPathChannel(gr.getPath(myLoc.add(dir),tempPair.p1),pathChan);
										// Increment the channel
										pathChan += 100;


										// Reserve a channel for the scooper
										rc.broadcast(pathChan+1, locToInt(tempPair.p1));
										rc.broadcast(pathChan+2, locToInt(tempPair.p2));
										PastrScoopQueue.add(new Integer(pathChan));
										pathChan += 100;															
									}
								}
								else rc.broadcast(BC_Pastr_Convert_Flag, 3); // Indicate to the convertee that there are no valid Pastrs remaining
							}
							else if ( nextRobotIsAPastrScoop ) {
								int pastrScoopChan = PastrScoopQueue.removeFirst().intValue();
								rc.broadcast(BC_ORDERS_PATH_CHAN, pastrScoopChan);
								if ( PastrScoopQueue.isEmpty() ) nextRobotIsAPastrScoop = false;
								rc.broadcast(BC_ORDERS_ID, 3);
								MapLocation tempLoc;
								if ( rc.readBroadcast(pastrScoopChan+2) == 0 ) tempLoc = tempPair.p2.add(tempPair.p2.directionTo(tempPair.p1));  
								else tempLoc = intToLoc(rc.readBroadcast(pastrScoopChan+2)).add(intToLoc(rc.readBroadcast(pastrScoopChan+2)).directionTo((intToLoc(rc.readBroadcast(pastrScoopChan+1)))));
								if ( debugLevel >= 1 ) System.out.println(">>> Creating Path [0] From  "+myLoc.add(dir).toString()+" -> "+tempLoc.toString()+"  For "+pathChan);
								postPathToPathChannel(gr.getPath(myLoc.add(dir),tempLoc),pastrScoopChan);
							}
							else { 
								rc.broadcast(BC_ORDERS_PATH_CHAN, pathChan);
								rc.broadcast(BC_ORDERS_ID, 1);
								postPathToPathChannel(gr.getPath(myLoc.add(dir), intToLoc(rc.readBroadcast(BC_Leader_Loc))),pathChan);
								pathChan += 100;
							}
						}
					} 
					
					
					// Attack!!  (If enemies nearby)
					Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,15,rc.getTeam().opponent());
					if ( enemyRobots.length > 0 ) {
						rc.attackSquare(rc.senseRobotInfo(enemyRobots[0]).location);
					}
					
					
					// Check leader path status
					if ( rc.readBroadcast(BC_Leader_Dest_Loc_Flag) == 1 ) {
						if ( debugLevel >= 1 ) System.out.println(">>> Creating Path [0] From  "+intToLoc(rc.readBroadcast(BC_Leader_Loc)).toString()+" -> "+intToLoc(rc.readBroadcast(BC_Leader_Dest_Loc)).toString()+"  For "+pathChan);
						postPathToPathChannel(gr.getPath(intToLoc(rc.readBroadcast(BC_Leader_Loc)), intToLoc(rc.readBroadcast(BC_Leader_Dest_Loc))),200);
						rc.broadcast(BC_Leader_Dest_Loc_Flag, 0);
					}
					
					
					// Check Defender path status
					boolean continueFlag = false;
					for ( int i = 200; i < pathChan; i += 100 ) {
						if ( rc.readBroadcast(i) == 2 ) {
							// A defender has requested a path recalculation
							continueFlag = true;
							postPathToPathChannel(gr.getPath(intToLoc(rc.readBroadcast(i+1)), intToLoc(rc.readBroadcast(BC_Leader_Loc))), i);
							break;
						}
					}
					
					// Post leader select status
					if ( debugLevel >= 1 ) rc.setIndicatorString(0,"Leader Status:   "+rc.readBroadcast(BC_Leader_Check));
					
					if ( continueFlag ) continue;					
					rc.yield();
				}
			
			case SOLDIER:
				runSoldier();
				
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
	
	private void runSoldier() throws GameActionException {
		myOrders = rc.readBroadcast(BC_ORDERS_ID);
		myPathChannel = rc.readBroadcast(BC_ORDERS_PATH_CHAN);
		myPathPosition = 3;
		while ( myOrders == 0 ) {
			rc.yield();
			myOrders = rc.readBroadcast(BC_ORDERS_ID);
		}
		if ( debugLevel >= 1 ) System.out.println(">> My Orders: "+myOrders+"\t\tMyPathChannel:  "+myPathChannel);		
		
		switch (myOrders) {
		case 1: // Soldier_Defender
			runSoldier_Defender();
			break;

		case 2: // Pastr
			runSoldier_Pastr();
			break;

		case 3: // Pastr Scooper
			runSoldier_Scoop();
			break;
		}
	}

	private void runSoldier_Scoop() throws GameActionException {

		MapLocation pastrTarget = intToLoc(rc.readBroadcast(myPathChannel+1));
		MapLocation scoopTarget = intToLoc(rc.readBroadcast(myPathChannel+2));

		int scoopState = 0;
		boolean startedLoop = false;
		pastrTarget = pastrTarget.add(pastrTarget.directionTo(scoopTarget));
		scoopTarget = scoopTarget.add(scoopTarget.directionTo(pastrTarget));
		MapLocation myTarget = scoopTarget;
		if ( debugLevel >= 1 ) rc.setIndicatorString(1, "My Orders:  "+myOrders+"     MyPathChan: "+myPathChannel+"     PastR: "+pastrTarget.toString()+"     Scoop: "+scoopTarget.toString());
		while ( true ) {
			myLoc = rc.getLocation();
			if ( rc.isActive() ) {
				if ( startedLoop == false ) {
					if ( myLoc.equals(scoopTarget) ) {
						startedLoop = true;
						continue;
					}
					else {
						if ( rc.readBroadcast(myPathChannel) != 1 ) takeStepTowards(scoopTarget);
						else {
							myTarget = intToLoc(rc.readBroadcast(myPathChannel+myPathPosition)); 
							if ( myLoc.equals(myTarget) ) {
								myPathPosition++;
								if ( rc.readBroadcast(myPathChannel+myPathPosition) == -1 ) myTarget = scoopTarget;
								else myTarget = intToLoc(rc.readBroadcast(myPathChannel+myPathPosition));
							}
							takeStepTowards(myTarget);
						}
					}

					
				}
				else {
					if ( myLoc.equals(myTarget) ) {
						scoopState = (scoopState + 1) % 4;
						switch (scoopState) {
						case 0: 
							myTarget = scoopTarget;
							break;
							
						case 1:
							myTarget = pastrTarget;
							break;
							
						case 2:
							// There are two potential targets, but one of them will be invalid.
							Direction d1 = scoopTarget.directionTo(pastrTarget).rotateRight().rotateRight(); 
							MapLocation t1 = pastrTarget.add(d1).add(d1);

							Direction d2 = scoopTarget.directionTo(pastrTarget).rotateLeft().rotateLeft(); 
							MapLocation t2 = pastrTarget.add(d2).add(d2);
							
							System.out.println(">>> Determining patrol point #2, Sensing Locations "+t1.toString()+" and "+t2.toString());
							if ( !locIsOnMap(t1) || rc.senseTerrainTile(t1).equals(TerrainTile.VOID)) myTarget = t2.add(d2);
							else myTarget = t1.add(d1);
							break;
							
						case 3:
							// Either the X's or the Y's will be the same for the two target locations, figure out which and then extrapolate.
							if ( pastrTarget.x == scoopTarget.x ) {
								myTarget = new MapLocation(myTarget.x,scoopTarget.y);
							}
							else {
								myTarget = new MapLocation(scoopTarget.x,myTarget.y);										
							}
							break;
						}

					}
					takeStepTowards(myTarget);
				}
			}
			rc.yield();
		}
	}

	private void runSoldier_Pastr() throws GameActionException {
		MapLocation pastrTarget = intToLoc(rc.readBroadcast(myPathChannel+1));
		MapLocation scoopTarget = intToLoc(rc.readBroadcast(myPathChannel+2));
		if ( debugLevel >= 1 ) rc.setIndicatorString(1, "My Orders:  "+myOrders+"     MyPathChan: "+myPathChannel+"     PastR: "+pastrTarget.toString()+"     Scoop: "+scoopTarget.toString());
		while ( true ) {
			myLoc = rc.getLocation();
			if ( rc.isActive() ) {
				if ( myLoc.equals(pastrTarget) ) rc.construct(RobotType.PASTR);
				else {
					if ( rc.readBroadcast(myPathChannel) != 1 ) takeStepTowards(pastrTarget);
					else {
						MapLocation myTarget = intToLoc(rc.readBroadcast(myPathChannel+myPathPosition)); 
						if ( myLoc.equals(myTarget) ) {
							myPathPosition++;
							if ( rc.readBroadcast(myPathChannel+myPathPosition) == -1 ) myTarget = pastrTarget;
							else myTarget = intToLoc(rc.readBroadcast(myPathChannel+myPathPosition));
							
						}
						takeStepTowards(myTarget);
					}
				}
			}
			rc.yield();
		}
	}

	private void runSoldier_Defender() throws GameActionException {
		int leaderCheck;
		double myHealth;
		boolean amLeader = false;
		myHealth = rc.getHealth();
		if ( debugLevel >= 1 ) rc.setIndicatorString(1, "My Orders:  "+myOrders+"     MyPathChan: "+myPathChannel);

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
					
					// If i am too far from the leader, fire at the nearest target
					if ( myLoc.distanceSquaredTo(intToLoc(rc.readBroadcast(BC_Leader_Loc))) >= 0.0 ) {
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
							
							if ( inRange ) {
								// The target is in shooting range, should probably fire, also signal the squad to fire.
								rc.attackSquare(rc.senseRobotInfo(bestTarget).location);
							}
						}
					}
					else {
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
			}
			
			// Moving
			if ( rc.isActive() ) {
				if ( amLeader ) {
					// If there is a distress call, prioritize that.
					
					// If not, if there are any enemy Pastr's, prioritize those.
					MapLocation[] enemyPastrs = rc.sensePastrLocations(rc.getTeam().opponent());
					if ( enemyPastrs.length > 0 ) {
						MapLocation pastrTarget = findClosestTo(myLoc,enemyPastrs);
						//takeStepTowards(pastrTarget);
						followLeaderPathTo(pastrTarget);
					}
					
					// If not, take defensive refuge in the center.
					if ( center == null ) {
						while ( rc.readBroadcast(BC_RALLY) == 0 ) {
							rc.yield();
						}
						center = intToLoc(rc.readBroadcast(BC_RALLY));
					}
					if ( !myLoc.equals(center) ) if ( rc.isActive() ) followLeaderPathTo(center); // takeStepTowards(center);
					
					// In any case, broadcast my position. 
					rc.broadcast(BC_Leader_Loc, locToInt(rc.getLocation()));
				}
				else {
					// Move towards the leader
					MapLocation leaderPos = intToLoc(rc.readBroadcast(BC_Leader_Loc));
					if ( myLoc.equals(leaderPos) ) { } // Do nothing
					else { followDefenderPathToLeader(); }
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
						myPathPosition = 3;
					}
				}
				
				// See if i am low enough to convert to a pastr target
				if ( myHealth < 40.0 ) {
					if ( rc.readBroadcast(BC_Pastr_Convert_Flag) != 3 ) {
						if ( debugLevel >= 3 ) System.out.println("CONVERSION!!!!!!!");
						rc.broadcast(BC_Pastr_Convert_Flag, 1);
						rc.broadcast(BC_Leader_Check, 0); // A new leader may need to be selected
						rc.broadcast(BC_Pastr_Convert_Loc, locToInt(rc.getLocation()));
						
						// Wait for response
						if ( debugLevel >= 3 ) System.out.println("CFlag = "+rc.readBroadcast(BC_Pastr_Convert_Flag));
						while ( rc.readBroadcast(BC_Pastr_Convert_Flag) == 0 ) {
							if ( debugLevel >= 3 ) System.out.println("Waiting... CFlag = "+rc.readBroadcast(BC_Pastr_Convert_Flag));
							rc.yield();
						}
						
						
						if ( rc.readBroadcast(BC_Pastr_Convert_Flag) == 2 ) {
							// Convert successful
							myOrders = 2;
							myPathPosition = 3;
							rc.broadcast(BC_Pastr_Convert_Flag, 0);
							myPathChannel = rc.readBroadcast(BC_ORDERS_PATH_CHAN);
							runSoldier_Pastr();
						}
					}
				}
			}
			else {
				myHealth = rc.getHealth();
			}

			
			if ( amLeader ) {
				//System.out.println("I am Leader!");
				if ( debugLevel >= 1 ) rc.setIndicatorString(0,"I am Leader!");
				rc.broadcast(BC_Leader_Check, 2);
			}
			rc.yield();
		}
	}

	private void followLeaderPathTo(MapLocation target) throws GameActionException {
		if ( debugLevel >= 2 ) System.out.println("LeaderPath -> "+target.toString());
		if ( !target.equals(intToLoc(rc.readBroadcast(BC_Leader_Dest_Loc))) ) {
			if ( debugLevel >= 2 ) System.out.println("REQ PATH CHANGE -> "+target.toString());
			// Request a path
			myPathPosition = 3;
			rc.broadcast(200, 0);
			rc.broadcast(BC_Leader_Dest_Loc_Flag, 1);
			rc.broadcast(BC_Leader_Dest_Loc, locToInt(target));			
		}
		if ( rc.readBroadcast(200) != 1 ) {
			if ( debugLevel >= 2 ) System.out.println("   LP -> BC == 1  "+target.toString());
			takeStepTowards(target);
		}
		else {
			if ( debugLevel >= 2 ) System.out.println("   LP -> BC != 1  "+target.toString());
			MapLocation myTarget = intToLoc(rc.readBroadcast(200+myPathPosition)); 
			if ( myLoc.equals(myTarget) ) {
				if ( debugLevel >= 2 ) System.out.println("   LP -> MyLoc == Targ  "+target.toString());
				myPathPosition++;
				if ( rc.readBroadcast(200+myPathPosition) == -1 ) myTarget = target;
				else myTarget = intToLoc(rc.readBroadcast(200+myPathPosition));		
			}
			else if ( rc.readBroadcast(200+myPathPosition) == -1 ) { myTarget = target; }
			if ( debugLevel >= 2 ) System.out.println("   LP -> Step Towards  "+myTarget.toString());
			takeStepTowards(myTarget);
		}

	}
	private void followDefenderPathToLeader() throws GameActionException {
		if ( debugLevel >= 2 ) System.out.println("DefenderPath -> ");
		if ( rc.readBroadcast(myPathChannel) != 1 && rc.readBroadcast(BC_Leader_Check) == 2) {
			if ( rc.getLocation().distanceSquaredTo(intToLoc(rc.readBroadcast(BC_Leader_Loc))) >= 20.0 ) {
				// I am working on my own, with no path to follow, if i am too far away i should request a path.
				if ( debugLevel >= 1 ) rc.setIndicatorString(0,"Requested Path to leader.");
				rc.broadcast(myPathChannel, 2);
				myPathPosition = 3;
				rc.broadcast(myPathChannel+1, locToInt(rc.getLocation()));
			}
			else { if ( debugLevel >= 1 ) rc.setIndicatorString(0,"MyDistanceFromLeader: "+rc.getLocation().distanceSquaredTo(intToLoc(rc.readBroadcast(BC_Leader_Loc)))); }
			takeStepTowards(intToLoc(rc.readBroadcast(BC_Leader_Loc)));
		}
		else {
			if ( debugLevel >= 1 ) rc.setIndicatorString(0,"Following the Path.");
			MapLocation myTarget = intToLoc(rc.readBroadcast(myPathChannel+myPathPosition)); 
			if ( rc.readBroadcast(myPathChannel+myPathPosition) == -1 ) { {
				if ( debugLevel >= 1 ) rc.setIndicatorString(0,"Exiting the path now.");
				myTarget = intToLoc(rc.readBroadcast(BC_Leader_Loc)); }
				rc.broadcast(myPathChannel, 0);
			}
			else if ( myLoc.equals(myTarget) ) {
				if ( debugLevel >= 1 ) rc.setIndicatorString(0,"Going to the next node in the path.");
				myPathPosition++;
				if ( rc.readBroadcast(myPathChannel+myPathPosition) == -1 ) myTarget = intToLoc(rc.readBroadcast(BC_Leader_Loc));
				else myTarget = intToLoc(rc.readBroadcast(myPathChannel+myPathPosition));				
			}
			takeStepTowards(myTarget);
		}

	}

	
	private LinkedList<PointPair> getAbstractPointPairsFromMapRender(MapRender mren) {
		LinkedList<PointPair> ppL = new LinkedList<PointPair>();
		MapLocation start,finish;
		int fin,height = rc.getMapHeight(),width = rc.getMapWidth();
		PointPair tempPP;
		int direcH,direcV;
		for ( int i = 1; i < width-1; i++) {			
			for ( int j = 1; j < height-1; j++) {
				if ( mren.terrainMatrix[i][j] != 99 ) {
					// I am not a void.  Check to see if i am a corner.

					// UL Corner
					if ( 		mren.terrainMatrix[i-1][j-1] == 99  && 
								mren.terrainMatrix[i-1][j] == 99  && 
								mren.terrainMatrix[i][j-1] == 99 ) {
						if ( debugLevel >= 3 ) System.out.println(">>> Valid UL Location Deteted: ["+i+","+j+"]");
						start = new MapLocation(i+1,j+1);
						direcH = 0;
						direcV = 0;
						while ( locIsOnMap(new MapLocation(i+direcH,j)) && mren.terrainMatrix[i+direcH][j] != 99 ) direcH++;
						finish = new MapLocation(i+(direcH-1)-1,j+1);
						tempPP = new PointPair(start,finish);
						ppL.add(tempPP);
						while ( locIsOnMap(new MapLocation(i,j+direcV)) && mren.terrainMatrix[i][j+direcV] != 99 ) direcV++;
						finish = new MapLocation(i+1,j+(direcV-1)-1);
						tempPP = new PointPair(start,finish);
						ppL.add(tempPP);						
					}
					
					// BR Corner
					if ( 		mren.terrainMatrix[i+1][j+1] == 99  && 
								mren.terrainMatrix[i+1][j] == 99  && 
								mren.terrainMatrix[i][j+1] == 99 ) {
						if ( debugLevel >= 3 ) System.out.println(">>> Valid BR Location Deteted: ["+i+","+j+"]");
						start = new MapLocation(i-1,j-1);
						direcH = 0;
						direcV = 0;
						while ( locIsOnMap(new MapLocation(i-direcH,j)) && mren.terrainMatrix[i-direcH][j] != 99 ) direcH++;
						finish = new MapLocation(i-(direcH-1)+1,j-1);
						tempPP = new PointPair(start,finish);
						ppL.add(tempPP);
						while ( locIsOnMap(new MapLocation(i,j-direcV)) && mren.terrainMatrix[i][j-direcV] != 99 ) direcV++;
						finish = new MapLocation(i-1,j-(direcV-1)+1);
						tempPP = new PointPair(start,finish);
						ppL.add(tempPP);						
					}
					
					// BL Corner
					if ( 		mren.terrainMatrix[i-1][j+1] == 99  && 
								mren.terrainMatrix[i-1][j] == 99  && 
								mren.terrainMatrix[i][j+1] == 99 ) {
						if ( debugLevel >= 3 ) System.out.println(">>> Valid BL Location Deteted: ["+i+","+j+"]");
						start = new MapLocation(i+1,j-1);
						direcH = 0;
						direcV = 0;
						while ( locIsOnMap(new MapLocation(i+direcH,j)) && mren.terrainMatrix[i+direcH][j] != 99 ) direcH++;
						finish = new MapLocation(i+(direcH-1)-1,j-1);
						tempPP = new PointPair(start,finish);
						ppL.add(tempPP);
						while ( locIsOnMap(new MapLocation(i,j-direcV)) && mren.terrainMatrix[i][j-direcV] != 99 ) direcV++;
						finish = new MapLocation(i+1,j-(direcV-1)+1);
						tempPP = new PointPair(start,finish);
						ppL.add(tempPP);						
					}

					// UR Corner
					if ( 		mren.terrainMatrix[i+1][j-1] == 99  && 
								mren.terrainMatrix[i+1][j] == 99  && 
								mren.terrainMatrix[i][j-1] == 99 ) {
						if ( debugLevel >= 3 ) System.out.println(">>> Valid UR Location Deteted: ["+i+","+j+"]");
						start = new MapLocation(i-1,j+1);
						direcH = 0;
						direcV = 0;
						while ( locIsOnMap(new MapLocation(i-direcH,j)) && mren.terrainMatrix[i-direcH][j] != 99 ) direcH++;
						finish = new MapLocation(i-(direcH-1)+1,j+1);
						tempPP = new PointPair(start,finish);
						ppL.add(tempPP);
						while ( locIsOnMap(new MapLocation(i,j+direcV)) && mren.terrainMatrix[i][j+direcV] != 99 ) direcV++;
						finish = new MapLocation(i-1,j+(direcV-1)-1);
						tempPP = new PointPair(start,finish);
						ppL.add(tempPP);						
					}
					
				}
			}
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

	
	private LinkedList<PointPair> getCornerPointPairsFromMapRender(MapRender mren) {

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
	
	private void postPathToPathChannel(Path p, int channel) throws GameActionException {
		int chan = channel+3;
		
		MapLocation next = p.nextLink();
		do {
			rc.broadcast(chan++, locToInt(next));
			next = p.nextLink();
		} while ( next != null );
		rc.broadcast(chan, -1);
		rc.broadcast(channel,1);
		if ( debugLevel >= 1 ) System.out.println(">> Path Channel "+channel+" Posted.");
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
