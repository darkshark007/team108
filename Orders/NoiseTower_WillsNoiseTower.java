package team108.Orders;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class NoiseTower_WillsNoiseTower extends NoiseTower {


	public NoiseTower_WillsNoiseTower(RobotController in) { super(in); }

	@Override
	public void executeOrders() throws GameActionException {
		/* */


		MapLocation myLoc[][] = new MapLocation[17][17];

		for ( int i1 = 0; i1 < myLoc.length; i1++ ) {
			for ( int i2 = 0; i2 < myLoc.length; i2++ ) {
				myLoc[i1][i2] = rc.getLocation();
			}		
		}



		//int j = myLoc[0][0].y + 8;
		//int i = myLoc[0][0].x + 8;
		int j = 8;
		int i = 8;



		try {

			while(j > 0){

				i = 8;

				while(i > 0)

				{

					i--;

					if(rc.canAttackSquare(myLoc[i][j])) {
						rc.attackSquare(myLoc[i][j]);
						rc.yield();
						rc.yield();
					}






				}

				j--;

			}

		} catch (Exception e) {

			rc.breakpoint();

			e.printStackTrace();
		}
		/* */



		/*
		MapLocation targetLoc;
		int dX = 0, dY = 0;


		targetLoc = new MapLocation(dX,dY);
		if ( debugLevel >= 2 ) System.out.println("[OO]  M: "+myLoc.toString()+"\tT: "+targetLoc.toString()+"\tD: "+myLoc.distanceSquaredTo(targetLoc));
		rc.attackSquare(targetLoc);
		 */		
	}

}
