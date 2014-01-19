package team108.Path;

import java.util.LinkedList;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import team108.I_Debugger;
import team108.Graph.MapRender;

public class ____DirectWithOptimizedBuggingPathGenerator____OLD extends PathGenerator {

	
	public ____DirectWithOptimizedBuggingPathGenerator____OLD(RobotController in) {
		super(in);
	}
	
	public void init() {
		if ( mren == null ) { 
			mren = new MapRender(rc);
			mren.init();
		}
	}
	
	public Path getPath(MapLocation from, MapLocation to) {
		if ( mren == null ) {
			mren = new MapRender(rc);
			mren.init();
		}
		LinkedList<MapLocation> workingPath = new LinkedList<MapLocation>();

		// Add the start point in the path.
		workingPath.add(from);
		
		// First, calculate a direct path, ignoring voids.
		MapLocation temp, current = from;
		Direction d;
		while ( !current.equals(to) ) {
			// Try straight
			d = current.directionTo(to);
			temp = current.add(d);
			//if ( mren.terrainMatrix[temp.x][temp.y] == 99 ) { 
			//	// Try Left
			//	temp = current.add(d.rotateLeft());
			//	if ( mren.terrainMatrix[temp.x][temp.y] == 99 ) {
			//		// Try Right
			//		temp = current.add(d.rotateRight());
			//		if ( mren.terrainMatrix[temp.x][temp.y] == 99 ) {
			//			// If nothing else works, add the void and it will be resolved later.
			//			temp = current.add(d); 
			//		}
			//	}
			//}
			workingPath.add(temp);
			current = temp;
		}
		current = from;
		
		// Now, go through the path and make sure it does not contain any Voids.  If it does, they will be resolved with bugging.
		int i = 0;
		MapLocation buggingTarget;
		while (!current.equals(to)) {
			current = workingPath.get(i);
			if ( debugLevel >= 2 ) System.out.println("Working..."+current.toString());
			if ( mren.terrainMatrix[current.x][current.y] == 99 ) {
				// VOID DETECTED
				if ( current.equals(to) ) {
					// The end of the path is a void, there is no way to bug around to it so stop processing and return the finished path.
					break;
				}
				
				boolean bugLeft = true,bugRight = true;

				if ( debugLevel >= 2 ) System.out.println("Void detected!!  "+current.toString());
				// Set current equal to one step back, or the first non-void before the detected void, as this is our starting point in the bug path.
				current = workingPath.get(i-1);
				// Get the bugging target, which is the next non-void location in the working path.
				// We're going to bug along the wall until reaching this location.
				buggingTarget = current;
				int j = i;
				do {
					j++;					
					buggingTarget = workingPath.get(j);
				} while ( mren.terrainMatrix[buggingTarget.x][buggingTarget.y] == 99 );
				
				if ( debugLevel >= 3 ) System.out.println("Marker #1  "+current.toString());
				MapLocation cBugLeft = current;
				MapLocation cBugRight = current;
				Direction dbLeft = cBugLeft.directionTo(buggingTarget);
				do {
					dbLeft = dbLeft.rotateLeft();
					cBugLeft = current.add(dbLeft);
					if ( !locIsOnMap(cBugLeft) ) {
						bugLeft = false;
						break;
					}
				} while ( mren.terrainMatrix[cBugLeft.x][cBugLeft.y] == 99 );
				cBugLeft = current;
				
				if ( debugLevel >= 3 ) System.out.println("Marker #2  "+current.toString());
				Direction dbRight = cBugRight.directionTo(buggingTarget);
				do {
					dbRight = dbRight.rotateRight();
					cBugRight = current.add(dbRight);
					if ( !locIsOnMap(cBugRight) ) {
						bugRight = false;
						break;
					}
				} while ( mren.terrainMatrix[cBugRight.x][cBugRight.y] == 99 );
				cBugRight = current;

				if ( debugLevel >= 3 ) System.out.println("Marker #3  "+current.toString());
				LinkedList<MapLocation> buggingLeft = new LinkedList<MapLocation>();
				LinkedList<MapLocation> buggingRight = new LinkedList<MapLocation>();
				
				// In order to get the shortest bug path, bug along the wall in both directions until reaching the bugTarget, then keep whichever path gets there first.
				while (true) {
					if ( bugLeft ) {
						// Bug Left along the wall
						// Check to see if we've reached the target.
						if ( cBugLeft.isAdjacentTo(buggingTarget) ) break;
						// Rotate right until we hit a wall, to make sure we're still bugging along the wall.
						if ( debugLevel >= 3 ) System.out.println("Marker #4  "+cBugLeft.toString());
						do {
							dbLeft = dbLeft.rotateRight();
							temp = cBugLeft.add(dbLeft);
							if ( !locIsOnMap(temp) ) {
								bugLeft = false;
								break;
							}
						} while ( mren.terrainMatrix[temp.x][temp.y] != 99);
						if ( debugLevel >= 3 ) System.out.println("Marker #5  "+current.toString());
						// Rotate left until we can move straight
						do {
							dbLeft = dbLeft.rotateLeft();
							temp = cBugLeft.add(dbLeft);
							if ( !locIsOnMap(temp) ) {
								bugLeft = false;
								break;
							}
						} while ( mren.terrainMatrix[temp.x][temp.y] == 99);
						if ( debugLevel >= 3 ) System.out.println("Marker #6  "+current.toString());
						buggingLeft.add(temp);
						cBugLeft = temp;						
					}
					
					if ( bugRight ) {
						// Bug Right along the wall
						// Check to see if we've reached the target.
						if ( cBugRight.isAdjacentTo(buggingTarget) ) break;
						// Rotate Left until we hit a wall, to make sure we're still bugging along the wall.
						do {
							dbRight = dbRight.rotateLeft();
							temp = cBugRight.add(dbRight);
							if ( !locIsOnMap(temp) ) {
								bugRight = false;
								break;
							}
						} while ( mren.terrainMatrix[temp.x][temp.y] != 99);
						if ( debugLevel >= 3 ) System.out.println("Marker #7  "+current.toString());
						// Rotate Right until we can move straight
						do {
							dbRight = dbRight.rotateRight();
							temp = cBugRight.add(dbRight);
							if ( !locIsOnMap(temp) ) {
								bugRight = false;
								break;
							}
						} while ( mren.terrainMatrix[temp.x][temp.y] == 99);
						if ( debugLevel >= 3 ) System.out.println("Marker #8  "+current.toString());
						buggingRight.add(temp);
						cBugRight = temp;						
					}
				}
				if ( debugLevel >= 3 ) System.out.println("Marker #9  "+current.toString());

				// Replace the section of the path that moves through the void with the optimal bug-path around the voids
				// Remove the links from (i) -> (j-1)
				for ( int k = (j-1); k >= i; k-- ) {
					workingPath.remove(k);
				}
				if ( debugLevel >= 3 ) System.out.println("Marker #10  "+current.toString());
				if ( cBugLeft.isAdjacentTo(buggingTarget) ) { workingPath.addAll(i, buggingLeft); }
				else if ( cBugRight.isAdjacentTo(buggingTarget) ) { workingPath.addAll(i, buggingRight); }
				i = i-2;
			}
			i++;
		}
		if ( debugLevel >= 3 ) System.out.println("Marker #11  "+current.toString());
		
		// Optimize the path by trimming redundant sections
		// Often times, bugging around the wall yields to backtracking.
		for ( int itFrom = 0; itFrom < workingPath.size(); itFrom++ ) {
			if ( debugLevel >= 3 ) System.out.println("{OPT} >  "+itFrom);
			for ( int itTo = workingPath.size()-1; itTo > itFrom; itTo-- ) {
				if ( debugLevel >= 3 ) System.out.println("{OPT} >>>  "+itTo);
				// If an earlier node has a direct path to a later node, delete all of the intermediate nodes.
				if ( mren.isDirectPath(workingPath.get(itFrom), workingPath.get(itTo)) ) {
					// Delete all of the intermediate nodes
					if ( debugLevel >= 2 ) System.out.println("Clearing Path between "+workingPath.get(itFrom).toString()+" -> "+workingPath.get(itTo).toString()); 
					for ( int itList = itTo-1; itList > itFrom; itList--) {
						if ( debugLevel >= 3 ) System.out.println("{OPT} >>>>>  "+itList);
						workingPath.remove(itList);
					}
					break;
				}
			}
		}

		// Construct the Path
		Path p = new Path();
		for ( MapLocation n : workingPath ) {
			p.addLinkE(n);
		}
		if ( debugLevel >= 3 ) System.out.println("Marker #12  "+current.toString());

		if ( debugLevel >= 2 ) p.printPath();
		
		return p;
	}
	
	public MapRender getMapRender() {
		if ( mren == null ) {
			mren = new MapRender(rc);
			mren.init();
		}
		return mren;
	}
	
	/**
	 * Checks to see whether the specified location is on the map.  Checks only the map boundaries, does NOT take into account whether the location is a wall.
	 * @param in	The MapLocation to check
	 * @return
	 */
	public boolean locIsOnMap(MapLocation in) {
		if ( in.x < 0 ) return false;
		if ( in.y < 0 ) return false;
		if ( in.x >= rc.getMapWidth() ) return false;
		if ( in.y >= rc.getMapHeight() ) return false;
		return true;
	}


}
