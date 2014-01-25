package team108.Strategies;

import java.util.LinkedList;
import java.util.PriorityQueue;

import team108.Graph.MapRender;
import team108.Graph.SimpleLinkedList;
import team108.Orders.*;
import team108.Path.DepthFirstPathing;
import team108.Path.ObstaclePointPathGenerator;
import team108.Path.Path;
import team108.Path.PathGenerator;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class HardFormationSwarmStrategy extends Strategy {

	final static int BC_SWARM_COUNT			= 0;
	final static int BC_SWARM_ID			= 1;
	final static int BC_SWARM_DIR			= 3;
	final static int BC_SWARM_LEAD_LOC		= 2;
	final static int BC_ORDERS_ID			= 4;
	final static int BC_ORDERS_PATH_CHAN	= 5;
	final static int BC_ATTACK_ADJ			= 6;
	final static int BC_ATTACK_FLAG			= 7;
	final static int BC_RALLY_POINT			= 8;
	final static int BC_SCATTER_FLAG		= 9;
	final static int BC_PASTR_TARGET		= 10;
	final static int BC_SPAWN_LOCATION		= 11;
	final static int BC_PASTR_ID			= 74;
	final static int BC_PASTR_LIST			= 75;
	
	
	
	int mySwarmID = 9999;
	int myOrders;
	int myPathChannel;
	int myPathPosition;
	int pathStatus;
	boolean moveStatus;
	MapLocation myLoc = null;
	MapLocation formLoc = null;
	Direction dir = null;

	
	// Enemy scan stuff
	int turnLastScannedEnemys = -999;
	int turnLastScannedPastrs = -999;
	Robot[] EnemyRobots;
	MapLocation[] EnemyPastrs;

	
	
	public HardFormationSwarmStrategy(RobotController in) { super(in); }

	public void run() {
		try {
			switch (rc.getType()) {
			case HQ:
				// Find Spawn direction
				myLoc = rc.getLocation(); 
				dir = myLoc.directionTo(rc.senseEnemyHQLocation());
				while ( !rc.canMove(dir) ) dir = dir.rotateLeft();
				
				
				// Initialize some variables
				rc.broadcast(BC_SWARM_ID, 1);
				rc.broadcast(BC_RALLY_POINT, -1);
				rc.broadcast(BC_ORDERS_ID, 1);
				rc.broadcast(BC_SPAWN_LOCATION, locToInt(myLoc.add(dir)));
				int pathChan = 300;
				rc.broadcast(BC_ORDERS_PATH_CHAN, pathChan);
				pathChan += 100;
				int invalidCount = 3;
				boolean continueFlag;
				// Herding variables
				int herdBotCount = 0;
				int[] herdBotLocs  = new int[25];
				int[] herdBotTypes = new int[25];
				CowLocation nextLoc = null;

				
				
				
				// Spawn the first robot
				rc.spawn(dir);
				
				
				// Initialize the Path Generators
				MapRender mren = new MapRender(rc);
				mren.blacklistEnemyBasePerimeter();
				mren.setFlag_CollectVoids(true);
				mren.init();
				PathGenerator wgr, gr = new DepthFirstPathing(rc,mren);
				double voidDensity = (((double)(mren.getVoids().size()))/((double)(rc.getMapWidth()*rc.getMapHeight())));
				if ( debugLevel >= 1 ) System.out.println("Void Density:  "+voidDensity);
				if ( voidDensity > .25 ) {
					// If void density in the map is too high, use normal pathing for the leader, as void-pathing will likely make pathing difficult.
					if ( debugLevel >= 1 ) System.out.println(" > Density Too high, using narrower path construction");
					wgr = gr;
				}
				else { 
					// Otherwise, use a padded version of the map for leader pathing.
					// This will allow the leader to move around the map in wider areas, 
					//   so that formation can easily be maintained 					
					if ( debugLevel >= 1 ) System.out.println(" > Using wide pathing for optimized formation movements.");
					wgr = new DepthFirstPathing(rc,mren.clone());
					wgr.getMapRender().voidPad(1);
				}
				
				rc.broadcast(BC_RALLY_POINT, locToInt(getRallyPoint(wgr.getMapRender())));
				if ( debugLevel >= 1 ) System.out.println("HQ Initialization Complete");				
				
				while ( true ) { 
					
					// Update some variables
					rc.broadcast(BC_SWARM_COUNT, rc.readBroadcast(BC_SWARM_ID)-1);
					rc.broadcast(BC_SWARM_ID, 1);
					continueFlag = false;
					if ( rc.readBroadcast(BC_SCATTER_FLAG) == 1 ) rc.broadcast(BC_SCATTER_FLAG, 0);


					// Check the status of the Pastrs
					int replaceMe = -1;
					for ( int i = 0; i < herdBotCount; i++ ) {
						if ( rc.readBroadcast(BC_PASTR_LIST+i) == 0 ) {
							replaceMe = i;
							//break;
						}
						else { rc.broadcast(BC_PASTR_LIST+i,rc.readBroadcast(BC_PASTR_LIST+i)-1); }
					}

					
					
					// Attack!!  (If enemies nearby)
					// TODO UPDATE this section to splash the enemy.
					if ( rc.isActive() ) {
						Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,15,rc.getTeam().opponent());
						if ( enemyRobots.length > 0 ) {
							rc.attackSquare(rc.senseRobotInfo(enemyRobots[0]).location);
						}
					}

					
					// Spawn a soldier
					if ( rc.senseRobotCount()<GameConstants.MAX_ROBOTS ) if ( rc.isActive() ) if ( rc.canMove(dir) ) {
						// Spawn the bot
						rc.spawn(dir);
						rc.broadcast(BC_ORDERS_ID, 0);

						// Post Orders for the bot
						rc.broadcast(BC_ORDERS_PATH_CHAN, pathChan);
						pathChan += 100;
						// Orders:
						//   1 = Soldier
						//   2 = Pastr
						//   3 = Noise Tower
						// TODO PUT IN A CASE to replace lost towers
						if ( replaceMe != -1 ) {
							// Need to replace a Pastr or a Noise Tower.
							rc.broadcast(BC_PASTR_ID, replaceMe);
							rc.broadcast(BC_ORDERS_ID, herdBotTypes[replaceMe]);
							rc.broadcast(BC_PASTR_TARGET, herdBotLocs[replaceMe]);
							continueFlag = true;
							postPathToPathChannel(gr.getPath(myLoc.add(dir),intToLoc(herdBotLocs[replaceMe])),pathChan-100);
						}
						else if ( Clock.getRoundNum() > 250 && herdBotCount < 2 && rc.readBroadcast(BC_SWARM_COUNT) >= 6 ) {
							if ( herdBotCount == 0 ) {
								nextLoc = cowLocs.remove();
								// Spawn a Noise Tower
								rc.broadcast(BC_PASTR_ID, herdBotCount);
								herdBotTypes[herdBotCount] = 3;
								herdBotLocs[herdBotCount] = locToInt(nextLoc.loc);
								rc.broadcast(BC_ORDERS_ID, 3);
								rc.broadcast(BC_PASTR_TARGET, herdBotLocs[herdBotCount]);
								rc.broadcast(herdBotLocs[herdBotCount]+4000,herdBotCount);
								herdBotCount++;
								continueFlag = true;
								postPathToPathChannel(gr.getPath(myLoc.add(dir),nextLoc.loc),pathChan-100);
							}
							else if ( herdBotCount == 1 ) {
								// Decide which location to build the Pastr in, there are 8 options.
								MapLocation tempLoc = null;
								MapLocation eHQ = rc.senseEnemyHQLocation();
								double tempDist = 0;
								MapLocation m1 = new MapLocation(nextLoc.loc.x-1,nextLoc.loc.y-1);	if ( !locIsOnMap(m1) ) m1 = eHQ;	if ( mren.terrainMatrix[m1.x][m1.y] == 99 ) m1 = eHQ;	
								MapLocation m2 = new MapLocation(nextLoc.loc.x-1,nextLoc.loc.y);	if ( !locIsOnMap(m2) ) m2 = eHQ;	if ( mren.terrainMatrix[m2.x][m2.y] == 99 ) m2 = eHQ;
								MapLocation m3 = new MapLocation(nextLoc.loc.x-1,nextLoc.loc.y+1);	if ( !locIsOnMap(m3) ) m3 = eHQ;	if ( mren.terrainMatrix[m3.x][m3.y] == 99 ) m3 = eHQ;
								MapLocation m4 = new MapLocation(nextLoc.loc.x,nextLoc.loc.y+1);	if ( !locIsOnMap(m4) ) m4 = eHQ;	if ( mren.terrainMatrix[m4.x][m4.y] == 99 ) m4 = eHQ;
								MapLocation m5 = new MapLocation(nextLoc.loc.x+1,nextLoc.loc.y+1);	if ( !locIsOnMap(m5) ) m5 = eHQ;	if ( mren.terrainMatrix[m5.x][m5.y] == 99 ) m5 = eHQ;
								MapLocation m6 = new MapLocation(nextLoc.loc.x+1,nextLoc.loc.y);	if ( !locIsOnMap(m6) ) m6 = eHQ;	if ( mren.terrainMatrix[m6.x][m6.y] == 99 ) m6 = eHQ;
								MapLocation m7 = new MapLocation(nextLoc.loc.x+1,nextLoc.loc.y-1);	if ( !locIsOnMap(m7) ) m7 = eHQ;	if ( mren.terrainMatrix[m7.x][m7.y] == 99 ) m7 = eHQ;
								MapLocation m8 = new MapLocation(nextLoc.loc.x,nextLoc.loc.y-1);	if ( !locIsOnMap(m8) ) m8 = eHQ;	if ( mren.terrainMatrix[m8.x][m8.y] == 99 ) m8 = eHQ;
								
								// Choose the one farthest from the EHQ.
								if ( m1.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m1; tempDist = m1.distanceSquaredTo(eHQ); }
								if ( m2.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m2; tempDist = m2.distanceSquaredTo(eHQ); }
								if ( m3.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m3; tempDist = m3.distanceSquaredTo(eHQ); }
								if ( m4.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m4; tempDist = m4.distanceSquaredTo(eHQ); }
								if ( m5.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m5; tempDist = m5.distanceSquaredTo(eHQ); }
								if ( m6.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m6; tempDist = m6.distanceSquaredTo(eHQ); }
								if ( m7.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m7; tempDist = m7.distanceSquaredTo(eHQ); }
								if ( m8.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m8; tempDist = m8.distanceSquaredTo(eHQ); }
								
								// Spawn a Pastr
								rc.broadcast(BC_PASTR_ID, herdBotCount);
								herdBotTypes[herdBotCount] = 2;
								herdBotLocs[herdBotCount] = locToInt(tempLoc);
								rc.broadcast(BC_ORDERS_ID, 2);
								rc.broadcast(BC_PASTR_TARGET, herdBotLocs[herdBotCount]);
								rc.broadcast(herdBotLocs[herdBotCount]+4000,herdBotCount);
								herdBotCount++;
								continueFlag = true;
								postPathToPathChannel(gr.getPath(myLoc.add(dir),tempLoc),pathChan-100);
							}
						}
						/* One Noise Tower
						else if ( Clock.getRoundNum() > 400 && herdBotCount < 4 && rc.readBroadcast(BC_SWARM_COUNT) >= 6 ) {
							if ( herdBotCount == 2 ) {
								if ( cowLocs.isEmpty() ) {
									rc.broadcast(BC_ORDERS_ID, 1);
									herdBotCount = 10;
								}
								else {
									// Get the location of the next NT
									boolean badLocation;
									do {
										badLocation = false;
										nextLoc = cowLocs.remove();
										
										for ( int i = 0; i < herdBotCount; i++) {
											if ( nextLoc.loc.distanceSquaredTo(intToLoc(herdBotLocs[i])) < 200 ) {
												badLocation = true;
											}
										}
									} while ( !cowLocs.isEmpty() && badLocation );
									// Spawn a Noise Tower
									rc.broadcast(BC_PASTR_ID, herdBotCount);
									herdBotTypes[herdBotCount] = 3;
									herdBotLocs[herdBotCount] = locToInt(nextLoc.loc);
									rc.broadcast(BC_ORDERS_ID, 3);
									rc.broadcast(BC_PASTR_TARGET, herdBotLocs[herdBotCount]);
									rc.broadcast(herdBotLocs[herdBotCount]+4000,herdBotCount);
									herdBotCount++;
									continueFlag = true;
									postPathToPathChannel(gr.getPath(myLoc.add(dir),nextLoc.loc),pathChan-100);
								}
							}
							else if ( herdBotCount == 3 ) {
								// Decide which location to build the Pastr in, there are 8 options.
								MapLocation tempLoc = null;
								MapLocation eHQ = rc.senseEnemyHQLocation();
								double tempDist = 0;
								MapLocation m1 = new MapLocation(nextLoc.loc.x-1,nextLoc.loc.y-1);	if ( !locIsOnMap(m1) ) m1 = eHQ;	if ( mren.terrainMatrix[m1.x][m1.y] == 99 ) m1 = eHQ;	
								MapLocation m2 = new MapLocation(nextLoc.loc.x-1,nextLoc.loc.y);	if ( !locIsOnMap(m2) ) m2 = eHQ;	if ( mren.terrainMatrix[m2.x][m2.y] == 99 ) m2 = eHQ;
								MapLocation m3 = new MapLocation(nextLoc.loc.x-1,nextLoc.loc.y+1);	if ( !locIsOnMap(m3) ) m3 = eHQ;	if ( mren.terrainMatrix[m3.x][m3.y] == 99 ) m3 = eHQ;
								MapLocation m4 = new MapLocation(nextLoc.loc.x,nextLoc.loc.y+1);	if ( !locIsOnMap(m4) ) m4 = eHQ;	if ( mren.terrainMatrix[m4.x][m4.y] == 99 ) m4 = eHQ;
								MapLocation m5 = new MapLocation(nextLoc.loc.x+1,nextLoc.loc.y+1);	if ( !locIsOnMap(m5) ) m5 = eHQ;	if ( mren.terrainMatrix[m5.x][m5.y] == 99 ) m5 = eHQ;
								MapLocation m6 = new MapLocation(nextLoc.loc.x+1,nextLoc.loc.y);	if ( !locIsOnMap(m6) ) m6 = eHQ;	if ( mren.terrainMatrix[m6.x][m6.y] == 99 ) m6 = eHQ;
								MapLocation m7 = new MapLocation(nextLoc.loc.x+1,nextLoc.loc.y-1);	if ( !locIsOnMap(m7) ) m7 = eHQ;	if ( mren.terrainMatrix[m7.x][m7.y] == 99 ) m7 = eHQ;
								MapLocation m8 = new MapLocation(nextLoc.loc.x,nextLoc.loc.y-1);	if ( !locIsOnMap(m8) ) m8 = eHQ;	if ( mren.terrainMatrix[m8.x][m8.y] == 99 ) m8 = eHQ;
								
								// Choose the one farthest from the EHQ.
								if ( m1.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m1; tempDist = m1.distanceSquaredTo(eHQ); }
								if ( m2.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m2; tempDist = m2.distanceSquaredTo(eHQ); }
								if ( m3.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m3; tempDist = m3.distanceSquaredTo(eHQ); }
								if ( m4.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m4; tempDist = m4.distanceSquaredTo(eHQ); }
								if ( m5.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m5; tempDist = m5.distanceSquaredTo(eHQ); }
								if ( m6.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m6; tempDist = m6.distanceSquaredTo(eHQ); }
								if ( m7.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m7; tempDist = m7.distanceSquaredTo(eHQ); }
								if ( m8.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m8; tempDist = m8.distanceSquaredTo(eHQ); }
								
								// Spawn a Pastr
								rc.broadcast(BC_PASTR_ID, herdBotCount);
								herdBotTypes[herdBotCount] = 2;
								herdBotLocs[herdBotCount] = locToInt(tempLoc);
								rc.broadcast(BC_ORDERS_ID, 2);
								rc.broadcast(BC_PASTR_TARGET, herdBotLocs[herdBotCount]);
								rc.broadcast(herdBotLocs[herdBotCount]+4000,herdBotCount);
								herdBotCount++;
								continueFlag = true;
								postPathToPathChannel(gr.getPath(myLoc.add(dir),tempLoc),pathChan-100);
							}
						}
						else if ( Clock.getRoundNum() > 600 && herdBotCount < 6 && rc.readBroadcast(BC_SWARM_COUNT) >= 6 ) {
							if ( herdBotCount == 4 ) {
								if ( cowLocs.isEmpty() ) {
									rc.broadcast(BC_ORDERS_ID, 1);
									herdBotCount = 10;
								}
								else {
									// Get the location of the next NT
									boolean badLocation;
									do {
										badLocation = false;
										nextLoc = cowLocs.remove();
										
										for ( int i = 0; i < herdBotCount; i++) {
											if ( nextLoc.loc.distanceSquaredTo(intToLoc(herdBotLocs[i])) < 200 ) {
												badLocation = true;
											}
										}
									} while ( !cowLocs.isEmpty() && badLocation );
									// Spawn a Noise Tower
									rc.broadcast(BC_PASTR_ID, herdBotCount);
									herdBotTypes[herdBotCount] = 3;
									herdBotLocs[herdBotCount] = locToInt(nextLoc.loc);
									rc.broadcast(BC_ORDERS_ID, 3);
									rc.broadcast(BC_PASTR_TARGET, herdBotLocs[herdBotCount]);
									rc.broadcast(herdBotLocs[herdBotCount]+4000,herdBotCount);
									herdBotCount++;
									continueFlag = true;
									postPathToPathChannel(gr.getPath(myLoc.add(dir),nextLoc.loc),pathChan-100);
								}
							}
							else if ( herdBotCount == 5 ) {
								// Decide which location to build the Pastr in, there are 8 options.
								MapLocation tempLoc = null;
								MapLocation eHQ = rc.senseEnemyHQLocation();
								double tempDist = 0;
								MapLocation m1 = new MapLocation(nextLoc.loc.x-1,nextLoc.loc.y-1);	if ( !locIsOnMap(m1) ) m1 = eHQ;	if ( mren.terrainMatrix[m1.x][m1.y] == 99 ) m1 = eHQ;	
								MapLocation m2 = new MapLocation(nextLoc.loc.x-1,nextLoc.loc.y);	if ( !locIsOnMap(m2) ) m2 = eHQ;	if ( mren.terrainMatrix[m2.x][m2.y] == 99 ) m2 = eHQ;
								MapLocation m3 = new MapLocation(nextLoc.loc.x-1,nextLoc.loc.y+1);	if ( !locIsOnMap(m3) ) m3 = eHQ;	if ( mren.terrainMatrix[m3.x][m3.y] == 99 ) m3 = eHQ;
								MapLocation m4 = new MapLocation(nextLoc.loc.x,nextLoc.loc.y+1);	if ( !locIsOnMap(m4) ) m4 = eHQ;	if ( mren.terrainMatrix[m4.x][m4.y] == 99 ) m4 = eHQ;
								MapLocation m5 = new MapLocation(nextLoc.loc.x+1,nextLoc.loc.y+1);	if ( !locIsOnMap(m5) ) m5 = eHQ;	if ( mren.terrainMatrix[m5.x][m5.y] == 99 ) m5 = eHQ;
								MapLocation m6 = new MapLocation(nextLoc.loc.x+1,nextLoc.loc.y);	if ( !locIsOnMap(m6) ) m6 = eHQ;	if ( mren.terrainMatrix[m6.x][m6.y] == 99 ) m6 = eHQ;
								MapLocation m7 = new MapLocation(nextLoc.loc.x+1,nextLoc.loc.y-1);	if ( !locIsOnMap(m7) ) m7 = eHQ;	if ( mren.terrainMatrix[m7.x][m7.y] == 99 ) m7 = eHQ;
								MapLocation m8 = new MapLocation(nextLoc.loc.x,nextLoc.loc.y-1);	if ( !locIsOnMap(m8) ) m8 = eHQ;	if ( mren.terrainMatrix[m8.x][m8.y] == 99 ) m8 = eHQ;
								
								// Choose the one farthest from the EHQ.
								if ( m1.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m1; tempDist = m1.distanceSquaredTo(eHQ); }
								if ( m2.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m2; tempDist = m2.distanceSquaredTo(eHQ); }
								if ( m3.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m3; tempDist = m3.distanceSquaredTo(eHQ); }
								if ( m4.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m4; tempDist = m4.distanceSquaredTo(eHQ); }
								if ( m5.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m5; tempDist = m5.distanceSquaredTo(eHQ); }
								if ( m6.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m6; tempDist = m6.distanceSquaredTo(eHQ); }
								if ( m7.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m7; tempDist = m7.distanceSquaredTo(eHQ); }
								if ( m8.distanceSquaredTo(eHQ) > tempDist ) { tempLoc = m8; tempDist = m8.distanceSquaredTo(eHQ); }
								
								// Spawn a Pastr
								rc.broadcast(BC_PASTR_ID, herdBotCount);
								herdBotTypes[herdBotCount] = 2;
								herdBotLocs[herdBotCount] = locToInt(tempLoc);
								rc.broadcast(BC_ORDERS_ID, 2);
								rc.broadcast(BC_PASTR_TARGET, herdBotLocs[herdBotCount]);
								rc.broadcast(herdBotLocs[herdBotCount]+4000,herdBotCount);
								herdBotCount++;
								continueFlag = true;
								postPathToPathChannel(gr.getPath(myLoc.add(dir),tempLoc),pathChan-100);
							}
						}
						/* */
						else {
							rc.broadcast(BC_ORDERS_ID, 1);						
						}
					} 
					
					
					// Update the paths, Starting with the Leader
					int rNum = Clock.getRoundNum();
					for ( int i = 200; i < pathChan; i += 100 ) {
						if ( debugLevel >= 3 ) System.out.println("Checking "+i+"\t:  "+rc.readBroadcast(i));
						if ( rc.readBroadcast(i) == 2 ) {
							MapLocation from = intToLoc(rc.readBroadcast(i+1));
							MapLocation to = intToLoc(rc.readBroadcast(i+2));
							if ( debugLevel >= 1 ) System.out.println("Calculating Path from "+from.toString()+" -> "+to.toString()+" for channel "+i);							
							rc.broadcast(i, 3);
							
							if ( i == 200 ) {
								if ( !postPathToPathChannel(wgr.getPath(from,to),i) ) {
									// If the attempt to get a leader path is unsucessful, switch the path generator to the non-padded version, as the map may simply be un-navigable.
									// TODO Alternatively, might want to consider re-calculating the path with the simple generator *only* for this path, to preserve some effectiveness, but also dont want to waste too many rounds calculating paths.  Research later.
									invalidCount--;
									if ( invalidCount == 0 ) wgr = gr;										
									from = intToLoc(rc.readBroadcast(i+1));
									to = intToLoc(rc.readBroadcast(i+2));
									rc.broadcast(i, 3);
									if ( debugLevel >= 1 ) System.out.println("RE-Calculating Path from "+from.toString()+" -> "+to.toString()+" for channel "+i);							
									postPathToPathChannel(gr.getPath(from,to),i);
								}
							}
							else postPathToPathChannel(gr.getPath(from,to),i);
							
							if ( rNum < Clock.getRoundNum() ) continueFlag = true;
							if ( continueFlag ) break;
						}
					}

					
					
					// DATA POST-PROCESSING					
					if ( processingCowLocations ) {
						// Process cow spawn locations, looking for good spots to build a Noise Tower
						continueFlag = true;
						processCowGrowthLocations(gr.getMapRender());
					}
					
					if ( continueFlag ) continue;
					rc.yield();
				}
			
			case SOLDIER:
				myOrders = rc.readBroadcast(BC_ORDERS_ID);
				while ( myOrders == 0 ) {
					rc.yield();
					myOrders = rc.readBroadcast(BC_ORDERS_ID);
				}
				
				switch (myOrders) {
				case 1: // Defender
					runSoldier_Defender();
					
					
				case 2: // Pastr
					runDefender_Pastr();
					
					
				case 3: // Noise Tower
					runDefender_NoiseTower();

					
				}
				
				
			case NOISETOWER:
				//I_Orders orders = new NoiseTower_CircularSweepUsingRadiusAndAngle(rc);
				I_Orders orders = new NoiseTower_CircularSweepUsingFarthestPointPrediction(rc);
				
				int myPastrID = rc.readBroadcast(locToInt(rc.getLocation())+4000);
				//I_Orders orders = new NoiseTower_DiagonalCrossSweepUsingRadiusAndAngle();
				while ( true ) {
					if ( rc.isActive() ) {
						rc.broadcast(BC_PASTR_LIST+myPastrID, 6);
						orders.executeOrders();
					}
					rc.yield();
				}

				
			case PASTR:
				gr = new DepthFirstPathing(rc);
				myPastrID = rc.readBroadcast(locToInt(rc.getLocation())+4000);
				while ( true ) {
					rc.broadcast(BC_PASTR_LIST+myPastrID, 20);
					for ( int i = 300; i < 4000; i += 100 ) {
						if ( debugLevel >= 3 ) System.out.println("Checking "+i+"\t:  "+rc.readBroadcast(i));
						if ( rc.readBroadcast(i) == 2 ) {
							MapLocation from = intToLoc(rc.readBroadcast(i+1));
							MapLocation to = intToLoc(rc.readBroadcast(i+2));
							if ( debugLevel >= 1 ) System.out.println("Calculating Path from "+from.toString()+" -> "+to.toString()+" for channel "+i);							
							rc.broadcast(i, 3);
							
							postPathToPathChannel(gr.getPath(from,to),i);
						}
					}
					//rc.yield();
				}

			default:
				break;
			}
		} catch (Exception e) {
			rc.breakpoint();
			e.printStackTrace();
		}
	}
	
	private void runDefender_NoiseTower() throws GameActionException {
		
		// For now, the Noise-Tower constructor's behavior will be exactly the same as the Defender_Pastr
		runDefender_Pastr();
	}
	
	private void runDefender_Pastr() throws GameActionException {
		int targInt = rc.readBroadcast(BC_PASTR_TARGET);
		MapLocation myTarget = intToLoc(targInt);
		myPathChannel = rc.readBroadcast(BC_ORDERS_PATH_CHAN);
		int myPastrID = rc.readBroadcast(BC_PASTR_ID);
		if ( debugLevel >= 1 ) {
			System.out.println("[+] My Orders: "+myOrders+"\tMyPastrID: "+myPastrID+"\tMyPastr: "+myTarget.toString());
			rc.setIndicatorString(1, "My Orders: "+myOrders+"     MyPastrID: "+myPastrID+"     MyPastr: "+myTarget.toString());
		}

		// Making formLoc == myTarget so that i can re-use followDefenderPathToLeader() 
		formLoc = myTarget;
		
		while (true) {
			pathStatus = rc.readBroadcast(myPathChannel);
			myLoc = rc.getLocation();
			rc.broadcast(BC_PASTR_LIST+myPastrID, 3);
		
			// Movement
			if ( rc.isActive() ) {
				if ( myLoc.equals(myTarget) ) {
					// TODO This section isnt strictly compliant with the method, but instead is adapted to fill the NT role as well.
					if ( myOrders == 2 ) {
						for ( int wait = 250; wait > 0; wait--) {
							rc.broadcast(BC_PASTR_LIST+myPastrID, 3);
							rc.yield();
						}
						//rc.selfDestruct();
						rc.construct(RobotType.PASTR);
					}
					if ( myOrders == 3 ) rc.construct(RobotType.NOISETOWER);
				}
				else followDefenderPathToLeader();
			}
			
			
			rc.yield();
		}
		
	}

	private void runSoldier_Defender() throws GameActionException {
		myPathChannel = rc.readBroadcast(BC_ORDERS_PATH_CHAN);
		MapLocation pastrTarget = null, rallyPoint = null;
		MapLocation center = new MapLocation(width/2,height/2);
		int tsi;
		rc.broadcast(BC_SWARM_ID, mySwarmID+1);
		if ( debugLevel >= 1 ) System.out.println("[+] My Orders: "+myOrders+"\tMyPathChan: "+myPathChannel+"\t");
		while ( true ) {
			// Get Swarm ID
			tsi = rc.readBroadcast(BC_SWARM_ID);
			if ( tsi <= mySwarmID ) {
				mySwarmID = tsi;
				rc.broadcast(BC_SWARM_ID, tsi+1);
			}

			
			
			// Update my variables
			myLoc = rc.getLocation();
			formLoc = getFormationLocation();
			dir = null;
			if ( formLoc != null && !locIsOnMap(formLoc) ) formLoc = intToLoc(rc.readBroadcast(BC_SWARM_LEAD_LOC));
			if ( mySwarmID == 1 ) pathStatus = rc.readBroadcast(200);
			else pathStatus = rc.readBroadcast(myPathChannel);
			moveStatus = rc.isActive();
			
			
			// Check my attack status
			if ( isAdjacentToEnemy() ) rc.broadcast(BC_ATTACK_ADJ, 1);
			if ( rc.isActive() ) {
				if ( mySwarmID == 1 ) {
					if ( rc.readBroadcast(BC_ATTACK_ADJ) == 1 || isAdjacentToEnemy() ) {
						// Flag the attack
						rc.broadcast(BC_ATTACK_FLAG, 1);
						
						// Get the attack target
						Robot target = getBestEnemyTarget();
						if ( target != null && isInShootingRange(target) ) {
							// Shoot it
							rc.attackSquare( rc.senseRobotInfo(target).location );
							moveStatus = false;
						}
					}
					else rc.broadcast(BC_ATTACK_FLAG, 0);
					rc.broadcast(BC_ATTACK_ADJ,0);
				}
				else {
					if ( myLoc.equals(formLoc) ) {
						if ( rc.readBroadcast(BC_ATTACK_FLAG) == 1) {
							// Get the attack target
							Robot target = getBestEnemyTarget();
							if ( target != null && isInShootingRange(target) ) {
								// Shoot it
								rc.attackSquare( rc.senseRobotInfo(target).location );
							}
						}
					}
					else if ( myLoc.distanceSquaredTo(formLoc) >= 25.0 ) {
						// If i am too far away, Attack anyway.  Dont want to be defenseless.
						Robot target = getBestEnemyTarget();
						if ( target != null && isInShootingRange(target) ) {
							// Shoot it
							rc.attackSquare( rc.senseRobotInfo(target).location );
						}
					}
				}
			}
			
			
			
			// Movement
			if ( mySwarmID == 1) {
				// If there is a distress call, prioritize that.

				// If there is an enemy nearby, prioritize that next.
				if ( moveStatus ) {
					Robot target = getBestEnemyTarget();
					if ( target != null ) {
						if ( rc.readBroadcast(BC_SWARM_COUNT) < 6 ) {
							if ( debugLevel >= 1 ) rc.setIndicatorString(2, "Avoiding nearby Enemy");
							dir = followLeaderPathTo(myLoc.add(myLoc.directionTo(rc.senseRobotInfo(target).location).opposite()));
							moveStatus = false;
						}
						else {
							if ( debugLevel >= 1 ) rc.setIndicatorString(2, "Moving to nearby Enemy");
							dir = followLeaderPathTo(rc.senseRobotInfo(target).location);
							moveStatus = false;
						}
					}
				}


				// If there is an enemy Pastr, prioritize that next.
				if ( moveStatus && rc.readBroadcast(BC_SWARM_COUNT) >= 7) {
					if ( turnLastScannedPastrs+10 < Clock.getRoundNum() ) {
						turnLastScannedPastrs = Clock.getRoundNum();
						EnemyPastrs = rc.sensePastrLocations(rc.getTeam().opponent());
						pastrTarget = null;
					}
					if ( pastrTarget == null ) {
						double pastrDist = 99999.9;
						for ( MapLocation n : EnemyPastrs ) {
							if ( n.distanceSquaredTo(myLoc) < pastrDist && n.distanceSquaredTo(rc.senseEnemyHQLocation()) > 25.0 ) {
								pastrTarget = n;
								pastrDist = n.distanceSquaredTo(myLoc);
							}
						}
					}
					if ( pastrTarget != null ) {
						if ( debugLevel >= 1 ) rc.setIndicatorString(2, "Moving towards a Pastr");
						dir = followLeaderPathTo(pastrTarget);
						moveStatus = false;
					}
				}


				// Otherwise, wait at the rally point.
				if ( moveStatus ) {
					if ( rallyPoint == null ) {
						int rp = rc.readBroadcast(BC_RALLY_POINT);
						if ( rp == -1 ) {
							// The rally point hasnt been calculated yet
							dir = takeStepTowards(center);
						}
						else {
							rallyPoint = intToLoc(rp);
						}
					}
					if ( rallyPoint != null ) {
						if ( myLoc.equals(rallyPoint) ) {
							if ( debugLevel >= 1 ) rc.setIndicatorString(2, "Waiting at Rally Point");									
						}
						else {
							if ( debugLevel >= 1 ) rc.setIndicatorString(2, "Heading to Rally Point");									
							dir = followLeaderPathTo(rallyPoint);
							moveStatus = false;									
						}
					}							
				}

				// Update my positional information
				myLoc = rc.getLocation();
				if ( dir != null ) rc.broadcast(BC_SWARM_DIR, directionToInt(dir)); 
				rc.broadcast(BC_SWARM_LEAD_LOC, locToInt(myLoc));
			}
			else if ( rc.isActive() ) {
				if ( rc.readBroadcast(BC_SCATTER_FLAG) == 1 ) {
					// Uh-oh, i'm blocking the leader.
					if ( debugLevel >= 1 ) {
						System.out.println("Scattering!!");
						rc.setIndicatorString(0, "Scattering!!!");
					}
					formLoc = intToLoc(rc.readBroadcast(BC_SPAWN_LOCATION));
					followDefenderPathToLeader();
				}
				else {
					if ( !myLoc.equals(formLoc) ) followDefenderPathToLeader();
				}
				myLoc = rc.getLocation();
			}

			
			// Update my status indicators
			if ( debugLevel >= 1 ) rc.setIndicatorString(1, "MyOrders: "+myOrders+"     Formation ID: "+mySwarmID+"     "+"PathChannel: "+myPathChannel+"     Attack Status: "+rc.readBroadcast(BC_ATTACK_FLAG));
			if ( mySwarmID == 1 ) {
				if ( dir != null ) rc.broadcast(BC_SWARM_LEAD_LOC, locToInt(myLoc.add(dir)));
			}
			
			
			// Check on my Bytecode Usage
			if ( debugLevel >= 2 ) {
				if ( Clock.getBytecodeNum() > 1000 ) {
					System.out.println("[>] BYTECODE LIMIT EXCEEDED:  "+Clock.getBytecodeNum());
				}
			}
			
			rc.yield();
		}
	}

	private boolean requestNewPath(int pathChan, int target) throws GameActionException {
		// Check to see if the points are valid 
		if ( myLoc.equals(intToLoc(rc.readBroadcast(200+1))) && intToLoc(target).equals(intToLoc(rc.readBroadcast(200+2))) ) {
			return false;
		}

		// Check to see if this path has already been requested 
		if ( pathStatus == 2 || pathStatus == 3 ) {
			return true;
		}
		
		if ( debugLevel >= 1 ) System.out.println(">>> Requesting New Path for channel "+pathChan);
		rc.broadcast(pathChan+1, locToInt(myLoc));
		rc.broadcast(pathChan+2, target);
		rc.broadcast(pathChan, 2);
		pathStatus = 2;
		myPathPosition = 3;			
		return true;				
	}
	
	private Direction followLeaderPathTo(MapLocation target) throws GameActionException {
		Direction dir = null;
		if ( debugLevel >= 2 ) System.out.println("[FLPT] START");
		
		if ( !target.equals(intToLoc(rc.readBroadcast(200+2))) ) {
			if ( debugLevel >= 2 ) System.out.println("[FLPT] > CHANGE TARGET");
			// My target has changed.
			requestNewPath(200,locToInt(target));
		}

		
		switch (pathStatus) {
		case 0: // I have no path
			if ( debugLevel >= 2 ) System.out.println("[FLPT] > CASE 0");
			// If i have no path, request one.
			requestNewPath(200,locToInt(target));
			return followLeaderPathTo(target);
			
		case 1: // I have a path			
			if ( debugLevel >= 2 ) System.out.println("[FLPT] > CASE 1");
			MapLocation myTarget = intToLoc(rc.readBroadcast(200+myPathPosition)); 
			if ( myLoc.equals(myTarget) ) {
				myPathPosition++;
				myTarget = intToLoc(rc.readBroadcast(200+myPathPosition));		
			}
			if ( rc.readBroadcast(200+myPathPosition) == -1 ) { myTarget = target; }
			if ( debugLevel >= 1 ) rc.setIndicatorString(0, "Moving towards -> "+myTarget.toString());
			dir = takeStepTowards(myTarget);
			
			if ( progressTarget != null && progressTarget.equals(myTarget) ) {
				if ( nonProgressTurnCount == 0 ) {
					if ( debugLevel >= 1 ) System.out.println("[FLPT]  NON-PROGRESS, REQUESTING NEW PATH");
					requestNewPath(200,locToInt(target));
				}
				else if ( myLoc.distanceSquaredTo(progressTarget) < progressDistance ) {
					progressDistance = myLoc.distanceSquaredTo(progressTarget);
				}
				else {
					nonProgressTurnCount--;
				}
			}
			else {
				progressTarget = myTarget;
				nonProgressTurnCount = 10;
				progressDistance = 99999.9;
			}

			if ( dir == null ) {
				// For some reason, my path current path didnt work.  Request a new one and move manually.
				if ( !requestNewPath(200,locToInt(target)) ) {
					// If the path is valid, but i still cant move, i am quite possibly boxed in.  Signal the troops to scatter.
					if ( debugLevel >= 1 ) rc.setIndicatorString(0, "Scattering the troops...");
					rc.broadcast(BC_SCATTER_FLAG, 1);
					return null;
				}
				return followLeaderPathTo(target);
			}
			break;
			
		case 2: // I have requested a new path
			if ( debugLevel >= 2 ) System.out.println("[FLPT] > CASE 2");
		case 3: // A new path is incoming
			if ( debugLevel >= 2 ) if ( pathStatus == 3 ) System.out.println("[FLPT] > CASE 3");
			// In the mean time, try and find my own way.
			if ( debugLevel >= 1 ) rc.setIndicatorString(0, "Waiting for a new Path");

			dir = takeStepTowards(target);
			break;
		}		

		if ( debugLevel >= 2 ) System.out.println("[FLPT] > END");

		myLoc = rc.getLocation();
		return dir;
	}
	
	// Pathing variables
	int nonProgressTurnCount = 10;
	double progressDistance = 99999.9;
	MapLocation progressTarget;
	
	
	private Direction followDefenderPathToLeader() throws GameActionException {
		Direction dir = null;
		MapLocation target = formLoc;
		int targInt = locToInt(formLoc);
		
		switch (pathStatus) {
		case 0: // I have no path
			// If i have no path, request one.
			requestNewPath(myPathChannel,targInt);
			return followDefenderPathToLeader();
			
		case 1: // I have a path			
			MapLocation myTarget = intToLoc(rc.readBroadcast(myPathChannel+myPathPosition)); 
			if ( myLoc.equals(myTarget) ) {
				myPathPosition++;
				myTarget = intToLoc(rc.readBroadcast(myPathChannel+myPathPosition));		
			}
			if ( rc.readBroadcast(myPathChannel+myPathPosition) == -1 ) { myTarget = target; }
			if ( debugLevel >= 1 ) rc.setIndicatorString(0, "Moving towards -> "+myTarget.toString());
			dir = takeStepTowards(myTarget);
			if ( progressTarget != null && progressTarget.equals(myTarget) ) {
				if ( nonProgressTurnCount == 0 ) {
					if ( debugLevel >= 1 ) System.out.println("NON-PROGRESS, REQUESTING NEW PATH");
					requestNewPath(myPathChannel,targInt);
				}
				else if ( myLoc.distanceSquaredTo(progressTarget) < progressDistance ) {
					progressDistance = myLoc.distanceSquaredTo(progressTarget);
				}
				else {
					nonProgressTurnCount--;
				}
			}
			else {
				progressTarget = myTarget;
				nonProgressTurnCount = 10;
				progressDistance = 99999.9;
			}
			

			if ( dir == null ) {
				// For some reason, my path current path didnt work.  
				// Check to see if the path is valid (start and end are correct, but i still cant go)
				// If so, there's no reason to request a new path, there's probably a bot in the way, just wait it out.
				if ( !myLoc.equals(intToLoc(rc.readBroadcast(myPathChannel+1))) || !target.equals(intToLoc(rc.readBroadcast(myPathChannel+2))) ) {
					// Check to see if a new path is already requested.
					if ( pathStatus != 2 && pathStatus != 3 ) {
						// Otherwise, request a new one and move manually.
						requestNewPath(myPathChannel,targInt);
					}					
					else if ( debugLevel >= 1 ) rc.setIndicatorString(0, "I cant seem to go anywhere...");
				}
				return followDefenderPathToLeader();
			}
			break;
			
		case 2: // I have requested a new path
		case 3: // A new path is incoming
			// In the mean time, try and find my own way.
			dir = takeStepTowards(target);
			break;
		}		
		
		myLoc = rc.getLocation();
		return dir;
	}
		
	/* OLD
	private Direction followLeaderPathTo(MapLocation target) throws GameActionException {
		// TODO Turned off all non-path movements, to try and fix some bugs at the cost of movement speed
		Direction dir = null;
		if ( debugLevel >= 2 ) System.out.println("LeaderPath -> "+target.toString());
		if ( !target.equals(intToLoc(rc.readBroadcast(200+2))) ) {
			if ( debugLevel >= 2 ) System.out.println("Leader  REQ PATH CHANGE -> "+target.toString());
			// Request a path
			myPathPosition = 3;
			rc.broadcast(200, 2);
			rc.broadcast(200+2, locToInt(target));
		}
		if ( rc.readBroadcast(200) != 1 ) {
			if ( debugLevel >= 2 ) System.out.println("   LP -> BC == 1  "+target.toString());
			dir = takeStepTowards(target);
			myLoc = rc.getLocation();
		}
		else {
			if ( debugLevel >= 2 ) System.out.println("   LP ->	 BC != 1  "+target.toString());
			MapLocation myTarget = intToLoc(rc.readBroadcast(200+myPathPosition)); 
			if ( myLoc.equals(myTarget) ) {
				if ( debugLevel >= 2 ) System.out.println("   LP -> MyLoc == Targ  "+target.toString());
				myPathPosition++;
				if ( rc.readBroadcast(200+myPathPosition) == -1 ) myTarget = target;
				else myTarget = intToLoc(rc.readBroadcast(200+myPathPosition));		
			}
			else if ( rc.readBroadcast(200+myPathPosition) == -1 ) { myTarget = target; }
			if ( debugLevel >= 2 ) System.out.println("   LP -> Step Towards  "+myTarget.toString());
			dir = takeStepTowards(myTarget);
			myLoc = rc.getLocation();
		}
		if ( dir == null && rc.readBroadcast(200) == 1 ) {
			System.out.println("   LP -> Unable to find my way, requesting a new path");
			int bc = rc.readBroadcast(200);
			if ( bc != 2 && bc != 3 ) rc.broadcast(200, 2);	
			return null;
			
		}
		return dir;
	}
	/* */
	
	
	/* OLD
	private void followDefenderPathToLeader() throws GameActionException {
		MapLocation myTarget;
		Direction sucess = null;
		if ( debugLevel >= 3 ) System.out.println("DefenderPath -> ");
		//System.out.println(rc.readBroadcast(myPathChannel));
		if ( rc.readBroadcast(myPathChannel) != 1 && rc.readBroadcast(myPathChannel) != 3 ) {
			// I am working on my own, with no path to follow, if i am too far away i should request a path.
			if ( rc.getLocation().distanceSquaredTo(formLoc) >= 20.0 ) {
				if ( debugLevel >= 1 ) rc.setIndicatorString(0,"Requested Path to leader.");
				rc.broadcast(myPathChannel, 2);
				myPathPosition = 3;
				rc.broadcast(myPathChannel+1, locToInt(rc.getLocation()));
				rc.broadcast(myPathChannel+2, locToInt(formLoc));
			}
			else { 
				if ( debugLevel >= 1 ) rc.setIndicatorString(0,"MyDistanceFromLeader: "+rc.getLocation().distanceSquaredTo(formLoc)); 
			}
			myTarget = formLoc;
			if ( !rc.getLocation().equals(myTarget) ) sucess = takeStepTowards(myTarget);
		}
		else {
			if ( debugLevel >= 1 ) rc.setIndicatorString(0,"Following the Path.");
			myTarget = intToLoc(rc.readBroadcast(myPathChannel+myPathPosition));
			if ( rc.readBroadcast(myPathChannel) == 3 ) {
				if ( debugLevel >= 1 ) rc.setIndicatorString(0,"Waiting for my Path.");
				myTarget = formLoc;
			}
			else if ( rc.readBroadcast(myPathChannel+myPathPosition) == -1 ) { 
				if ( debugLevel >= 1 ) rc.setIndicatorString(0,"Exiting the path now.");
				myTarget = formLoc;
				rc.broadcast(myPathChannel, 0);
			}
			else if ( myLoc.equals(myTarget) ) {
				if ( debugLevel >= 1 ) rc.setIndicatorString(0,"Going to the next node in the path.");
				myPathPosition++;
				if ( rc.readBroadcast(myPathChannel+myPathPosition) == -1 ) myTarget = formLoc;
				else myTarget = intToLoc(rc.readBroadcast(myPathChannel+myPathPosition));				
			}
			if ( !rc.getLocation().equals(myTarget) ) sucess = takeStepTowards(myTarget);
		}
		if ( sucess == null && rc.readBroadcast(myPathChannel) == 1 ) {
			if ( debugLevel >= 1 ) rc.setIndicatorString(0,"Unable to find my way, requesting a new path");
			int bc = rc.readBroadcast(myPathChannel);
			if ( bc != 2 && bc != 3 ) rc.broadcast(myPathChannel, 2);	
		}

	}
	/* */



	
	
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

	private MapLocation getFormationLocation() throws GameActionException {
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
	
	
	private boolean isAdjacentToEnemy() throws GameActionException {
		if ( turnLastScannedEnemys < (turnLastScannedEnemys = Clock.getRoundNum()) ) {
			EnemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
		}
		if ( EnemyRobots.length == 0 ) return false;
		for ( Robot n : EnemyRobots ) {
			if ( rc.senseRobotInfo(n).location.isAdjacentTo(myLoc) ) return true;
		}
		return false;
	}
	
	private boolean isInShootingRange(Robot r) throws GameActionException {
		if(rc.senseRobotInfo(r).location.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared) return true;
		return false;
	}
	
	private Robot getBestEnemyTarget() throws GameActionException {
		if ( turnLastScannedEnemys < (turnLastScannedEnemys = Clock.getRoundNum()) ) {
			EnemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
		}
		if ( EnemyRobots.length == 0 ) return null;
		Robot bestTarget = null; 
		boolean inRange = false;
		// Process the enemies
		for ( Robot n : EnemyRobots ) {
			RobotInfo ri = rc.senseRobotInfo(n);
			if ( debugLevel >= 2 ) System.out.print("[**] Detected "+n.getID()+", "+ri.type+"\tat "+ri.location.toString()+"\tHP: "+ri.health);
			if ( rc.senseRobotInfo(n).type == RobotType.HQ ) { 
				if ( debugLevel >= 2 ) System.out.println("    Ignoring it, because it is an HQ.");
				continue; 
			} // Ignore HQ's
			if ( rc.senseRobotInfo(n).location.distanceSquaredTo(eHQ) < 25.0 ) { 
				if ( debugLevel >= 2 ) System.out.println("    Ignoring it, because it is too close to the HQ.");
				continue; 
			} // Ignore HQ's
			if ( bestTarget == null ) {
				bestTarget = n;
				if ( isInShootingRange(n) ) inRange = true;
			}
			else {
				if ( rc.senseRobotInfo(bestTarget).health > rc.senseRobotInfo(n).health ) {
					if ( inRange ) {
						if ( isInShootingRange(n) ) bestTarget = n;
						else if ( debugLevel >= 2 ) System.out.print("    Ignoring it, because it is not in range");
					}
					else {
						bestTarget = n;
						if ( isInShootingRange(n) ) inRange = true;
					}
					
				}
				else if ( rc.senseRobotInfo(bestTarget).health == rc.senseRobotInfo(n).health ) {
					if ( rc.senseRobotInfo(bestTarget).location.distanceSquaredTo(myLoc) > rc.senseRobotInfo(n).location.distanceSquaredTo(myLoc) ) {
						bestTarget = n;
						if ( isInShootingRange(n) ) inRange = true;						
					}
					else {
						if ( debugLevel >= 2 ) System.out.print("    Ignoring it, because farther than my current target.");
					}
				}

				else {
					if ( debugLevel >= 2 ) System.out.print("    Ignoring it, because its health is too high.");
				}
			}
			if ( debugLevel >= 2 ) System.out.println();
		}
		return bestTarget;
	}
	
	
	private boolean postPathToPathChannel(Path p, int channel) throws GameActionException {
		int chan = channel+3;
		MapLocation next;
		if ( p != null ) next = p.nextLink();
		else {
			if ( debugLevel >= 1 ) System.out.println("Invalid Path");
			rc.broadcast(channel, 0);
			//rc.broadcast(channel+3,rc.readBroadcast(channel+2));
			return false;
		}
		
		if ( debugLevel >= 2 ) p.printPath();
		
		do {
			rc.broadcast(chan++, locToInt(next));
			next = p.nextLink();
		} while ( next != null );
		rc.broadcast(chan, -1);
		rc.broadcast(channel,1);
		if ( debugLevel >= 1 ) System.out.println(">> Path Channel "+channel+" Posted.");
		return true;
	}
	
	private MapLocation getRallyPoint(MapRender mren) {
		
		int width = rc.getMapWidth();
		int height = rc.getMapHeight();
		
		MapLocation HQ = rc.senseHQLocation();
		MapLocation EHQ = rc.senseEnemyHQLocation();
		double cX = (((HQ.x-EHQ.x)/2.0)+EHQ.x);
		double cY = (((HQ.y-EHQ.y)/2.0)+EHQ.y);
		
	
		double Slope = ((double)(HQ.y-EHQ.y))/((double)(HQ.x-EHQ.x));
		double invSlope = -1/Slope;
		
		double B = (cY-(invSlope*cX));
		
		if ( debugLevel >= 2 ) System.out.println("C: ["+cX+", "+cY+"]\tSlp: "+Slope+"\tInvSlp: "+invSlope+"\tB: "+B);
		
		LinkedList<MapLocation> sll = new SimpleLinkedList<MapLocation>();
		int point;
		MapLocation newPoint;
		if ( (HQ.x-EHQ.x) == 0 ) {
			if ( debugLevel >= 2 ) System.out.println("Calculating all points (Horizontal Asymptote)");
			for ( int i = 0; i < width; i++) {
				newPoint = new MapLocation(i,(int)cY);
				if ( locIsOnMap(newPoint) ) {
					if ( debugLevel >= 3 ) System.out.print(" > "+newPoint.toString());
					if ( mren.terrainMatrix[newPoint.x][newPoint.y] != 99 ) {
						sll.add(newPoint);					
					}
					else {
						if ( debugLevel >= 3 ) System.out.print("\tIs a void.");
					}
					if ( debugLevel >= 3 ) System.out.println();
				}
			}
		}
		else if ( (HQ.y-EHQ.y) == 0 ) {
			if ( debugLevel >= 2 ) System.out.println("Calculating all points (Vertical Asymptote)");
			for ( int i = 0; i < height; i++) {
				newPoint = new MapLocation((int)cX,i);
				if ( locIsOnMap(newPoint) ) {
					if ( debugLevel >= 3 ) System.out.print(" > "+newPoint.toString());
					if ( mren.terrainMatrix[newPoint.x][newPoint.y] != 99 ) {
						sll.add(newPoint);					
					}
					else {
						if ( debugLevel >= 3 ) System.out.print("\tIs a void.");
					}
					if ( debugLevel >= 3 ) System.out.println();
				}
			}
		}
		else if ( invSlope <= 1 && invSlope >= -1 ) {
			// If the slope is less than or equal to one, all the points can be computed using the X-Axis.
			if ( debugLevel >= 2 ) System.out.println("Calculating all points (From X)");
			for ( int i = 0; i < width; i++) {
				point = (int)Math.round((i*invSlope)+B);
				newPoint = new MapLocation(i,point);
				if ( locIsOnMap(newPoint) ) {
					if ( debugLevel >= 3 ) System.out.print(" > "+newPoint.toString());
					if ( mren.terrainMatrix[newPoint.x][newPoint.y] != 99 ) {
						sll.add(newPoint);					
					}
					else {
						if ( debugLevel >= 3 ) System.out.print("\tIs a void.");
					}
					if ( debugLevel >= 3 ) System.out.println();
				}
			}
		}
		else {
			// If the slope is GREATER than one,, all the points can be computed using the Y-Axis.
			if ( debugLevel >= 2 ) System.out.println("Calculating all points (From Y)");
			for ( int i = 0; i < height; i++) {
				point = (int)Math.round(((((double)i)-B)/invSlope));
				newPoint = new MapLocation(point,i);
				if ( locIsOnMap(newPoint) ) {
					if ( debugLevel >= 3 ) System.out.print(" > "+newPoint.toString());
					if ( mren.terrainMatrix[newPoint.x][newPoint.y] != 99 ) {
						sll.add(newPoint);					
					}
					else {
						if ( debugLevel >= 3 ) System.out.print("\tIs a void.");
					}
					if ( debugLevel >= 3 ) System.out.println();
				}
			}
		}
		MapLocation mapCenter = new MapLocation((int)cX,(int)cY);
		MapLocation closestPoint = null;
		double dist = 99999.9;
		for ( MapLocation n : sll) {
			if ( mapCenter.distanceSquaredTo(n) < dist ) {
				dist = mapCenter.distanceSquaredTo(n);
				closestPoint = n;
			}
		}
		
		if ( debugLevel >= 1 ) System.out.println("Calculated Rally Point:  "+closestPoint.toString());
		
		return closestPoint;
	}
	
	
	// This method will process using class variables, in order to allow it to return and resume between rounds.
	int pcgX = 0;
	int pcgY = 0;
	boolean processingCowLocations = true;
	double[][] cowSpawnMatrix = null;
	PriorityQueue<CowLocation> cowLocs = new PriorityQueue<CowLocation>();
	
	private void processCowGrowthLocations(MapRender in) {
		if ( cowSpawnMatrix == null ) cowSpawnMatrix = rc.senseCowGrowth();
		int i = pcgX;
		int j = pcgY;
		int k;
		int startTurn = Clock.getRoundNum();
		CowLocation cowLoc;
		double temp;
		for ( ; i < width; i++ ) {
			for ( ; j < height; j++ ) {
				
				// If this location is a void, a-void (Haha) it.
				if ( in.terrainMatrix[i][j] == 99 ) { continue; }
				
				// If there is no cow growth at this location, skip it.  
				// There may still be growth nearby, but we can do better, and it's not worth the bytecodes.
				if ( cowSpawnMatrix[i][j] < 0.00001 ) { continue; }
				
				temp = 0;
				
				// Cross-sect left 
				for ( k = 1; k < 18; k++ ) {
					// If we reach the edge of the map, break;
					if ( i-k < 0 ) break;
					// If we've reached a void, break.  It will be difficult to farm that direction.
					if ( in.terrainMatrix[i-k][j] == 99 ) break;
					cowSpawnMatrix[i][j] += in.cowSpawnRate[i-k][j];
				}
				
				// Cross-sect right
				for ( k = 1; k < 18; k++ ) {
					// If we reach the edge of the map, break;
					if ( i+k == width ) break;
					// If we've reached a void, break.  It will be difficult to farm that direction.
					if ( in.terrainMatrix[i+k][j] == 99 ) break;
					cowSpawnMatrix[i][j] += in.cowSpawnRate[i+k][j];
				}

				// Cross-sect up
				for ( k = 1; k < 18; k++ ) {
					// If we reach the edge of the map, break;
					if ( j-k < 0 ) break;
					// If we've reached a void, break.  It will be difficult to farm that direction.
					if ( in.terrainMatrix[i][j-k] == 99 ) break;
					cowSpawnMatrix[i][j] += in.cowSpawnRate[i][j-k];
				}

				// Cross-sect down
				for ( k = 1; k < 18; k++ ) {
					// If we reach the edge of the map, break;
					if ( j+k == height ) break;
					// If we've reached a void, break.  It will be difficult to farm that direction.
					if ( in.terrainMatrix[i][j+k] == 99 ) break;
					cowSpawnMatrix[i][j] += in.cowSpawnRate[i][j+k];
				}
				
				
				// Add this location to the queue.
				cowLoc = new CowLocation(new MapLocation(i,j),cowSpawnMatrix[i][j]);
				cowLocs.add(cowLoc);
				
				if ( debugLevel >= 3 ) System.out.println("Processed Location:  ["+i+", "+j+"] == "+cowSpawnMatrix[i][j]);
				
				// Check the turn counter
				if ( Clock.getRoundNum() > startTurn ) {
					pcgX = i;
					pcgY = j+1;
					return;
				}
			}
			j = 0;
		}
		if ( debugLevel >= 1 ) System.out.println("Completed Cow Growth Processing");
		processingCowLocations = false;
	}
	
	public class CowLocation implements Comparable<CowLocation> {
		
		MapLocation loc;
		double rate;
		
		public CowLocation(MapLocation in, double cowRate) {
			loc = in;
			rate = cowRate;
		}

		@Override
		public int compareTo(CowLocation comp) {
			if ( rate < comp.rate ) return 1;
			if ( rate > comp.rate ) return -1;
			return 0;
		}
		
	}

}
