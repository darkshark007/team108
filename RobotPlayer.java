package team108;

import java.util.Random;

import team108.Graph.MapRender;
import team108.Orders.*;
import team108.Path.BreadthFirstPathing;
import team108.Path.DepthFirstPathing;
import team108.Path.____DirectWithOptimizedBuggingPathGenerator____OLD;
import team108.Path.DirectWithOptimizedBuggingPathGenerator;
import team108.Path.Path;
import team108.Path.PathGenerator;
import team108.Strategies.*;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

/**
 * The Modular 
 * @author Stephen Bush
 */
public class RobotPlayer {
	
	static I_RobotStrategy strategy;
	
	public static void run(RobotController rc) throws GameActionException {


		
			
			
			
		
		
		
		//strategy = new HardFormationSwarmStrategy(rc);
		//strategy = new SwarmAndScoopStrategy(rc);
		//strategy = new ___NewNoiseTowerTester(rc);
		//strategy = new PassiveHerdingStrategy(rc);
		strategy = new HardFormationSwarmStrategyV2(rc);
		
		
	
		while (true) {
			strategy.run();
			rc.yield();
		}
	}
}
