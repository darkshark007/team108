package team108.Orders;

import java.util.LinkedList;

import team108.Graph.SimpleLinkedList;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class NoiseTower_MaximumCowsPerPathGreedyFromCenterStrategy extends Orders {

	
	public NoiseTower_MaximumCowsPerPathGreedyFromCenterStrategy(RobotController in) { super(in); }

	@Override
	public void executeOrders() throws GameActionException {
		MapLocation targetLoc;
		int dX = 0, dY = 0;

		
		targetLoc = new MapLocation(dX,dY);
		if ( debugLevel >= 2 ) System.out.println("[OO]  M: "+myLoc.toString()+"\tT: "+targetLoc.toString()+"\tD: "+myLoc.distanceSquaredTo(targetLoc));
		rc.attackSquareLight(targetLoc);		
	}
	
	private LinkedList<MapLocation> getFurtherAdjacentPoints(MapLocation start) {
		LinkedList<MapLocation> sll = new SimpleLinkedList<MapLocation>();
		
		
		
		
		return sll;
	}
	
	private LinkedList<MapLocation> getStartingPerimeter() {
		LinkedList<MapLocation> sll = new SimpleLinkedList<MapLocation>();

		
		return sll;
	}

}
