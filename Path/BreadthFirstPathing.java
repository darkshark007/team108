package team108.Path;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.Stack;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Queue;

import team108.Graph.MapRender;

public class BreadthFirstPathing extends PathGenerator {

	public BreadthFirstPathing(RobotController rc) {
		super(rc);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Path getPath(MapLocation from, MapLocation to) {
		
		if ( mren == null ) mren = new MapRender(rc);
		
		Queue<MapLocation> queue = new ArrayDeque<MapLocation>();
		boolean[] visited = new boolean[width*height];
		MapLocation[] pathTrace = new MapLocation[width*height];
		
		queue.add(from);
		
		MapLocation current;
		int index;
		int i1,i2,i3,i4,i5,i6,i7,i8;
		MapLocation m1,m2,m3,m4,m5,m6,m7,m8;
		Direction dirS,dirL,dirR;
		while ( !queue.isEmpty() ) {
			current = queue.remove();
			index = locationToIndex(current);
			visited[index] = true;

			dirS = current.directionTo(to);
			m1 = current.add(dirS);
			dirL = dirS.rotateLeft();
			m2 = current.add(dirL);
			dirR = dirS.rotateRight();
			m3 = current.add(dirR);
			dirL = dirL.rotateLeft();
			m4 = current.add(dirL);
			dirR = dirR.rotateRight();
			m5 = current.add(dirR);
			dirL = dirL.rotateLeft();
			m6 = current.add(dirL);
			dirR = dirR.rotateRight();
			m7 = current.add(dirR);
			dirS = dirS.opposite();
			m8 = current.add(dirS);
			
			
			// Try UL
			if ( locIsOnMap(m1) ) {
				i1 = locationToIndex(m1);
				if ( !visited[i1] ) {
					if ( mren.terrainMatrix[m1.x][m1.y] != 99 ) {
						queue.add(m1);
						pathTrace[i1] = current;
						if ( m1.equals(to) ) break;
					}
					visited[i1] = true;
				}
			}

		
		
			// Try L
			if ( locIsOnMap(m2) ) {
				i2 = locationToIndex(m2);
				if ( !visited[i2] ) {
					if ( mren.terrainMatrix[m2.x][m2.y] != 99 ) {
						queue.add(m2);
						pathTrace[i2] = current;
						if ( m2.equals(to) ) break;
					}
					visited[i2] = true;
				}
			}

		
			// Try BL
			if ( locIsOnMap(m3) ) {
				i3 = locationToIndex(m3);
				if ( !visited[i3] ) {
					if ( mren.terrainMatrix[m3.x][m3.y] != 99 ) {
						queue.add(m3);
						pathTrace[i3] = current;
						if ( m3.equals(to) ) break;
					}
					visited[i3] = true;
				}
			}

		
			// Try B
			if ( locIsOnMap(m4) ) {
				i4 = locationToIndex(m4);
				if ( !visited[i4] ) {
					if ( mren.terrainMatrix[m4.x][m4.y] != 99 ) {
						queue.add(m4);
						pathTrace[i4] = current;
						if ( m4.equals(to) ) break;
					}
					visited[i4] = true;
				}
			}
			
			
			// Try BR
			if ( locIsOnMap(m5) ) {
				i5 = locationToIndex(m5);
				if ( !visited[i5] ) {
					if ( mren.terrainMatrix[m5.x][m5.y] != 99 ) {
						queue.add(m5);
						pathTrace[i5] = current;
						if ( m5.equals(to) ) break;
					}
					visited[i5] = true;
				}
			}
			
			
			// Try R
			if ( locIsOnMap(m6) ) {
				i6 = locationToIndex(m6);
				if ( !visited[i6] ) {
					if ( mren.terrainMatrix[m6.x][m6.y] != 99 ) {
						queue.add(m6);
						pathTrace[i6] = current;
						if ( m6.equals(to) ) break;
					}
					visited[i6] = true;
				}
			}
			
			
			// Try UR
			if ( locIsOnMap(m7) ) {
				i7 = locationToIndex(m7);
				if ( !visited[i7] ) {
					if ( mren.terrainMatrix[m7.x][m7.y] != 99 ) {
						queue.add(m7);
						pathTrace[i7] = current;
						if ( m7.equals(to) ) break;
					}
					visited[i7] = true;
				}
			}
			
			
			// Try U
			if ( locIsOnMap(m8) ) {
				i8 = locationToIndex(m8);
				if ( !visited[i8] ) {
					if ( mren.terrainMatrix[m8.x][m8.y] != 99 ) {
						queue.add(m8);
						pathTrace[i8] = current;
						if ( m8.equals(to) ) break;
					}
					visited[i8] = true;
				}
			}
		}
		if ( queue.isEmpty() ) return null;

		// Build the path
		Path p = new Path();
		current = to;
		while ( !current.equals(from) ) {
			//System.out.println("Build>>  "+current.toString());
			p.addLink(current);
			index = locationToIndex(current);
			current = pathTrace[index];
		}
		//System.out.println("Optimize>>  "+current.toString());		
		optimizePathSection(p.getList(),0,p.getList().size()-1);
		
		return p;
	}

}
