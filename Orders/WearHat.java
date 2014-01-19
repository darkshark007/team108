package team108.Orders;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

/**
 * This is a simple example of a series of executable instructions that might be defined using the I_Orders interface. 
 * @author Stephen Bush
 */
public class WearHat implements I_Orders {
	
	boolean isWearingHat = false;

	@Override
	public void executeOrders(RobotController in) {
		// TODO Auto-generated method stub
		try {
			in.setIndicatorString(0,"R: "+Clock.getRoundNum());
			if ( !isWearingHat ) {	
				in.wearHat();
				isWearingHat = true;
			}
		} catch (GameActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

}
