package team108.Strategies;

import java.util.LinkedList;
import java.util.PriorityQueue;

import team108.Graph.MapRender;
import team108.Graph.SimpleLinkedList;
import team108.Orders.I_Orders;
import team108.Orders.NoiseTower;
import team108.Orders.NoiseTower_CircularSweepUsingRadiusAndAngle;
import team108.Orders.NoiseTower_DepthFirstPathGenerator;
import team108.Path.DepthFirstPathing;
import team108.Path.Path;
import team108.Path.PathGenerator;
import team108.Strategies.HardFormationSwarmStrategy.CowLocation;
import battlecode.common.RobotInfo;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.TerrainTile;

public class PassiveHerdingStrategy extends Strategy {
	
	final static int BC_ORDERS_PATH_CHAN			= 0;
	final static int BC_ORDERS_ID					= 1;
	final static int BC_ORDERS_READY				= 2;
	final static int BC_ORDERS_PASTR_TARGET			= 3;
	final static int BC_ORDERS_NOISE_TARGET			= 4;
	final static int BC_ORDERS_MANIC_TARGET			= 5;
	final static int BC_STATUS_SQUAD_ID				= 6;
	// Squad Channels:
	// BC_STATUS_LIST + SQUAD_ID + 0 :   NOISE_TARGET   
	// BC_STATUS_LIST + SQUAD_ID + 1 :   PASTR_TARGET 
	// BC_STATUS_LIST + SQUAD_ID + 2 :   Noise_Tower is alive
	// BC_STATUS_LIST + SQUAD_ID + 3 :   Pastr is alive
	// BC_STATUS_LIST + SQUAD_ID + 4 :   Manic_Bot is alive
	// BC_STATUS_LIST + SQUAD_ID + 5 :   Turns Till Sweep Convergence
	final static int BC_STATUS_LIST					= 50;
	
	
	int myOrders;
	MapLocation myLoc;
	MapLocation myPastrTarget;
	Direction dir;
	int myPathChannel;
	int myPathPosition;
	int HQSquadID = 0;
	int mySquadID;
	int pathStatus;
	LinkedList<CowLocation> cowBlackList;
	CowLocation nextLoc = null;
	
	// Enemy scan stuff
	int turnLastScannedEnemys = -999;
	int turnLastScannedPastrs = -999;
	Robot[] EnemyRobots;
	MapLocation[] EnemyPastrs;



	public PassiveHerdingStrategy(RobotController in) { super(in); }

