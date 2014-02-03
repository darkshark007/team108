package team108.Strategies;

import team108.Orders.I_Orders;
import team108.Orders.NoiseTower_CircularSweepUsingRadiusAndAngle;
import team108.Orders.NoiseTower_DepthFirstPathGenerator;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class ___NewNoiseTowerTester extends ___Strategy___ModifiedTakeStep {

	public ___NewNoiseTowerTester(RobotController in) { super(in); }

	public void run() {
		try {

			if ( rc.getType() == RobotType.HQ ) {
				rc.spawn(Direction.SOUTH);
				//for ( MapLocation n : MapLocation.getAllMapLocationsWithinRadiusSq(new MapLocation(30,30), 300)) System.out.println(n);
				while (!rc.isActive()) rc.yield();
				rc.spawn(Direction.SOUTH);
				while (!rc.isActive()) rc.yield();
				//rc.spawn(Direction.SOUTH);
				while (true) rc.yield();
			}
			if ( rc.getType() == RobotType.SOLDIER ) {
				if ( rc.getRobot().getID() == 105 ) {
					MapLocation cLoc = new MapLocation(36,38);
					while ( !rc.getLocation().equals(cLoc) ) {
						if ( rc.isActive() ) takeStepTowards(cLoc);
						rc.yield();
					}
					while ( !rc.isActive() ) {
						rc.yield();
					}
					rc.construct(RobotType.NOISETOWER);
					while (true) rc.yield();
				}
				if ( rc.getRobot().getID() == 280 ) {
					MapLocation cLoc = new MapLocation(39,12);
					while ( !rc.getLocation().equals(cLoc) ) {
						if ( rc.isActive() ) takeStepTowards(cLoc);
						rc.yield();
					}
					while ( !rc.isActive() ) {
						rc.yield();
					}
					rc.construct(RobotType.NOISETOWER);
					while (true) rc.yield();
				}
				if ( rc.getRobot().getID() == 172 ) {
					MapLocation cLoc = new MapLocation(37,39);
					while ( !rc.getLocation().equals(cLoc) ) {
						if ( rc.isActive() ) takeStepTowards(cLoc);
						rc.yield();
					}
					while ( !rc.isActive() ) {
						rc.yield();
					}
					rc.construct(RobotType.PASTR);
					while (true) rc.yield();
				}
			}
			if ( rc.getType() == RobotType.NOISETOWER ) {
				I_Orders ord = new NoiseTower_CircularSweepUsingRadiusAndAngle(rc);
				NoiseTower_DepthFirstPathGenerator ord2 = new NoiseTower_DepthFirstPathGenerator(rc);
				while (true) {
					System.out.println("AD:  "+rc.getActionDelay());
					if ( rc.isActive() ) ord.executeOrders();

					
					// Handle the data processing.
					int sTurn = Clock.getRoundNum();
					System.out.println("Density:  "+ord2.getPathDensity());
					if ( ord2.getPathDensity() < 0.5 ) {
						ord = ord2;
					}
					ord2.processData(); 
					if ( sTurn == Clock.getRoundNum() ) rc.yield();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			rc.breakpoint();
		}
	}
}
