package team108.Orders;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class NoiseTower___TEMPLATE extends NoiseTower {

	
	public NoiseTower___TEMPLATE(RobotController in) { super(in); }

	@Override
	public void executeOrders() throws GameActionException {
		MapLocation targetLoc;
		int dX = 0, dY = 0;

		
		targetLoc = new MapLocation(dX,dY);
		if ( debugLevel >= 2 ) System.out.println("[OO]  M: "+myLoc.toString()+"\tT: "+targetLoc.toString()+"\tD: "+myLoc.distanceSquaredTo(targetLoc));
		rc.attackSquare(targetLoc);		
	}

}
