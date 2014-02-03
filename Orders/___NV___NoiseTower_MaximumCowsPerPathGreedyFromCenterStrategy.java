package team108.Orders;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;

import team108.Graph.SimpleLinkedList;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class ___NV___NoiseTower_MaximumCowsPerPathGreedyFromCenterStrategy extends NoiseTower {

	int arraySize = (1+MAX_RADIUS+MAX_RADIUS)*(1+MAX_RADIUS+MAX_RADIUS);
	int[] pathMap = new int[arraySize];
	LinkedList<MapLocation> perimeter = new SimpleLinkedList<MapLocation>();
	
	public ___NV___NoiseTower_MaximumCowsPerPathGreedyFromCenterStrategy(RobotController in) { 
		super(in);
		
		System.out.println("Building PathMap...");
		// Build the Path Map
		Queue<MapLocation> queue = new ArrayDeque<MapLocation>();
		boolean[] visited = new boolean[arraySize];
		queue.add(myLoc);
		visited[612] = true;
		
		
		// Modified Reverse Breadth First path generator-
		MapLocation current;
		//LinkedList<MapLocation> tempPoints;
		MapLocation[] tempPoints;
		int curIndex, index;
		TerrainTile t;
		while ( !queue.isEmpty() ) {
			// Process the current point
			current = queue.remove();
			perimeter.add(current);
			curIndex = ntLocToIndex(current);
			
			// Get all adjacent points
			//tempPoints = getAllAdjacentPoints(current);
			tempPoints = MapLocation.getAllMapLocationsWithinRadiusSq(current, 2);
			//System.out.println("TP  "+tempPoints.length);
			// Process the adjacent points.
			for ( MapLocation n : tempPoints ) {
				// Check to see if it is not on the map or a Void
				t = rc.senseTerrainTile(n);
				if ( t.equals(TerrainTile.OFF_MAP) ) continue;
				if ( t.equals(TerrainTile.VOID) ) continue;
				
				// Check my distance to the center
				if ( n.distanceSquaredTo(myLoc) > MAX_RANGE ) continue;
				
				
				// Check to see if it is visited
				index = ntLocToIndex(n);
				if ( visited[index] ) continue;
				
				visited[index] = true;
				pathMap[index] = curIndex;
				queue.add(n);
			}			
		}
		System.out.println("Finished PathMap...");

	}
	
	double[] cows = new double[arraySize];

	@Override
	public void executeOrders() throws GameActionException {
		MapLocation targetLoc;
		int dX = 0, dY = 0;
		
		
		System.out.println("Scanning...."+perimeter.size());
		int index;
		for ( MapLocation n : perimeter ) {
			System.out.println(n);
			index = ntLocToIndex(n);
			
			cows[index] = rc.senseCowsAtLocation(n);
		}
		System.out.println("Finished Scanning");

		
		//targetLoc = new MapLocation(dX,dY);
		targetLoc = new MapLocation(myLoc.x-10,myLoc.y-10);
		if ( debugLevel >= 2 ) System.out.println("[OO]  M: "+myLoc.toString()+"\tT: "+targetLoc.toString()+"\tD: "+myLoc.distanceSquaredTo(targetLoc));
		rc.attackSquare(targetLoc);		
	}
	

}
