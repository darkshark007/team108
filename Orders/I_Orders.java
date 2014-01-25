package team108.Orders;

import team108.I_Debugger;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;


/**
 * 
 * @author Stephen Bush
 *
 */
public interface I_Orders extends I_Debugger {
	
	public void executeOrders() throws GameActionException;

	
	
}
