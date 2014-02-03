package team108.Orders;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class NoiseTower_CircularSweepUsingRadiusAndAngle extends NoiseTower {

	double radius = 0;
	double angle = 0.0;
	int nullAreas = 0;
	// NOTE:    The tower can only attack once per two turns,
	//   so the real "number of turns" is actually double.
	final static double TURNS_TO_SWEEP = 16;
	final static double TURNS_TO_PULL = 175.0;
	final static double MAX_RADIUS = 250.0;
	final static double MAX_ANGLE = 2*Math.PI;
	
	public NoiseTower_CircularSweepUsingRadiusAndAngle(RobotController in) { super(in); }

	@Override
	public int getTurnsTillConvergence() {
		return (int)((((MAX_RADIUS)-radius)/MAX_RADIUS)*(TURNS_TO_PULL-nullAreas)*2.0)-80;
	}
	
	@Override
	public void executeOrders() throws GameActionException {
		MapLocation targetLoc;
		double tempRadius = Math.sqrt(MAX_RADIUS-radius);
		double tempAngle = MAX_ANGLE-angle;
		double lengthX = 0,lengthY = 0,LoS = 0;
		int dX=0,dY=0;
		
		LoS = tempRadius/(Math.sin(Math.PI/2));
		if ( debugLevel >= 3 ) System.out.println("R: "+radius+" -> "+tempRadius+"\tA: "+angle+" -> "+tempAngle);
		if ( -1 <= tempAngle && tempAngle < MAX_ANGLE/4 ) {
			lengthX = LoS*Math.sin(Math.PI/2-tempAngle);
			lengthY = LoS*Math.sin(tempAngle);
			dX = myLoc.x+(int)lengthX;
			dY = myLoc.y+(int)lengthY;
			if ( debugLevel >= 3 ) System.out.println("[|||] FIRST\t"+lengthX+"\t"+lengthY+"\t"+dX+"\t"+dY);
		}
		else if ( MAX_ANGLE/4 <= tempAngle && tempAngle < MAX_ANGLE*2/4 ) {
			tempAngle = tempAngle - (MAX_ANGLE*1/4);
			lengthY = LoS*Math.sin(Math.PI/2-tempAngle);
			lengthX = LoS*Math.sin(tempAngle);
			dX = myLoc.x-(int)lengthX;
			dY = myLoc.y+(int)lengthY;
			if ( debugLevel >= 3 ) System.out.println("[|||] SECOND\t"+lengthX+"\t"+lengthY+"\t"+dX+"\t"+dY);
		}
		else if ( MAX_ANGLE*2/4 <= tempAngle && tempAngle < MAX_ANGLE*3/4 ) {
			tempAngle = tempAngle - (MAX_ANGLE*2/4);
			lengthX = LoS*Math.sin(Math.PI/2-tempAngle);
			lengthY = LoS*Math.sin(tempAngle);
			dX = myLoc.x-(int)lengthX;
			dY = myLoc.y-(int)lengthY;
			if ( debugLevel >= 3 ) System.out.println("[|||] THIRD\t"+lengthX+"\t"+lengthY+"\t"+dX+"\t"+dY);
		}
		else if ( MAX_ANGLE*3/4 <= tempAngle && tempAngle <= MAX_ANGLE ) {
			tempAngle = tempAngle - (MAX_ANGLE*3/4);
			lengthY = LoS*Math.sin(Math.PI/2-tempAngle);
			lengthX = LoS*Math.sin(tempAngle);
			dX = myLoc.x+(int)lengthX;
			dY = myLoc.y-(int)lengthY;
			if ( debugLevel >= 3 ) System.out.println("[|||] FOURTH\t"+lengthX+"\t"+lengthY+"\t"+dX+"\t"+dY);
		}
		
		
		targetLoc = new MapLocation(dX,dY);
		
		// Check to see if this loc is too far off the map.
		MapLocation temp = targetLoc.add(targetLoc.directionTo(myLoc)).add(targetLoc.directionTo(myLoc));
		if ( rc.senseTerrainTile(temp).equals(TerrainTile.OFF_MAP) ) {
			// If it is too far off the map, skip and go to the next point
			radius = (radius+(MAX_RADIUS/TURNS_TO_PULL)) % MAX_RADIUS;
			
			angle = (angle+(MAX_ANGLE/TURNS_TO_SWEEP)) % MAX_ANGLE;
			nullAreas++;
			executeOrders();
			return;
		}
		if ( debugLevel >= 2 ) System.out.println("[OO]  M: "+myLoc.toString()+"\tT: "+targetLoc.toString()+"\tD: "+myLoc.distanceSquaredTo(targetLoc));


		rc.attackSquare(targetLoc);
		//rc.attackSquareLight(targetLoc);
		
		radius = (radius+(MAX_RADIUS/TURNS_TO_PULL)) % MAX_RADIUS;
		
		angle = (angle+(MAX_ANGLE/TURNS_TO_SWEEP)) % MAX_ANGLE;
	}

}
