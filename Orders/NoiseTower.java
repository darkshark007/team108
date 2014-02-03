package team108.Orders;

import java.util.LinkedList;
import java.util.Stack;

import team108.Graph.SimpleLinkedList;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public abstract class NoiseTower extends Orders {

	final static int MAX_RANGE = RobotType.NOISETOWER.attackRadiusMaxSquared;
	final static int MAX_SENSOR_RANGE = RobotType.NOISETOWER.sensorRadiusSquared;
	final static int MAX_RADIUS = (int)Math.sqrt(MAX_RANGE);
	
	public NoiseTower(RobotController in) { super(in); }
	
	public int getTurnsTillConvergence() {
		return 0;
	}
	
	
	protected LinkedList<MapLocation> getAllAdjacentPoints(MapLocation start) {
		LinkedList<MapLocation> sll = new SimpleLinkedList<MapLocation>();
		
		sll.add(new MapLocation(start.x+1,start.y+1));
		sll.add(new MapLocation(start.x+1,start.y));
		sll.add(new MapLocation(start.x+1,start.y-1));
		sll.add(new MapLocation(start.x,start.y-1));
		sll.add(new MapLocation(start.x-1,start.y-1));
		sll.add(new MapLocation(start.x-1,start.y));
		sll.add(new MapLocation(start.x-1,start.y+1));
		sll.add(new MapLocation(start.x,start.y+1));
		
		return sll;
	}

	protected LinkedList<MapLocation> getFartherNTPoints(MapLocation start) {
		double myDist = start.distanceSquaredTo(myLoc);
		LinkedList<MapLocation> sll = new SimpleLinkedList<MapLocation>();
		
		
		MapLocation m1 = new MapLocation(start.x+1,start.y+1);
		MapLocation m2 = new MapLocation(start.x+1,start.y);
		MapLocation m3 = new MapLocation(start.x+1,start.y-1);
		MapLocation m4 = new MapLocation(start.x,start.y-1);
		MapLocation m5 = new MapLocation(start.x-1,start.y-1);
		MapLocation m6 = new MapLocation(start.x-1,start.y);
		MapLocation m7 = new MapLocation(start.x-1,start.y+1);
		MapLocation m8 = new MapLocation(start.x,start.y+1);

		if ( m1.distanceSquaredTo(myLoc) > myDist ) sll.add(m1);
		if ( m2.distanceSquaredTo(myLoc) > myDist ) sll.add(m2);
		if ( m3.distanceSquaredTo(myLoc) > myDist ) sll.add(m3);
		if ( m4.distanceSquaredTo(myLoc) > myDist ) sll.add(m4);
		if ( m5.distanceSquaredTo(myLoc) > myDist ) sll.add(m5);
		if ( m6.distanceSquaredTo(myLoc) > myDist ) sll.add(m6);
		if ( m7.distanceSquaredTo(myLoc) > myDist ) sll.add(m7);
		if ( m8.distanceSquaredTo(myLoc) > myDist ) sll.add(m8);

		return sll;
	}

	protected int ntLocToIndex(MapLocation loc) {
		return (35*(loc.x-(myLoc.x-MAX_RADIUS)))+(loc.y-(myLoc.y-17));
	}

	protected MapLocation indexToNTLoc(int index) {
		return new MapLocation((int)(index/35)+(myLoc.x-17),(index % 35)+(myLoc.y-17));
	}

}
