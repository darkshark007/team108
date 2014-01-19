package team108.Strategies;

import java.util.LinkedList;

import team108.Graph.MapRender;
import team108.Path.____DirectWithOptimizedBuggingPathGenerator____OLD;
import team108.Path.ObstaclePointPathGenerator;
import team108.Path.Path;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class CornerScoopStrategy extends Strategy {

	public CornerScoopStrategy(RobotController in) { super(in); }
	
	final int BC_ORDERS_ID 			= 0;
	final int BC_ORDERS_PASTR_X		= 1;
	final int BC_ORDERS_PASTR_Y		= 2;
	final int BC_ORDERS_SCOOP_X		= 3;
	final int BC_ORDERS_SCOOP_Y		= 4;
	final int BC_ORDERS_PATH_CHAN	= 5;
	
	// Path channel variables
	int myPathChannel;
	int myPathStatus;
	int myPathPosition;
	

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
				int pathChan = 200;
				
				rc.broadcast(BC_ORDERS_PATH_CHAN, pathChan);				
				if ( rc.isActive() ) if ( rc.canMove(dir) ) rc.spawn(dir);

				
				// Graph Generator
				//ObstaclePointPathGenerator gr = new ObstaclePointPathGenerator(rc);
				____DirectWithOptimizedBuggingPathGenerator____OLD gr = new ____DirectWithOptimizedBuggingPathGenerator____OLD(rc);
				LinkedList<PointPair> pps = getCornerPointPairsForMapRender(gr.getMapRender());
				
				for ( PointPair n : pps ) {
					System.out.println("P1: "+n.p1.toString()+"\t\tP2: "+n.p2.toString());
				}
				
				// 1 = Pastr
				// 0 = Scoop
				int oType = 0;
				PointPair tempPair = null;
				tempPair = pps.removeFirst();
				LinkedList<MapLocation> pastrsBuilt = new LinkedList<MapLocation>();
				rc.broadcast(BC_ORDERS_PASTR_X, tempPair.p1.x);
				rc.broadcast(BC_ORDERS_PASTR_Y, tempPair.p1.y);
				rc.broadcast(BC_ORDERS_SCOOP_X, tempPair.p2.x);
				rc.broadcast(BC_ORDERS_SCOOP_Y, tempPair.p2.y);
				rc.broadcast(BC_ORDERS_ID, 1);
				pastrsBuilt.add(tempPair.p1);
				
				postPathToPathChannel(gr.getPath(myLoc.add(dir),tempPair.p1),pathChan);
				while ( true ) { 
					if ( rc.isActive() ) {
						// Spawn a soldier
						if ( rc.canMove(dir) ) {
							if ( oType == 1 ) {
								// Spawn a Pastr
								if ( !pps.isEmpty() ) {
									tempPair = pps.removeFirst();
								}
								else {
									rc.yield();
									continue;
								}
								
								if ( pastrsBuilt.contains(tempPair.p1) ) {
									oType = 1-oType;
								}
								else {
									pastrsBuilt.add(tempPair.p1);
									rc.broadcast(BC_ORDERS_ID, 1);
								}
								rc.broadcast(BC_ORDERS_PASTR_X, tempPair.p1.x);
								rc.broadcast(BC_ORDERS_PASTR_Y, tempPair.p1.y);
								rc.broadcast(BC_ORDERS_SCOOP_X, tempPair.p2.x);
								rc.broadcast(BC_ORDERS_SCOOP_Y, tempPair.p2.y);
							}
							pathChan += 100;												
							rc.broadcast(BC_ORDERS_PATH_CHAN, pathChan);
							if ( oType == 0 ) {
								rc.broadcast(BC_ORDERS_ID, 2);
							}
							rc.spawn(dir);
							if ( oType == 1 ) {
								postPathToPathChannel(gr.getPath(myLoc.add(dir),tempPair.p1),pathChan);
							}
							else if ( oType == 0 ) {
								postPathToPathChannel(gr.getPath(myLoc.add(dir),tempPair.p2),pathChan);
							}
							oType = 1-oType;
						}
					} 
					rc.yield();
				}
			
			case SOLDIER:
				int myOrders = rc.readBroadcast(BC_ORDERS_ID);
				myPathChannel = rc.readBroadcast(BC_ORDERS_PATH_CHAN);
				while ( myOrders == 0 ) {
					rc.yield();
					myOrders = rc.readBroadcast(BC_ORDERS_ID);
				}
				MapLocation pastrTarget = new MapLocation(rc.readBroadcast(BC_ORDERS_PASTR_X),rc.readBroadcast(BC_ORDERS_PASTR_Y));
				MapLocation scoopTarget = new MapLocation(rc.readBroadcast(BC_ORDERS_SCOOP_X),rc.readBroadcast(BC_ORDERS_SCOOP_Y));
				myPathPosition = 1;
				System.out.println(">> My Orders: "+myOrders+"\t\tMyPathChannel:  "+myPathChannel);
				
				rc.setIndicatorString(0, "My Orders:  "+myOrders+"     PastR: "+pastrTarget.toString()+"     Scoop: "+scoopTarget.toString());

				switch (myOrders) {
				case 1: // Pastr
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


				case 2: // Scoop
					int scoopState = 0;
					boolean startedLoop = false;
					pastrTarget = pastrTarget.add(pastrTarget.directionTo(scoopTarget));
					scoopTarget = scoopTarget.add(scoopTarget.directionTo(pastrTarget));
					MapLocation myTarget = scoopTarget;
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
										MapLocation t1 = pastrTarget.add(d1).add(d1).add(d1);

										Direction d2 = scoopTarget.directionTo(pastrTarget).rotateLeft().rotateLeft(); 
										MapLocation t2 = pastrTarget.add(d2).add(d2).add(d2);
										
										if ( locIsOnMap(t1) ) myTarget = t1;
										else myTarget = t2;
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
	
	
	private void postPathToPathChannel(Path p, int channel) throws GameActionException {
		int chan = channel+1;
		
		MapLocation next = p.nextLink();
		do {
			rc.broadcast(chan++, locToInt(next));
			next = p.nextLink();
		} while ( next != null );
		rc.broadcast(chan, -1);
		rc.broadcast(channel,1);
		System.out.println(">> Path Channel "+channel+" Posted.");
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
