package team108.Strategies;

import team108.I_Debugger;
import team108.Orders.I_Orders;
import team108.Path.Path;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public interface I_RobotStrategy extends I_Debugger {

	public void run();
	
	public void goTo(MapLocation in) throws GameActionException;
	public void goTo(MapLocation in,I_Orders orders) throws GameActionException;
	public void followPath(Path in) throws GameActionException;
	public Direction takeStepTowards(MapLocation in) throws GameActionException;
	//public boolean locIsOnMap(MapLocation in);
}
