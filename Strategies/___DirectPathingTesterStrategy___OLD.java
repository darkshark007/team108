package team108.Strategies;

import java.util.LinkedList;

import team108.Graph.Graph;
import team108.Graph.MapRender;
import team108.Path.____DirectWithOptimizedBuggingPathGenerator____OLD;
import team108.Path.ObstaclePointPathGenerator;
import team108.Path.Path;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ___DirectPathingTesterStrategy___OLD extends Strategy {

	public ___DirectPathingTesterStrategy___OLD(RobotController in) { super(in); }

	public void run() {
		Direction dir;
		MapLocation myLoc;
		try {
			switch (rc.getType()) {
			case HQ:
				// Find Spawn direction
				// Check Straight
				myLoc = rc.getLocation(); 
				dir = myLoc.directionTo(rc.senseEnemyHQLocation());
				
		
				while ( true ) { 
					if ( rc.isActive() ) {
						// Spawn a soldier
						if ( rc.canMove(dir) ) rc.spawn(dir);
					} 
					rc.yield();
				}
			
			case SOLDIER:
				____DirectWithOptimizedBuggingPathGenerator____OLD gr = new ____DirectWithOptimizedBuggingPathGenerator____OLD(rc);
				
				Path p = gr.getPath(rc.getLocation(), rc.senseEnemyHQLocation());
				System.out.println("Path Generated"+"\t\tAD: "+rc.getActionDelay());
				p.printPath();
				MapLocation myTarget = p.nextLink();
				while ( true ) {
					if ( rc.isActive() ) {
						if ( rc.getLocation().equals(myTarget) ) myTarget = p.nextLink();
						takeStepTowards(myTarget);
					}
					rc.yield();
				}
				
			case NOISETOWER:
				while ( true ) {
					if ( rc.isActive() ) {
					}
					rc.yield();
				}

			case PASTR:
				while ( true ) {
					if ( rc.isActive() ) {
					}
					rc.yield();
				}

			default:
				break;
			}
		} catch (Exception e) {
			rc.breakpoint();
			e.printStackTrace();
		}
	}	
}
