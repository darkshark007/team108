package team108.Strategies;

import java.util.LinkedList;
import java.util.PriorityQueue;

import team108.Graph.MapRender;
import team108.Graph.SimpleLinkedList;
import team108.Orders.*;
import team108.Path.DepthFirstPathing;
import team108.Path.DirectWithOptimizedBuggingPathGenerator;
import team108.Path.ObstaclePointPathGenerator;
import team108.Path.Path;
import team108.Path.PathGenerator;
import team108.Strategies.PassiveHerdingStrategy.CowLocation;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class HardFormationSwarmStrategyV2 extends Strategy {

	final static int BC_SWARM_COUNT			= 0;
	final static int BC_SWARM_FORM_COUNT	= 1;
	final static int BC_SWARM_FORM_COUNTER	= 15;
	final static int BC_SWARM_ID			= 2;
	final static int BC_SWARM_DIR			= 3;
	final static int BC_SWARM_LEAD_LOC		= 4;
	final static int BC_ORDERS_ID			= 5;
	final static int BC_ORDERS_PATH_CHAN	= 6;
	final static int BC_ORDERS_PASTR_TARGET	= 7;
	final static int BC_ORDERS_NOISE_TARGET	= 8;	
	final static int BC_ORDERS_MANIC_TARGET	= 9;	
	final static int BC_ATTACK_ADJ			= 10;
	final static int BC_ATTACK_FLAG			= 11;
	final static int BC_RALLY_POINT			= 12;
	final static int BC_SCATTER_FLAG		= 13;
	//final static int BC_PASTR_TARGET		= 73;
	final static int BC_SPAWN_LOCATION		= 14;
	final static int BC_PASTR_SPAWN			= 16;
	final static int BC_DISTRESS_ACTIVE		= 17;
	final static int BC_DISTRESS_PENDING	= 18;
	final static int BC_ORDERS_FLEE			= 19;
	final static int BC_ALT_RALLY			= 20;
	final static int BC_ALT_USE				= 21;
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
	CowLocation nextLoc = null;
	final int distressTurnLife = 25;


	
	// Enemy scan stuff
	int turnLastScannedEnemys = -999;
	int turnLastScannedPastrs = -999;
	Robot[] EnemyRobots;
	MapLocation[] EnemyPastrs;

	
	
	public HardFormationSwarmStrategyV2(RobotController in) { super(in); }

	public void run() {
		try {
			
			myLoc = rc.getLocation();
			
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
				rc.broadcast(BC_PASTR_SPAWN, 85);
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

				
				
				
				// Spawn the first robot
				rc.spawn(dir);
				
				
				// Initialize the Path Generators
				MapRender mren = new MapRender(rc);
				mren.setFlag_CollectVoids(true);
				mren.init();
				mren.blacklistEnemyBasePerimeter();
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
				if ( debugLevel >= 2 ) wgr.getMapRender().printMap();
				
				MapLocation rallyPoint;
				rallyPoint = getRallyPoint(wgr.getMapRender());
				if ( rallyPoint == null ) rallyPoint = getRallyPoint(gr.getMapRender());
				rc.broadcast(BC_RALLY_POINT, locToInt(rallyPoint));
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
							break;
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
							if ( debugLevel >= 1 ) System.out.println(">> SPAWNING BOT:  Replacement for PastrID "+replaceMe);
							// Need to replace a Pastr or a Noise Tower.
							rc.broadcast(BC_PASTR_ID, replaceMe);
							rc.broadcast(BC_ORDERS_ID, herdBotTypes[replaceMe]);
							if ( herdBotTypes[replaceMe] == 2 ) rc.broadcast(BC_ORDERS_PASTR_TARGET, herdBotLocs[replaceMe]);
							if ( herdBotTypes[replaceMe] == 3 ) rc.broadcast(BC_ORDERS_NOISE_TARGET, herdBotLocs[replaceMe]);
							if ( herdBotTypes[replaceMe] == 4 ) rc.broadcast(BC_ORDERS_MANIC_TARGET, herdBotLocs[replaceMe]);
							
							continueFlag = true;
							postPathToPathChannel(gr.getPath(myLoc.add(dir),intToLoc(herdBotLocs[replaceMe])),pathChan-100);
						}
						else {
							if ( debugLevel >= 1 ) System.out.println(">> SPAWNING BOT:  Soldier");
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
					if ( Clock.getRoundNum() > 500 && nextLoc == null ) {
						setUpNewPastr(gr.getMapRender());
						
						herdBotCount += 3;
						
						continueFlag = true;
						rc.yield();
						
						herdBotLocs[0] = rc.readBroadcast(BC_ORDERS_NOISE_TARGET);
						herdBotLocs[1] = rc.readBroadcast(BC_ORDERS_PASTR_TARGET);
						herdBotLocs[2] = rc.readBroadcast(BC_ORDERS_MANIC_TARGET);
						
						herdBotTypes[0] = 3;
						herdBotTypes[1] = 2;
						herdBotTypes[2] = 4;
						
					}
					if ( processingCowLocations ) {
						// Process cow spawn locations, looking for good spots to build a Noise Tower
						continueFlag = true;
						processCowGrowthLocations(gr.getMapRender());
					}
					else {
						if ( rc.readBroadcast(BC_SWARM_COUNT) > 6 && nextLoc == null ) {
							setUpNewPastr(gr.getMapRender());
							
							herdBotCount += 3;
							
							continueFlag = true;
							rc.yield();
							
							herdBotLocs[0] = rc.readBroadcast(BC_ORDERS_NOISE_TARGET);
							herdBotLocs[1] = rc.readBroadcast(BC_ORDERS_PASTR_TARGET);
							herdBotLocs[2] = rc.readBroadcast(BC_ORDERS_MANIC_TARGET);
							
							herdBotTypes[0] = 3;
							herdBotTypes[1] = 2;
							herdBotTypes[2] = 4;
							
						}
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

					
				case 4: // Manic_Bot
					runDefender_ManicBot();

					
				}
				
				
			case NOISETOWER:
				// Working
				//I_Orders orders = new NoiseTower_CircularSweepUsingRadiusAndAngle(rc);
				int myPastrID = rc.readBroadcast(locToInt(rc.getLocation())+4000);
				myLoc = rc.getLocation();
				if ( debugLevel >= 1 ) {
					System.out.println("[+] My Orders: "+2+"\tMyPastrID: "+myPastrID+"\tMyPastr: "+myLoc);
					rc.setIndicatorString(1, "My Orders: "+2+"     MyPastrID: "+myPastrID+"     MyPastr: "+myLoc);
				}
				
				// Testing
				rc.broadcast(BC_PASTR_LIST+myPastrID, 25);
				
				//I_Orders orders = new NoiseTower_CircularSweepUsingFarthestPointPrediction(rc);
				//NoiseTower ord = new NoiseTower_WillsNoiseTower(rc);
				NoiseTower ord = new NoiseTower_CircularSweepUsingRadiusAndAngle(rc);
				NoiseTower_DepthFirstPathGenerator ord2 = new NoiseTower_DepthFirstPathGenerator(rc);
				
				//I_Orders orders = new NoiseTower_DiagonalCrossSweepUsingRadiusAndAngle();
				while ( true ) {
					rc.broadcast(BC_PASTR_LIST+myPastrID, 6);

					rc.broadcast(BC_PASTR_SPAWN, ord.getTurnsTillConvergence());
					
					
					// Check for Distress Signal
					Robot closestTarget = getClosestEnemyTarget();
					if ( closestTarget != null ) {
						// Send out a distress call
						sendDistressCall(50,rc.senseRobotInfo(closestTarget).location.distanceSquaredTo(myLoc),myLoc);
					}

					
					if ( rc.isActive() ) ord.executeOrders();
					// Handle the data processing.
					int sTurn = Clock.getRoundNum();
					if ( debugLevel >= 3 ) System.out.println("Density:  "+ord2.getPathDensity());
					if ( ord2.getPathDensity() < 0.5 ) {
						ord = ord2;
					}
					ord2.processData(); 
					if ( sTurn == Clock.getRoundNum() ) rc.yield();
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
					
					// Check for Distress Signal
					Robot closestTarget = getClosestEnemyTarget();
					if ( closestTarget != null ) {
						// Send out a distress call
						sendDistressCall(80,rc.senseRobotInfo(closestTarget).location.distanceSquaredTo(myLoc),myLoc);
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
	
	private void runDefender_ManicBot() throws GameActionException {
		
		// Now, wait for orders.
		MapLocation myPastrTarget = intToLoc(rc.readBroadcast(BC_ORDERS_MANIC_TARGET));
		MapLocation myPastr = intToLoc(rc.readBroadcast(BC_ORDERS_PASTR_TARGET));
		myPathChannel = rc.readBroadcast(BC_ORDERS_PATH_CHAN);
		int myPastrID = rc.readBroadcast(BC_PASTR_ID);
		if ( debugLevel >= 1 ) {
			System.out.println("[+] My Orders: "+myOrders+"\tMyPastrID: "+myPastrID+"\tMyPastr: "+myPastrTarget.toString());
			rc.setIndicatorString(1, "My Orders: "+myOrders+"     MyPastrID: "+myPastrID+"     MyPastr: "+myPastrTarget.toString());
		}

		boolean inPosition = false;
		boolean pastrIsDead = false;
		Robot myPastrRobot = null;
		RobotInfo rif;
		
		while ( true ) {

			// Update my variables
			myLoc = rc.getLocation();
			pathStatus = rc.readBroadcast(myPathChannel);
			rc.broadcast(BC_PASTR_LIST+myPastrID, 5);
			rc.broadcast(BC_PASTR_LIST+myPastrID-1, 5);
			
			
			
			// Check for Distress Signal
			Robot target = getClosestEnemyTarget();
			if ( target != null ) {
				// Send out a distress call
				sendDistressCall(25,rc.senseRobotInfo(target).location.distanceSquaredTo(myLoc),myLoc);
			}


			
			// Check for Enemies
			RobotInfo tInf = null;
			if ( target != null ) tInf = rc.senseRobotInfo(target);

			
			
			if ( pastrIsDead && rc.isActive() ) {
				// If enemy is detected and pastr is dead, flee towards the swarm for protection
				if ( target != null ) followPath(intToLoc(rc.readBroadcast(BC_SWARM_LEAD_LOC)),rc.senseRobotInfo(target).location);
				else {
					if (myLoc.equals(myPastr) ) {
						rc.construct(RobotType.PASTR);
					}
					else followPath(myPastr);
				}
			}
			
			
			// If an enemy is detected and close enough, and i am in position, finish off the Pastr
			if ( rc.isActive() && target != null && inPosition && tInf.location.distanceSquaredTo(myLoc) <= 25.0 ) {
				// If i dont already have the object for my Pastr robot, try to find it.
				if ( myPastrRobot == null ) {
					Robot[] r = rc.senseNearbyGameObjects(Robot.class);
					for ( Robot n : r ) {
						rif = rc.senseRobotInfo(n);
						//System.out.println()
						if ( rif.type.equals(RobotType.PASTR) && rif.location.isAdjacentTo(myLoc) ) myPastrRobot = n;
					}
				}

				if ( myPastrRobot != null ) {
					if ( rc.canSenseObject(myPastrRobot) ) {
						rif = rc.senseRobotInfo(myPastrRobot);
						rc.attackSquare(rif.location);
						if ( !rc.canSenseObject(myPastrRobot) ) {
							System.out.println("Pastr is DEAD!!!!");
							pastrIsDead = true;
						}
					}
				}
			}

			
			/*
			// If an enemy is detected, move to avoid it.
			if ( rc.isActive() && target != null && !inPosition ) {
				MapLocation awayTarget = myLoc.add(tInf.location.directionTo(myLoc));
				takeStepTowards(awayTarget);				
				System.out.println("Enemy Detected at "+tInf.location+"!  Moving away, towards "+awayTarget);
			}
			/* */

			
			
			// Movement to get in position
			if ( rc.isActive() && !inPosition ) {
				if ( myLoc.equals(myPastrTarget) ) { inPosition = true; }
				else followPath(myPastrTarget);
				System.out.println("In Position:  "+inPosition+"     PT: "+myPastrTarget);
			}

			
			
			// Attack and KILL MY PASTR....almost.
			System.out.println(( inPosition == true )+"  && "+( rc.isActive() )+" == "+(( inPosition == true ) && ( rc.isActive() )));
			if ( ( inPosition == true ) && ( rc.isActive() ) ) {
				// If i dont already have the object for my Pastr robot, try to find it.
				if ( myPastrRobot == null ) {
					Robot[] r = rc.senseNearbyGameObjects(Robot.class);
					for ( Robot n : r ) {
						rif = rc.senseRobotInfo(n);
						//System.out.println()
						if ( rif.type.equals(RobotType.PASTR) && rif.location.isAdjacentTo(myLoc) ) myPastrRobot = n;
					}
				}
				
				if ( myPastrRobot != null ) {
					if ( rc.canSenseObject(myPastrRobot) ) {
						rif = rc.senseRobotInfo(myPastrRobot);
						if ( rif.health > 10.0 ) rc.attackSquare(rif.location);
					}
				}
			}
			
			
			
			
			
			rc.yield();
		}
	}

	private void runDefender_NoiseTower() throws GameActionException {
		
		// For now, the Noise-Tower constructor's behavior will be exactly the same as the Defender_Pastr
		runDefender_Pastr();
	}
	
	private void setUpNewPastr(MapRender mren) throws GameActionException {
		// Time to give the squad its orders
		if ( debugLevel >= 1 ) System.out.println("[Cow] Queue Size:  "+cowLocs.size());
		
		// Next, select a location that doesnt interfere with any of the current locations
		boolean badLocation;
		MapLocation cur;
		do {
			badLocation = false;
			nextLoc = cowLocs.remove();
			
			// Look for a new rally point for the swarm
			// Starting from the loc, move 10 spaces towards the EHQ
			cur = nextLoc.loc;
			for ( int i = 10; i > 0; i--) {
				cur = cur.add(cur.directionTo(eHQ));
				if ( rc.senseTerrainTile(cur).equals(TerrainTile.VOID) ) {
					badLocation = true;
					break;
				}
			}
			
			// Next, see if this area has at least enough room to hold the swarm.
			Direction curD = cur.directionTo(eHQ);
			for ( int i = 8; i > 0; i-- ) {
				curD.rotateLeft();
				if ( rc.senseTerrainTile(cur.add(curD)).equals(TerrainTile.VOID) ) {
					badLocation = true;
					break;
				}
			}
			
			if ( debugLevel >= 1 ) System.out.println("Trying "+nextLoc.loc+"...");
		} while ( !cowLocs.isEmpty() && badLocation );
	
		rc.broadcast(BC_ORDERS_NOISE_TARGET, locToInt(nextLoc.loc));
		
		rc.broadcast(BC_ALT_RALLY, locToInt(cur));
		rc.broadcast(BC_ALT_USE, 1);
		
		// Decide which location to build the Pastr in, there are 8 options.
		MapLocation tempLoc = null;
		MapLocation eHQ = rc.senseEnemyHQLocation();
		double tempDist = 0;
		// Check to see if each is on the map AND not a void
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

		rc.broadcast(BC_ORDERS_PASTR_TARGET, locToInt(tempLoc));
		
		// Get the Maniac destination
		Direction tempDir = tempLoc.directionTo(nextLoc.loc).rotateLeft();

		MapLocation temp = tempLoc.add(tempDir);
		TerrainTile t = rc.senseTerrainTile(temp);

		while ( t.equals(TerrainTile.OFF_MAP) || t.equals(TerrainTile.VOID)) {
			tempDir = tempDir.rotateLeft();
			temp = tempLoc.add(tempDir);
			t = rc.senseTerrainTile(temp);
		}
		
		rc.broadcast(BC_ORDERS_MANIC_TARGET, locToInt(temp));
	}

	
	private void runDefender_Pastr() throws GameActionException {
		int targInt = 0;
		if ( myOrders == 2 ) targInt = rc.readBroadcast(BC_ORDERS_PASTR_TARGET);
		else if ( myOrders == 3 ) targInt = rc.readBroadcast(BC_ORDERS_NOISE_TARGET);
		MapLocation myTarget = intToLoc(targInt);
		myPathChannel = rc.readBroadcast(BC_ORDERS_PATH_CHAN);
		int myPastrID = rc.readBroadcast(BC_PASTR_ID);
		rc.broadcast(targInt+4000,myPastrID);
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
			if ( debugLevel >= 3 ) System.out.println("Broadcasting:: "+myPastrID);
			
			
			// Check for Distress Signal
			Robot closestTarget = getClosestEnemyTarget();
			if ( closestTarget != null ) {
				// Send out a distress call
				sendDistressCall(20,rc.senseRobotInfo(closestTarget).location.distanceSquaredTo(myLoc),myLoc);
			}

		
			// Movement
			if ( rc.isActive() ) {
				if ( myLoc.equals(myTarget) ) {
					// TODO This section isnt strictly compliant with the method, but instead is adapted to fill the NT role as well.
					if ( myOrders == 2 ) {
						if ( rc.readBroadcast(BC_PASTR_SPAWN) <= 50 ) rc.construct(RobotType.PASTR);
					}
					if ( myOrders == 3 ) rc.construct(RobotType.NOISETOWER);
				}
				else followPath(formLoc);
			}
			
			
			rc.yield();
		}
		
	}

	private void runSoldier_Defender() throws GameActionException {
		myPathChannel = rc.readBroadcast(BC_ORDERS_PATH_CHAN);
		MapLocation pastrTarget = null, rallyPoint = null;
		MapLocation center = new MapLocation(width/2,height/2);
		boolean flee;
		DistressSignal currentDistressCall = new DistressSignal(0);
		int tsi;
		rc.broadcast(BC_SWARM_ID, mySwarmID+1);
		int timeSinceFormLoc = 0;
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
			flee = false;
			if ( mySwarmID == 1 ) {
				rc.broadcast(BC_SWARM_FORM_COUNT, rc.readBroadcast(BC_SWARM_FORM_COUNTER));
				rc.broadcast(BC_SWARM_FORM_COUNTER,0);
			}
			else {
				formLoc = getFormationLocation();
				timeSinceFormLoc--;
				if ( myLoc.equals(formLoc) ) {
					timeSinceFormLoc = 15;
				}
				if ( timeSinceFormLoc > 0) rc.broadcast(BC_SWARM_FORM_COUNTER, rc.readBroadcast(BC_SWARM_FORM_COUNTER)+1);
				if ( formLoc != null && !locIsOnMap(formLoc) ) formLoc = intToLoc(rc.readBroadcast(BC_SWARM_LEAD_LOC));
			}
			dir = null;
			if ( mySwarmID == 1 ) pathStatus = rc.readBroadcast(200);
			else pathStatus = rc.readBroadcast(myPathChannel);
			moveStatus = rc.isActive();
			
			
			// Check my attack status
			if ( isAdjacentToEnemy() ) rc.broadcast(BC_ATTACK_ADJ, 1);
			if ( rc.isActive() ) {
				if ( mySwarmID == 1 ) {
					Robot target = getBestEnemyTarget();

					if ( target != null && rc.senseRobotInfo(target).location.distanceSquaredTo(eHQ) < 25.0 && isInShootingRange(target) ) {
						rc.broadcast(BC_ATTACK_FLAG, 1);
						rc.attackSquare( rc.senseRobotInfo(target).location );
						moveStatus = false;
					}
					else if ( rc.readBroadcast(BC_ATTACK_ADJ) == 1 || isAdjacentToEnemy() ) {
						// Flag the attack
						rc.broadcast(BC_ATTACK_FLAG, 1);
						
						// Get the attack target
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
					if ( timeSinceFormLoc > 0 ) {
						if ( rc.readBroadcast(BC_ATTACK_FLAG) == 1) {
							// Get the attack target
							Robot target = getBestEnemyTarget();
							if ( target != null ) {
								if ( isInShootingRange(target) ) {
									// Shoot it
									rc.attackSquare( rc.senseRobotInfo(target).location );
								}
								else takeStepTowards(rc.senseRobotInfo(target).location);
								moveStatus = false;
							}
						}
					}
					else if ( myLoc.distanceSquaredTo(formLoc) >= 25.0 ) {
						// If i am too far away, Attack anyway.  Dont want to be defenseless.
						Robot target = getBestEnemyTarget();
						if ( target != null && isInShootingRange(target) ) {
							// Shoot it
							rc.attackSquare( rc.senseRobotInfo(target).location );
							moveStatus = false;
						}
					}
				}
			}
			
			
			
			// Movement
			if ( mySwarmID == 1) {
				// If there is a distress call, prioritize that.
				DistressSignal tempDistressCall = new DistressSignal(rc.readBroadcast(BC_DISTRESS_PENDING));
				rc.broadcast(BC_DISTRESS_PENDING, 0);
				// If it's null, accept it as the current;
				if ( currentDistressCall.age+distressTurnLife < Clock.getRoundNum() ) currentDistressCall = new DistressSignal(0);
				if ( tempDistressCall.priority >= currentDistressCall.priority ) { currentDistressCall = tempDistressCall; }				
				if ( moveStatus && currentDistressCall.priority > 0 ) {
					if ( debugLevel >= 1 ) {
						rc.setIndicatorString(2, "Responding to Distress Call near"+intToLoc(currentDistressCall.loc)+", Priority "+currentDistressCall.priority);
						System.out.println("Responding to Distress Call near "+intToLoc(currentDistressCall.loc)+", Priority "+currentDistressCall.priority);
					}
					Robot target = getClosestEnemyTarget();
					if ( target != null && rc.readBroadcast(BC_SWARM_FORM_COUNT) < EnemyRobots.length+2 ) {
						followPath(intToLoc(currentDistressCall.loc),rc.senseRobotInfo(target).location);						
					}
					else followPath(intToLoc(currentDistressCall.loc));
					
					moveStatus = false;
				}
				

				// If there is an enemy nearby, prioritize that next.
				if ( moveStatus ) {
					Robot target = getBestEnemyTarget();
					if ( target != null ) {
						if ( rc.readBroadcast(BC_SWARM_FORM_COUNT) < EnemyRobots.length+2 ) {
							if ( debugLevel >= 1 ) rc.setIndicatorString(2, "Avoiding nearby Enemy");
							flee = true;
							//dir = followPath(myLoc.add(myLoc.directionTo(rc.senseRobotInfo(target).location).opposite()));
							dir = followPath(rc.senseRobotInfo(target).location,rc.senseRobotInfo(target).location);
							moveStatus = false;
						}
						else {
							if ( debugLevel >= 1 ) rc.setIndicatorString(2, "Moving to nearby Enemy at "+rc.senseRobotInfo(target).location);
							dir = followPath(rc.senseRobotInfo(target).location);
							moveStatus = false;
							rc.broadcast(BC_ATTACK_FLAG, 1);
						}
					}
				}


				// If there is an enemy Pastr, prioritize that next.
				if ( moveStatus && rc.readBroadcast(BC_SWARM_FORM_COUNT) >= EnemyRobots.length+2) {
					if ( turnLastScannedPastrs+10 < Clock.getRoundNum() ) {
						turnLastScannedPastrs = Clock.getRoundNum();
						EnemyPastrs = rc.sensePastrLocations(rc.getTeam().opponent());
						pastrTarget = null;
					}
					if ( pastrTarget == null ) {
						double pastrDist = 99999.9;
						for ( MapLocation n : EnemyPastrs ) {
							if ( n.distanceSquaredTo(myLoc) < pastrDist && n.distanceSquaredTo(rc.senseEnemyHQLocation()) > 9 ) {
								pastrTarget = n;
								pastrDist = n.distanceSquaredTo(myLoc);
							}
						}
					}
					if ( pastrTarget != null ) {
						if ( debugLevel >= 1 ) rc.setIndicatorString(2, "Moving towards a Pastr ");
						dir = followPath(pastrTarget);
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
						if ( rc.readBroadcast(BC_ALT_USE) == 1 ) {
							if ( debugLevel >= 1 ) System.out.println("USING ALT RALLY");
							rallyPoint = intToLoc(rc.readBroadcast(BC_ALT_RALLY));
						}
						if ( myLoc.equals(rallyPoint) ) {
							if ( debugLevel >= 1 ) rc.setIndicatorString(2, "Waiting at Rally Point");									
						}
						else {
							if ( debugLevel >= 1 ) rc.setIndicatorString(2, "Heading to Rally Point");									
							dir = followPath(rallyPoint);
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
					followPath(formLoc);
				}
				else {
					// Avoid the enemy, not ready to attack.
					
					Robot target = getClosestEnemyTarget();
					
					if ( target != null && rc.readBroadcast(BC_ATTACK_FLAG) == 0 ) {
						if ( debugLevel >= 1 ) {
							System.out.println("Flee!!");
							rc.setIndicatorString(0, "Flee!!!");
						}
						if ( !myLoc.equals(formLoc) ) {
							followPath(formLoc,rc.senseRobotInfo(target).location);
						}
					}
					else {
						if ( !myLoc.equals(formLoc) ) {
							followPath(formLoc);
						}
					}
				}
				myLoc = rc.getLocation();
			}

			
			// Update my status indicators
			if ( debugLevel >= 1 ) rc.setIndicatorString(1, 
					"MyOrders: "+myOrders
					+"     Formation ID: "+mySwarmID
					+"     PathChannel: "+myPathChannel
					+"     Path Status: "+pathStatus
					+"     Attack Status: "+rc.readBroadcast(BC_ATTACK_FLAG)
					+"     SwarmSize: "+rc.readBroadcast(BC_SWARM_FORM_COUNT));
			if ( mySwarmID == 1 ) {
				if ( dir != null ) rc.broadcast(BC_SWARM_LEAD_LOC, locToInt(myLoc.add(dir)));
				
				if ( flee ) {
					if ( rc.readBroadcast(BC_ORDERS_FLEE) == 0 ) rc.broadcast(BC_ORDERS_FLEE, 1);
				}
				else if ( rc.readBroadcast(BC_ORDERS_FLEE) == 1 ) rc.broadcast(BC_ORDERS_FLEE, 0);
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

	int lastTurnRequested = -999;
	private boolean requestNewPath(int pathChan, int target) throws GameActionException {
		// Check to see if the points are valid 
		if ( myLoc.equals(intToLoc(rc.readBroadcast(pathChan+1))) && intToLoc(target).equals(intToLoc(rc.readBroadcast(pathChan+2))) ) {
			return false;
		}

		// Check to see if this path has already been requested 
		if ( pathStatus == 2 || pathStatus == 3 ) {
			return true;
		}
		
		//if ( Clock.getRoundNum() < lastTurnRequested+25 ) return false;
		//lastTurnRequested = Clock.getRoundNum();
		
		if ( debugLevel >= 1 ) System.out.println(">>> Requesting New Path for channel "+pathChan);
		rc.broadcast(pathChan+1, locToInt(myLoc));
		rc.broadcast(pathChan+2, target);
		rc.broadcast(pathChan, 2);
		pathStatus = 2;
		myPathPosition = 3;			
		return true;				
	}

	// Pathing variables
	int nonProgressTurnCount = 5;
	double progressDistance = 99999.9;
	MapLocation progressTarget;
	private Direction followPath(MapLocation in) throws GameActionException {
		return followPath(in,null);
	}
	private Direction followPath(MapLocation in,MapLocation avoiding) throws GameActionException {
		Direction dir = null;
		MapLocation target = in;
		int targInt = locToInt(target);
		int myPathChannel = this.myPathChannel;
		if ( mySwarmID == 1 ) myPathChannel = 200;

		// The leader will request a new path every time it changes.  
		// Other bots will follow their path until the end, and then request a bot.  
		// This is because on complex maps, the formation bots flood the HQ with requests exponentially 
		//    faster then it can handle them, as their formation locations change each turn.
		if ( mySwarmID == 1 ) {
			if ( !in.equals(intToLoc(rc.readBroadcast(myPathChannel+2))) ) {
				// My goal has changed.
				if ( debugLevel >= 2 ) System.out.println("Requesting New Path because my Goal has changed");
				requestNewPath(myPathChannel,targInt);			
			}
		}
		
		switch (pathStatus) {
		case 0: // I have no path
			// If i have no path, request one.
			if ( debugLevel >= 2 ) System.out.println("Requesting New Path because i dont have one");
			requestNewPath(myPathChannel,targInt);
			break;
			
		case 1: // I have a path			
			MapLocation myTarget = intToLoc(rc.readBroadcast(myPathChannel+myPathPosition)); 
			if ( myLoc.equals(myTarget) ) {
				myPathPosition++;
				myTarget = intToLoc(rc.readBroadcast(myPathChannel+myPathPosition));		
			}
			if ( rc.readBroadcast(myPathChannel+myPathPosition) == -1 ) { 
				myTarget = target;
				if ( debugLevel >= 2 ) System.out.println("Requesting New Path because i have reached the end of my current one");
				requestNewPath(myPathChannel,targInt);
			}
			if ( debugLevel >= 1 ) rc.setIndicatorString(0, "Moving towards -> "+myTarget.toString());
			if ( avoiding == null ) dir = takeStepTowards(myTarget);
			else dir = takeStepTowardsWhileAvoiding(myTarget,avoiding);
			if ( progressTarget != null && progressTarget.equals(myTarget) ) {
				if ( nonProgressTurnCount == 0 ) {
					if ( debugLevel >= 2 ) System.out.println("Requesting New Path because i am stuck");
					requestNewPath(myPathChannel,targInt);
				}
				else if ( myLoc.distanceSquaredTo(progressTarget) < progressDistance ) {
					progressDistance = myLoc.distanceSquaredTo(progressTarget);
					nonProgressTurnCount = 5;
				}
				else {
					nonProgressTurnCount--;
				}
			}
			else {
				progressTarget = myTarget;
				nonProgressTurnCount = 5;
				progressDistance = 99999.9;
			}
			

			if ( dir == null ) {
				// For some reason, my path current path didnt work.  
				// Check to see if the path is valid (start and end are correct, but i still cant go)
				// If so, there's no reason to request a new path, there's probably a bot in the way, just wait it out.
				
				// Check to see if there is a bot in the way
				GameObject gob;
				if ( rc.canSenseSquare(myTarget) ) {
					gob = rc.senseObjectAtLocation(myTarget);
					if ( gob != null ) {
						if ( mySwarmID == 200 ) {
							if ( debugLevel >= 2 ) System.out.println("Requesting New Path because i cant move.");
							if ( !requestNewPath(200,locToInt(target)) ) {
								// If the path is valid, but i still cant move, i am quite possibly boxed in.  Signal the troops to scatter.
								if ( debugLevel >= 1 ) rc.setIndicatorString(0, "Scattering the troops...");
								rc.broadcast(BC_SCATTER_FLAG, 1);
								return null;
							}
						}
						else {
							if ( debugLevel >= 1 ) System.out.println("[<>] Tried to move to "+myTarget+" but there was a bot in the way.  Skipping to the next point.");
							if ( rc.readBroadcast(myPathChannel+myPathPosition) != -1) {
								myPathPosition++;
								return followPath(target,avoiding);
							}
						}
					}
				}
			}
			break;
			
		case 2: // I have requested a new path
			// In the mean time, wait.
			//if ( myLoc.distanceSquaredTo(target) < 16 ) dir = takeStepTowards(target);
			if ( avoiding == null ) dir = takeStepTowards(target);
			else dir = takeStepTowardsWhileAvoiding(target,avoiding);
			if ( dir != null ) rc.broadcast(myPathChannel+1, locToInt(myLoc.add(dir)));			
			break;
		case 3: // A new path is incoming
			// Do nothing, wait for the path.
		}		
		
		myLoc = rc.getLocation();
		return dir;
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
	
	private Robot getClosestEnemyTarget() throws GameActionException {
		if ( turnLastScannedEnemys < (turnLastScannedEnemys = Clock.getRoundNum()) ) {
			EnemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
		}
		if ( EnemyRobots.length == 0 ) return null;
		Robot bestTarget = null;
		RobotInfo btInf;
		double range = 999.0;
		// Process the enemies
		for ( Robot n : EnemyRobots ) {
			btInf = rc.senseRobotInfo(n);
			switch (btInf.type) {
			case HQ:
			case NOISETOWER:
			case PASTR:
				break;
				
			case SOLDIER:
				if ( btInf.location.distanceSquaredTo(myLoc) < range ) {
					range = btInf.location.distanceSquaredTo(myLoc);
					bestTarget = n;
				}
				break;
				
			default:
				break;
			}
		}
		
		return bestTarget;
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
			//if ( rc.senseRobotInfo(n).location.distanceSquaredTo(eHQ) < 25.0 ) { 
			//	if ( debugLevel >= 2 ) System.out.println("    Ignoring it, because it is too close to the HQ.");
			//	continue; 
			//} // Ignore HQ's
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
		
		if ( debugLevel >= 1 ) p.printPath();
		
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
		
		if ( debugLevel >= 1 ) if ( closestPoint != null ) System.out.println("Calculated Rally Point:  "+closestPoint.toString());
		
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
	// Priorities:
	//   Swarm Bot:		10 // Maybe
	//   Pastr Bot:		20
	//   Manic Bot:		20
	//   Tower Bot:		20
	//   Noise Tower:	50
	//   Pastr:			80
	public void sendDistressCall(int basePriority, double distanceFromEnemy, MapLocation distressLoc) throws GameActionException {
		
		// Start with the base priority based on the incoming distress type (Pastrs are worth more than bots, etc.)
		double calcPriority = basePriority;
		
		// Next, factor in the distance to the enemy
		if ( distanceFromEnemy < 24.0 ) calcPriority *= 1.5;
		
		
		// Next, factor in the distance from the swarm.  If they're close, this distress call can be handled quickly, so divert to handle it.
		if ( distressLoc.distanceSquaredTo(intToLoc(rc.readBroadcast(BC_SWARM_LEAD_LOC))) < (distressTurnLife*distressTurnLife/4) ) {
			calcPriority *= 1.2;
		}
		
		// Now, get the current Distress signal
		DistressSignal current = new DistressSignal(rc.readBroadcast(BC_DISTRESS_PENDING));
		
		// If the current Distress signal is lower priority than this one, override the current signal.
		if ( current.priority < calcPriority ) {
			//DistressSignal n = new DistressSignal((int)calcPriority, (int)distanceFromEnemy, locToInt(distressLoc));
			DistressSignal n = new DistressSignal(
					locToInt(distressLoc),
					(int)calcPriority, 
					(int)distanceFromEnemy 
					);
			if ( debugLevel >= 1 ) System.out.println("Updating Distress Signal at "+distressLoc+" with DFE"+distanceFromEnemy+" :  "+n.priority+"\t"+n.dfe+'\t'+n.loc);
			rc.broadcast(BC_DISTRESS_PENDING, n.toInt());
		}
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
	
	public class DistressSignal {
		
		int loc;
		int priority;
		int dfe;
		int age;
		
		public DistressSignal(int in) {
			loc = (int)(in/(100*200));
			priority = (int)((in-(100*200*loc))/100);
			dfe = (in-(100*priority)-(loc*200*100));
			age = Clock.getRoundNum();
		}
		
		public DistressSignal(int l, int p, int d) {
			loc = l;
			priority = p;
			dfe = d;
			age = Clock.getRoundNum();
		}
		
		public int toInt() {
			return (dfe+(priority*100)+(200*100*loc));
		}
	}

}
