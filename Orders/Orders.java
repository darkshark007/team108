package team108.Orders;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class Orders implements I_Orders {

	MapLocation myLoc = null;
	RobotController rc;
	
	public Orders(RobotController in) {
		rc = in;
		myLoc = rc.getLocation();
	}
	
	public boolean locIsOnMap(MapLocation in) {
		if ( in.x < 0 ) return false;
		if ( in.y < 0 ) return false;
		if ( in.x >= rc.getMapWidth() ) return false;
		if ( in.y >= rc.getMapHeight() ) return false;
		return true;
	}

}
