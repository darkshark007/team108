package team108.Orders;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public abstract class Orders implements I_Orders {

	MapLocation myLoc = null;
	RobotController rc;
	int height;
	int width;
	
	public Orders(RobotController in) {
		rc = in;
		myLoc = rc.getLocation();
		height = rc.getMapHeight();
		width = rc.getMapWidth();
	}
	
	/* */
	public boolean locIsOnMap(MapLocation in) {
		if ( in.x < 0 ) return false;
		if ( in.y < 0 ) return false;
		if ( in.x >= width ) return false;
		if ( in.y >= height ) return false;
		return true;
	}
	/* */
	
	// This method is optional.  Override if used.
	public void processData() { }

	/*
	public boolean locIsOnMap(MapLocation in) {
		System.out.println("NewLocIsOnMap");
		return !(rc.senseTerrainTile(in).equals(TerrainTile.OFF_MAP));
	}
	/* */

}
