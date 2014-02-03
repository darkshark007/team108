package team108.Orders;

import java.util.LinkedList;
import java.util.Stack;

import team108.Graph.MapRender;
import team108.Graph.SimpleLinkedList;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ___NV___NoiseTower_MaximumCowsPerPathGreedyFromEndStrategy extends NoiseTower {

	MapLocation[] Perimeter;
	
	public ___NV___NoiseTower_MaximumCowsPerPathGreedyFromEndStrategy(RobotController in) { 
		super(in);

		System.out.println("Calculating Perimeter...");
		Perimeter = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), 300);
		
		for ( MapLocation n : Perimeter ) {
			System.out.println(n.toString());
		}
	}

	@Override
	public void executeOrders() throws GameActionException {
		MapLocation targetLoc;
		int dX = 0, dY = 0;

		
		
		targetLoc = myLoc;
		if ( debugLevel >= 2 ) System.out.println("[OO]  M: "+myLoc.toString()+"\tT: "+targetLoc.toString()+"\tD: "+myLoc.distanceSquaredTo(targetLoc));
		rc.attackSquare(targetLoc);		
	}
	

}
