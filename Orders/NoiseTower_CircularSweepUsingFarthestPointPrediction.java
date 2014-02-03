package team108.Orders;

import java.util.LinkedList;
import java.util.PriorityQueue;

import team108.Strategies.SwarmAndScoopStrategy.PointPair;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class NoiseTower_CircularSweepUsingFarthestPointPrediction extends NoiseTower {

	MapLocation[] Perimeter;
	
	// Consider removing  the next outer square as well (Perhaps leaving the corners)
	// Or maybe only pulse once.
	public NoiseTower_CircularSweepUsingFarthestPointPrediction(RobotController in) { 
		super(in); 
	
		System.out.println("Calculating Perimeter...");
		Perimeter = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), 300);
		
		for ( MapLocation n : Perimeter ) {
			System.out.println(n.toString());
		}
		
	}
	
	MapLocation targetLoc = null;
	ValidCowTarget cowTarg;
	//boolean[] validTargets = null;
	PriorityQueue<ValidCowTarget> validTargets = null;
	PriorityQueue<ValidCowTarget> validTargets2 = null;
	int shotNum;
	int targetIndex;

	@Override
	public void executeOrders() throws GameActionException {
		int dX = 0, dY = 0;
		
		// If the validTargets array is null, then we're starting over from scratch.
		if ( validTargets == null ) {
			validTargets = new PriorityQueue<ValidCowTarget>();
			validTargets2 = new PriorityQueue<ValidCowTarget>();
			for ( int i = 0; i < Perimeter.length; i++ ) if ( locIsOnMap(Perimeter[i]) ) validTargets.add(new ValidCowTarget(Perimeter[i],myLoc.distanceSquaredTo(Perimeter[i])));
			shotNum = 1;
		}
		
		
		shotNum = 1-shotNum;
		
		if ( shotNum == 1 ) {
			if ( debugLevel >= 2 ) System.out.println("[OO]  M: "+myLoc.toString()+"\tT: "+targetLoc.toString()+"\tD: "+myLoc.distanceSquaredTo(targetLoc));
			rc.attackSquareLight(targetLoc);
			
			// Find the locations that must be updated.
			LinkedList<MapLocation> tempList = getAllAdjacentPoints(targetLoc); 
			
			for ( MapLocation n : tempList ) {
				if ( myLoc.distanceSquaredTo(n) <= cowTarg.dist ) validTargets.remove(new ValidCowTarget(n,0));
			}
			
			// Return
			return;
		}
		
		// Find the farthest valid target
		cowTarg = validTargets.remove();
		targetLoc = cowTarg.target;
		



		
		//targetLoc = new MapLocation(dX,dY);
		//targetLoc = myLoc;
		if ( debugLevel >= 2 ) System.out.println("[OO]  M: "+myLoc.toString()+"\tT: "+targetLoc.toString()+"\tD: "+myLoc.distanceSquaredTo(targetLoc));
		rc.attackSquare(targetLoc);		
	}
	
	private class ValidCowTarget implements Comparable<ValidCowTarget> {
		
		MapLocation target;
		double dist;
		
		public ValidCowTarget(MapLocation in, double distIn) {
			target = in;
			dist = distIn;
		}

		@Override
		public int compareTo(ValidCowTarget test) {
			if ( test.dist > dist ) return 1;
			if ( test.dist < dist ) return -1;
			return 0;
		}

		@Override
		public boolean equals(Object e) {
			if ( e == null ) return false;
			else if ( !(e instanceof ValidCowTarget) ) return false;
			else {
				ValidCowTarget p = (ValidCowTarget)e;
				return p.target.equals(target);
			}
		}

		
	}
}
