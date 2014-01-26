package team108.Orders;

import java.util.LinkedList;
import java.util.Stack;

import team108.Graph.SimpleLinkedList;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public abstract class NoiseTower extends Orders {

	final static int MAX_RANGE = RobotType.NOISETOWER.attackRadiusMaxSquared;
	final static int MAX_RADIUS = (int)Math.sqrt(MAX_RANGE);
	
	public NoiseTower(RobotController in) { super(in); }
	
	
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
	
	protected int ntLocToIndex(MapLocation loc) {
		return (35*(loc.x-(myLoc.x-MAX_RADIUS)))+(loc.y-(myLoc.y-17));
	}

	protected MapLocation indexToNTLoc(int index) {
		return new MapLocation((int)(index/35)+(myLoc.x-17),(index % 35)+(myLoc.y-17));
	}

}
