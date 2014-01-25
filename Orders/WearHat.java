package team108.Orders;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

/**
 * This is a simple example of a series of executable instructions that might be defined using the I_Orders interface. 
 * @author Stephen Bush
 */
public class WearHat extends Orders {
	
	public WearHat(RobotController in) { super(in); }

	boolean isWearingHat = false;

	@Override
	public void executeOrders() {
		// TODO Auto-generated method stub
		try {
			rc.setIndicatorString(0,"R: "+Clock.getRoundNum());
			if ( !isWearingHat ) {	
				rc.wearHat();
				isWearingHat = true;
			}
		} catch (GameActionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

}
