package team108.Path;

import java.util.LinkedList;

import team108.Graph.MapRender;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class PathGenerator implements I_PathGenerator {

	MapRender mren = null;
	RobotController rc;
	int width;
	int height;
	
	public PathGenerator(RobotController rcin, MapRender in) {
		mren = in;
		rc = rcin;
		width = rc.getMapWidth();
		height = rc.getMapHeight();
	}
	
	public PathGenerator(RobotController rcin) {
		this(rcin,null);
	}

	public abstract Path getPath(MapLocation from, MapLocation to);
	
	public MapRender getMapRender() {
		if ( mren == null ) {
			mren = new MapRender(rc);
		}
		return mren; 
	}
	
	public int optimizePathSection(LinkedList<MapLocation> workingPath, int from, int to) {
		int firstOpt = -1;
		int finLen = workingPath.size()-to;
		MapLocation temp = workingPath.get(to);
		
		if ( workingPath.size() < 1 ) return -1;
		
		int itFrom;
		int itTo;
		/* Optimization????
		 * Didnt seem to work 
		 
		for ( itFrom = from; itFrom < workingPath.size()-finLen-1; itFrom++ ) {
			Direction curDir = workingPath.get(itFrom).directionTo(workingPath.get(itFrom+1));
			itTo = itFrom;
			Direction tCurDir = workingPath.get(itTo).directionTo(workingPath.get(itTo+1));
			while ( tCurDir.equals(curDir) && ++itTo < workingPath.size()-finLen ) {
				tCurDir = workingPath.get(itTo).directionTo(workingPath.get(itTo+1));
			}

			if ( debugLevel >= 2 ) System.out.println("Pre-Clearing Path between "+workingPath.get(itFrom).toString()+" -> "+workingPath.get(itTo).toString()); 
			for ( int itList = itTo-1; itList > itFrom; itList--) {
				if ( debugLevel >= 3 ) System.out.println("{REM} >>>>>  "+itList);
				workingPath.remove(itList);
			}
		}
		/* */
		
		
		itFrom = from;
		itTo = to;
		for ( ; !workingPath.get(itFrom).equals(temp); itFrom++ ) {
			if ( debugLevel >= 3 ) System.out.println("{OPT} >  "+itFrom);
			for ( itTo = workingPath.indexOf(temp); itTo-1 > itFrom; itTo-- ) {
				if ( debugLevel >= 3 ) System.out.println("{OPT} >>>  "+itTo);
				// If an earlier node has a direct path to a later node, delete all of the intermediate nodes.
				if ( mren.isDirectPath(workingPath.get(itFrom), workingPath.get(itTo)) ) {
					if ( firstOpt == -1 ) firstOpt = itFrom;
					// Delete all of the intermediate nodes
					if ( debugLevel >= 2 ) System.out.println("Clearing Path between "+workingPath.get(itFrom).toString()+" -> "+workingPath.get(itTo).toString()); 
					for ( int itList = itTo-1; itList > itFrom; itList--) {
						if ( debugLevel >= 3 ) System.out.println("{REM} >>>>>  "+itList);
						workingPath.remove(itList);
					}
					break;
				}
			}
		}
		if ( debugLevel >= 3 ) System.out.println("FirstOp: "+firstOpt+"\tTo: "+to);
		if ( firstOpt == -1 ) return from; 
		else return firstOpt;
	}
	
	
	/**
	 * Checks to see whether the specified location is on the map.  Checks only the map boundaries, does NOT take into account whether the location is a wall.
	 * @param in	The MapLocation to check
	 * @return
	 */
	protected boolean locIsOnMap(MapLocation in) {
		if ( in.x < 0 ) return false;
		if ( in.y < 0 ) return false;
		if ( in.x >= rc.getMapWidth() ) return false;
		if ( in.y >= rc.getMapHeight() ) return false;
		return true;
	}
	
	protected int locationToIndex(MapLocation in) {
		return (in.x*height)+in.y;
	}



}
