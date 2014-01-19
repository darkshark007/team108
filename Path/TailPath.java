package team108.Path;

import battlecode.common.MapLocation;

public class TailPath {

	tailPathNode root;
	
	public TailPath(MapLocation in) { this(in.x,in.y); }
	
	/* OLD 
	public TailPath(int x, int y) {
		root = new tailPathNode(x-2,y);
		tailPathNode next;
		next = root.next = new tailPathNode(x-1,y+1);
		next = next.next = new tailPathNode(x,y+1);
		next = next.next = new tailPathNode(x,y+2);
		next = next.next = new tailPathNode(x+1,y+1);
		next = next.next = new tailPathNode(x+1,y);
		next = next.next = new tailPathNode(x+2,y);
		next = next.next = new tailPathNode(x+1,y-1);
		next = next.next = new tailPathNode(x,y-1);
		next = next.next = new tailPathNode(x,y-2);
		next = next.next = new tailPathNode(x-1,y-1);
		next = next.next = new tailPathNode(x-1,y);
		next.next = root;
	}
	/* */
	
	public TailPath(int x, int y) {
		root = new tailPathNode(x-1,y+1);
		tailPathNode next;
		next = root.next = new tailPathNode(x,y+1);
		next = next.next = new tailPathNode(x+1,y+1);
		next = next.next = new tailPathNode(x+1,y);
		next = next.next = new tailPathNode(x+1,y-1);
		next = next.next = new tailPathNode(x,y-1);
		next = next.next = new tailPathNode(x-1,y-1);
		next = next.next = new tailPathNode(x-1,y);
		next.next = root;
	}
	
	public boolean checkPath(MapLocation test) {			
		tailPathNode start = root;
		do {
			if ( start.data.equals(test)) return true;
			start = start.next;	
		} while ( start != root );
		return false;
	}
	
	public Path getPathStartingAt(MapLocation in) {
		tailPathNode start = root;
		do {
			if ( start.data.equals(in)) break;
			start = start.next;
			if ( start == root ) return null;
		} while ( true );
		
		Path pth = new Path();
		
		tailPathNode current = start;
		do {
			pth.addLinkE(current.data);
			current = current.next;	
		} while ( current != start );
		
		return pth;
	}

	public class tailPathNode {
		
		MapLocation data;
		tailPathNode next;
		
		public tailPathNode(int x, int y) {
			data = new MapLocation(x,y);
		}
	}

}
