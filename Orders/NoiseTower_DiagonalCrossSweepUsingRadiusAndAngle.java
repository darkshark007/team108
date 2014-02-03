package team108.Orders;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class NoiseTower_DiagonalCrossSweepUsingRadiusAndAngle extends Orders {

	double radius = 0;
	double angle = Math.PI/4;
	// NOTE:    The tower can only attack once per two turns,
	//   so the real "number of turns" is actually double.
	final static double TURNS_TO_SWEEP = 4;
	final static double TURNS_TO_PULL = 25;
	final static double MAX_RADIUS = 250.0;
	final static double MAX_ANGLE = 2*Math.PI;
	
	public NoiseTower_DiagonalCrossSweepUsingRadiusAndAngle(RobotController in) { super(in); }

	@Override
	public void executeOrders() throws GameActionException {
		MapLocation targetLoc;
		double tempRadius = Math.sqrt(MAX_RADIUS-radius);
		double tempAngle = MAX_ANGLE-angle;
		double lengthX = 0,lengthY = 0,LoS = 0;
		int dX=0,dY=0;
		
		LoS = tempRadius/(Math.sin(Math.PI/2));
		System.out.println("R: "+radius+" -> "+tempRadius+"\tA: "+angle+" -> "+tempAngle);
		if ( -1 <= tempAngle && tempAngle < MAX_ANGLE/4 ) {
			lengthX = LoS*Math.sin(Math.PI/2-tempAngle);
			lengthY = LoS*Math.sin(tempAngle);
			dX = myLoc.x+(int)lengthX;
			dY = myLoc.y+(int)lengthY;
			System.out.println("[|||] FIRST\t"+lengthX+"\t"+lengthY+"\t"+dX+"\t"+dY);
		}
		else if ( MAX_ANGLE/4 <= tempAngle && tempAngle < MAX_ANGLE*2/4 ) {
			tempAngle = tempAngle - (MAX_ANGLE*1/4);
			lengthY = LoS*Math.sin(Math.PI/2-tempAngle);
			lengthX = LoS*Math.sin(tempAngle);
			dX = myLoc.x-(int)lengthX;
			dY = myLoc.y+(int)lengthY;
			System.out.println("[|||] SECOND\t"+lengthX+"\t"+lengthY+"\t"+dX+"\t"+dY);
		}
		else if ( MAX_ANGLE*2/4 <= tempAngle && tempAngle < MAX_ANGLE*3/4 ) {
			tempAngle = tempAngle - (MAX_ANGLE*2/4);
			lengthX = LoS*Math.sin(Math.PI/2-tempAngle);
			lengthY = LoS*Math.sin(tempAngle);
			dX = myLoc.x-(int)lengthX;
			dY = myLoc.y-(int)lengthY;
			System.out.println("[|||] THIRD\t"+lengthX+"\t"+lengthY+"\t"+dX+"\t"+dY);
		}
		else if ( MAX_ANGLE*3/4 <= tempAngle && tempAngle <= MAX_ANGLE ) {
			tempAngle = tempAngle - (MAX_ANGLE*3/4);
			lengthY = LoS*Math.sin(Math.PI/2-tempAngle);
			lengthX = LoS*Math.sin(tempAngle);
			dX = myLoc.x+(int)lengthX;
			dY = myLoc.y-(int)lengthY;
			System.out.println("[|||] FOURTH\t"+lengthX+"\t"+lengthY+"\t"+dX+"\t"+dY);
		}
		
		targetLoc = new MapLocation(dX,dY);
		System.out.println("M: "+myLoc.toString()+"\tT: "+targetLoc.toString()+"\tD: "+myLoc.distanceSquaredTo(targetLoc));
		rc.attackSquare(targetLoc);
		
		radius = (radius+(MAX_RADIUS/TURNS_TO_PULL)) % MAX_RADIUS;
		
		angle = (angle+(MAX_ANGLE/TURNS_TO_SWEEP)) % MAX_ANGLE;
	}

}