	public void run() {
		try {
			switch (rc.getType()) {
			case HQ:
				// Find Spawn direction
				// Check Straight
				myLoc = rc.getLocation(); 
				dir = myLoc.directionTo(rc.senseEnemyHQLocation()).opposite();
				// Rotate until i hit a valid spawn point
				while ( !rc.canMove(dir) ) dir = dir.rotateLeft();
				
				
				// Initialize some variables
				int pathChan = 300;
				rc.broadcast(BC_ORDERS_PATH_CHAN, pathChan);
				rc.broadcast(BC_ORDERS_READY, 0);
				rc.broadcast(BC_STATUS_SQUAD_ID, HQSquadID);
				HQSquadID += 6;
				cowBlackList = new SimpleLinkedList<CowLocation>();
				
				pathChan += 100;

				
				
				// Spawn the first Bot
				rc.broadcast(BC_ORDERS_ID, 1);
				rc.spawn(dir);
				
				
				
				// Initialize the Path Generators
				MapRender mren = new MapRender(rc);
				mren.setFlag_CollectVoids(true);
				mren.init();
				mren.voidPad(1);
				mren.blacklistEnemyBasePerimeter();
				PathGenerator gr = new DepthFirstPathing(rc,mren);
				gr.getMapRender().printMap();

				
				boolean continueFlag;
				while ( true ) { 

					// Update some variables
					continueFlag = false;
					if ( rc.readBroadcast(BC_ORDERS_READY) == 1 ) rc.broadcast(BC_ORDERS_READY, 0);

					
					// Spawn a soldier
					if ( rc.isActive() ) {
						if ( rc.senseRobotCount()<GameConstants.MAX_ROBOTS ) {
							if ( rc.canMove(dir) ) rc.spawn(dir);
							else {
								Direction tempDir = dir;
								do {
									tempDir = tempDir.rotateRight();
								} while ( !rc.canMove(tempDir) );
								rc.spawn(tempDir);
							}
						}

						// Update spawn variables
						rc.broadcast(BC_ORDERS_PATH_CHAN, pathChan);
						pathChan += 100;

						if ( rc.readBroadcast(BC_ORDERS_ID) == 3 ) { 
							rc.broadcast(BC_ORDERS_ID, 1); 
							rc.broadcast(BC_STATUS_SQUAD_ID, HQSquadID);
							HQSquadID += 6;
						}
						else if ( rc.readBroadcast(BC_ORDERS_ID) == 1 ) { rc.broadcast(BC_ORDERS_ID, 2); }
						else if ( rc.readBroadcast(BC_ORDERS_ID) == 2 ) { 
							rc.broadcast(BC_ORDERS_ID, 3);
							
							setUpNewPastr(mren);

							
							
						}
					} 
					
					
					// Update the paths, Starting with the Leader
					int rNum = Clock.getRoundNum();
					for ( int i = 300; i < pathChan; i += 100 ) {
						if ( debugLevel >= 3 ) System.out.println("Checking "+i+"\t:  "+rc.readBroadcast(i));
						if ( rc.readBroadcast(i) == 2 ) {
							MapLocation from = intToLoc(rc.readBroadcast(i+1));
							MapLocation to = intToLoc(rc.readBroadcast(i+2));
							if ( debugLevel >= 1 ) System.out.println("Calculating Path from "+from.toString()+" -> "+to.toString()+" for channel "+i);							
							rc.broadcast(i, 3);
							
							postPathToPathChannel(gr.getPath(from,to),i);
							
							if ( rNum < Clock.getRoundNum() ) continueFlag = true;
							if ( continueFlag ) break;
						}
					}

					
					// DATA POST-PROCESSING					
					if ( processingCowLocations ) {
						// Process cow spawn locations, looking for good spots to build a Noise Tower
						continueFlag = true;
						processCowGrowthLocations(mren);
					}
					
					if ( continueFlag ) continue;
					rc.yield();
				}
			
			case SOLDIER:
				myOrders = rc.readBroadcast(BC_ORDERS_ID);
				myPathChannel = rc.readBroadcast(BC_ORDERS_PATH_CHAN);
				mySquadID = rc.readBroadcast(BC_STATUS_SQUAD_ID);
				switch (myOrders) {
				case 1:
					runSoldier_NoiseTower();
				case 2:
					runSoldier_Pastr();
				case 3:
					runSoldier_HomicidalManiacBot();
				}
			case NOISETOWER:
				NoiseTower ord = new NoiseTower_CircularSweepUsingRadiusAndAngle(rc);
				NoiseTower_DepthFirstPathGenerator ord2 = new NoiseTower_DepthFirstPathGenerator(rc);
				mySquadID = rc.readBroadcast(locToInt(rc.getLocation())+4000);

				while (true) {

					rc.broadcast(BC_STATUS_LIST+mySquadID+2, 15);

					System.out.println("AD:  "+rc.getActionDelay());
					if ( rc.isActive() ) ord.executeOrders();
					
					rc.broadcast(BC_STATUS_LIST+mySquadID+5, ord.getTurnsTillConvergence());

					
					// Handle the data processing.
					int sTurn = Clock.getRoundNum();
					System.out.println("Density:  "+ord2.getPathDensity());
					if ( ord2.getPathDensity() < 0.5 ) {
						ord = ord2;
					}
					ord2.processData(); 
					if ( sTurn == Clock.getRoundNum() ) rc.yield();
				}

			case PASTR:
				mySquadID = rc.readBroadcast(locToInt(rc.getLocation())+4000);
				while ( true ) {

					rc.broadcast(BC_STATUS_LIST+mySquadID+3, 45);

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
	
	private void setUpNewPastr(MapRender mren) throws GameActionException {
		// Time to give the squad its orders
		System.out.println("Size:  "+cowLocs.size());
		
		// First, make a list of NoiseTower areas to blacklist
		MapLocation[] blackLocs = new MapLocation[25];
		int blSize = 0;
		boolean mergeFlag = false;
		
		for ( int i = 0; i < HQSquadID; i += 6 ) {
			// If this squad's noise tower and either the Pastr or the Manic Bot are still alive, enforce the black list.
			if ( rc.readBroadcast(BC_STATUS_LIST+i+2) > 0 && ( rc.readBroadcast(BC_STATUS_LIST+i+3) > 0 || rc.readBroadcast(BC_STATUS_LIST+i+4) > 0) ) {
				blackLocs[blSize] = intToLoc(rc.readBroadcast(BC_STATUS_LIST+i+0));
				System.out.println("Blacklisted "+blackLocs[blSize]);
				blSize++;
			}
			// Else, this area is free to inhabit.
			// Make sure the previously blacklisted areas (including the location of that NT) are re-added to the queue.
			else mergeFlag = true;
		}
		
		// If the merge flag is true, merge the blacklisted queue into the main queue.
		if ( mergeFlag ) {
			for ( CowLocation n : cowBlackList ) cowLocs.add(n);
			cowBlackList = new SimpleLinkedList<CowLocation>();
			mergeFlag = false;
		}
		
		
		
		// Next, select a location that doesnt interfere with any of the current locations
		boolean badLocation;
		do {
			badLocation = false;
			nextLoc = cowLocs.remove();
			System.out.println("Trying "+nextLoc.loc+"...");
			cowBlackList.add(nextLoc);
			
			for ( int i = 0; i < blSize; i++) {
				System.out.println("Distance to "+blackLocs[i]+"  == "+nextLoc.loc.distanceSquaredTo(blackLocs[i]));
				if ( nextLoc.loc.distanceSquaredTo(blackLocs[i]) < 400 ) {
					badLocation = true;
				}
			}
		} while ( !cowLocs.isEmpty() && badLocation );
	
		rc.broadcast(BC_ORDERS_NOISE_TARGET, locToInt(nextLoc.loc));
		
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
		
		rc.broadcast(BC_ORDERS_READY,1);
	}
	
	private void runSoldier_NoiseTower() throws GameActionException {
		// Take two steps away from the HQ.
		while ( !rc.isActive() ) rc.yield();
		myLoc = rc.getLocation();
		//dir = myLoc.directionTo(rc.senseHQLocation()).opposite(); // Opposite
		dir = myLoc.directionTo(rc.senseHQLocation());
		takeStepTowards(myLoc.add(dir));
		while ( !rc.isActive() ) rc.yield();
		myLoc = rc.getLocation();
		//dir = myLoc.directionTo(rc.senseHQLocation()).opposite(); // Opposite
		dir = myLoc.directionTo(rc.senseHQLocation());
		takeStepTowards(myLoc.add(dir));
		
		// Now, wait for orders.
		while ( rc.readBroadcast(BC_ORDERS_READY) == 0 ) { rc.yield(); }
		myPastrTarget = intToLoc(rc.readBroadcast(BC_ORDERS_NOISE_TARGET));
		rc.broadcast(BC_STATUS_LIST+mySquadID+0, rc.readBroadcast(BC_ORDERS_NOISE_TARGET));
		rc.broadcast(locToInt(myPastrTarget)+4000,mySquadID);
		rc.setIndicatorString(1, "My Orders: "+myOrders+"    My Squad ID: "+mySquadID+"     My Path Channel: "+myPathChannel+"     Pastr: "+intToLoc(rc.readBroadcast(BC_ORDERS_PASTR_TARGET))+"     NT: "+intToLoc(rc.readBroadcast(BC_ORDERS_NOISE_TARGET))+"     MT: "+intToLoc(rc.readBroadcast(BC_ORDERS_MANIC_TARGET)));

		
		while ( true ) {
			
			// Update my variables
			myLoc = rc.getLocation();
			pathStatus = rc.readBroadcast(myPathChannel);
			rc.broadcast(BC_STATUS_LIST+mySquadID+2, 15);

			
			// Check for Enemies
			Robot target = getClosestEnemyTarget();
			RobotInfo tInf = null;
			if ( target != null ) tInf = rc.senseRobotInfo(target);

			
			
			// If an enemy is detected, move to avoid it.
			if ( rc.isActive() && target != null ) {
				MapLocation awayTarget = myLoc.add(tInf.location.directionTo(myLoc));
				takeStepTowards(awayTarget);				
				System.out.println("Enemy Detected at "+tInf.location+"!  Moving away, towards "+awayTarget);
			}

			
			
			// Movement
			if ( rc.isActive() ) {
				if ( myLoc.equals(myPastrTarget) ) rc.construct(RobotType.NOISETOWER);
				else followPath();
			}
			rc.yield();
		}
	}
	
	private void runSoldier_Pastr() throws GameActionException {
		// Take one step away from the HQ.
		while ( !rc.isActive() ) rc.yield();
		myLoc = rc.getLocation();
		//dir = myLoc.directionTo(rc.senseHQLocation()).opposite(); // Opposite
		dir = myLoc.directionTo(rc.senseHQLocation());
		takeStepTowards(myLoc.add(dir));
		
		// Now, wait for orders.
		while ( rc.readBroadcast(BC_ORDERS_READY) == 0 ) { rc.yield(); }
		myPastrTarget = intToLoc(rc.readBroadcast(BC_ORDERS_PASTR_TARGET));
		rc.broadcast(BC_STATUS_LIST+mySquadID+1, rc.readBroadcast(BC_ORDERS_PASTR_TARGET));
		rc.broadcast(BC_STATUS_LIST+mySquadID+5, 90);
		rc.broadcast(locToInt(myPastrTarget)+4000,mySquadID);
		rc.setIndicatorString(1, "My Orders: "+myOrders+"    My Squad ID: "+mySquadID+"     My Path Channel: "+myPathChannel+"     Pastr: "+intToLoc(rc.readBroadcast(BC_ORDERS_PASTR_TARGET))+"     NT: "+intToLoc(rc.readBroadcast(BC_ORDERS_NOISE_TARGET))+"     MT: "+intToLoc(rc.readBroadcast(BC_ORDERS_MANIC_TARGET)));
		
		
		while ( true ) {
			
			// Update my variables
			myLoc = rc.getLocation();
			pathStatus = rc.readBroadcast(myPathChannel);
			rc.broadcast(BC_STATUS_LIST+mySquadID+3, 15);

			
			// Check for Enemies
			Robot target = getClosestEnemyTarget();
			RobotInfo tInf = null;
			if ( target != null ) tInf = rc.senseRobotInfo(target);

			
			
			// If an enemy is detected, move to avoid it.
			if ( rc.isActive() && target != null ) {
				MapLocation awayTarget = myLoc.add(tInf.location.directionTo(myLoc));
				takeStepTowards(awayTarget);
				System.out.println("Enemy Detected at "+tInf.location+"!  Moving away, towards "+awayTarget);
			}

			
			
			// Movement
			if ( rc.isActive() ) {
				if ( myLoc.equals(myPastrTarget) ) {
					if ( rc.readBroadcast(BC_STATUS_LIST+mySquadID+5) < 50 ) rc.construct(RobotType.PASTR);
				}
				else followPath();
			}
			rc.yield();
		}
	}
	
	private void runSoldier_HomicidalManiacBot() throws GameActionException {
		
		// Now, wait for orders.
		while ( rc.readBroadcast(BC_ORDERS_READY) == 0 ) { rc.yield(); }
		myPastrTarget = intToLoc(rc.readBroadcast(BC_ORDERS_MANIC_TARGET));
		MapLocation myPastr = intToLoc(rc.readBroadcast(BC_ORDERS_PASTR_TARGET));
		rc.setIndicatorString(1, "My Orders: "+myOrders+"    My Squad ID: "+mySquadID+"     My Path Channel: "+myPathChannel+"     Pastr: "+intToLoc(rc.readBroadcast(BC_ORDERS_PASTR_TARGET))+"     NT: "+intToLoc(rc.readBroadcast(BC_ORDERS_NOISE_TARGET))+"     MT: "+intToLoc(rc.readBroadcast(BC_ORDERS_MANIC_TARGET)));
		boolean inPosition = false;
		boolean pastrIsDead = false;
		Robot myPastrRobot = null;
		RobotInfo rif;
		
		while ( true ) {

			// Update my variables
			myLoc = rc.getLocation();
			pathStatus = rc.readBroadcast(myPathChannel);
			rc.broadcast(BC_STATUS_LIST+mySquadID+4, 5);

			
			// Check for Enemies
			Robot target = getClosestEnemyTarget();
			RobotInfo tInf = null;
			if ( target != null ) tInf = rc.senseRobotInfo(target);

			
			
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
						}
					}
				}
			}

			
			
			// If an enemy is detected, move to avoid it.
			if ( rc.isActive() && target != null && !inPosition ) {
				MapLocation awayTarget = myLoc.add(tInf.location.directionTo(myLoc));
				takeStepTowards(awayTarget);				
				System.out.println("Enemy Detected at "+tInf.location+"!  Moving away, towards "+awayTarget);
			}

			
			
			// Movement to get in position
			if ( rc.isActive() && !inPosition ) {
				if ( myLoc.equals(myPastrTarget) ) { inPosition = true; }
				else followPath();
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

	// This method will process using class variables, in order to allow it to return and resume between rounds.
	int pcgX = 0;
	int pcgY = 0;
	boolean processingCowLocations = true;
	double[][] cowSpawnMatrix = null;
	PriorityQueue<CowLocation> cowLocs = new PriorityQueue<CowLocation>();
	
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
	
	
	// Pathing Variables
	int nonProgressTurnCount = 10;
	double progressDistance = 99999.9;
	MapLocation progressTarget;
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

	private Direction followPath() throws GameActionException {
		Direction dir = null;
		MapLocation target = myPastrTarget;
		int targInt = locToInt(myPastrTarget);
		
		switch (pathStatus) {
		case 0: // I have no path
			// If i have no path, request one.
			requestNewPath(myPathChannel,targInt);
			break;
			
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
			}
			break;
			
		case 2: // I have requested a new path
		case 3: // A new path is incoming
			// In the mean time, wait.
			break;
		}		
		
		myLoc = rc.getLocation();
		return dir;
	}

	
	final static int cowGrowthRange = 4;
	private void processCowGrowthLocations(MapRender in) {
		if ( cowSpawnMatrix == null ) cowSpawnMatrix = rc.senseCowGrowth();
		int i = pcgX;
		int j = pcgY;
		int k;
		int startTurn = Clock.getRoundNum();
		CowLocation cowLoc;
		for ( ; i < width; i++ ) {
			for ( ; j < height; j++ ) {
				
				// If this location is a void, a-void (Haha) it.
				if ( in.terrainMatrix[i][j] == 99 ) { continue; }
				
				// If there is no cow growth at this location, skip it.  
				// There may still be growth nearby, but we can do better, and it's not worth the bytecodes.
				if ( cowSpawnMatrix[i][j] < 0.00001 ) { continue; }
				
				// Cross-sect left 
				for ( k = 1; k < cowGrowthRange; k++ ) {
					// If we reach the edge of the map, break;
					if ( i-k < 0 ) break;
					// If we've reached a void, break.  It will be difficult to farm that direction.
					if ( in.terrainMatrix[i-k][j] == 99 ) break;
					// cowSpawnMatrix[i][j] += in.cowSpawnRate[i-k][j]*(50/(50+k)); // Weighted for distance from center.
					cowSpawnMatrix[i][j] += in.cowSpawnRate[i-k][j];
				}
				
				// Cross-sect right
				for ( k = 1; k < cowGrowthRange; k++ ) {
					// If we reach the edge of the map, break;
					if ( i+k == width ) break;
					// If we've reached a void, break.  It will be difficult to farm that direction.
					if ( in.terrainMatrix[i+k][j] == 99 ) break;
					// cowSpawnMatrix[i][j] += in.cowSpawnRate[i+k][j]*(50/(50+k)); // Weighted for distance from center.
					cowSpawnMatrix[i][j] += in.cowSpawnRate[i+k][j];
				}

				// Cross-sect up
				for ( k = 1; k < cowGrowthRange; k++ ) {
					// If we reach the edge of the map, break;
					if ( j-k < 0 ) break;
					// If we've reached a void, break.  It will be difficult to farm that direction.
					if ( in.terrainMatrix[i][j-k] == 99 ) break;
					// cowSpawnMatrix[i][j] += in.cowSpawnRate[i][j-k]*(50/(50+k)); // Weighted for distance from center.
					cowSpawnMatrix[i][j] += in.cowSpawnRate[i][j-k];
				}

				// Cross-sect down
				for ( k = 1; k < cowGrowthRange; k++ ) {
					// If we reach the edge of the map, break;
					if ( j+k == height ) break;
					// If we've reached a void, break.  It will be difficult to farm that direction.
					if ( in.terrainMatrix[i][j+k] == 99 ) break;
					// cowSpawnMatrix[i][j] += in.cowSpawnRate[i][j+k]*(50/(50+k)); // Weighted for distance from center.
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
		System.out.println("Completed Cow Growth Processing");
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
