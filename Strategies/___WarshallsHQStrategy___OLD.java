package team108.Strategies;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class ___WarshallsHQStrategy___OLD extends Strategy {
	
	int height;
	int width;

	public ___WarshallsHQStrategy___OLD(RobotController in) { super(in); }

	public void run() {
		Direction dir;
		MapLocation myLoc;
		MapLocation center = new MapLocation(rc.getMapWidth() / 2, rc.getMapHeight() / 2);
		try {
			switch (rc.getType()) {
			case HQ:
				// Find Spawn direction
				// Check Straight
				myLoc = rc.getLocation(); 
				dir = myLoc.directionTo(rc.senseEnemyHQLocation());
				
				width = rc.getMapWidth();
				height = rc.getMapHeight();
				int i,j;
				
				short[][] terrainMatrix = new short[width][height];
				int[][] adjacencyMatrix = new int[width*height][width*height];
				int bc = Clock.getBytecodeNum();

				// 1.  Compile the terrainMatrix
				/* V1
				for ( int i = 0; i < width; i++ ) {
					for ( int j = 0; j < height; j++ ) {
						//System.out.println("S: "+bc+"\tF: "+Clock.getBytecodeNum()+"\t"+(Clock.getBytecodeNum()-bc));
						//bc = Clock.getBytecodeNum();
						switch(rc.senseTerrainTile(new MapLocation(i,j))) {
						case VOID:
							terrainMatrix[i][j] = 99;
							break;
						case NORMAL:
							terrainMatrix[i][j] = 1;
							break;
						case ROAD:
							terrainMatrix[i][j] = 2;
							break;
						default:
							break;
						}
					}
				}
				/* */

				
				
				/* V2
				
				MapLocation[] array = MapLocation.getAllMapLocationsWithinRadiusSq(center, center.distanceSquaredTo(new MapLocation(width-1,height-1)));
				System.out.println("Size:  "+array.length);
				for ( MapLocation n : array ) {
					//System.out.println("S: "+bc+"\tF: "+Clock.getBytecodeNum()+"\t"+(Clock.getBytecodeNum()-bc));
					//bc = Clock.getBytecodeNum();
					switch(rc.senseTerrainTile(n)) {
						case NORMAL:
							terrainMatrix[n.x][n.y] = 1;
							break;
						case OFF_MAP:
							break;
						case ROAD:
							terrainMatrix[n.x][n.y] = 2;
							break;
						case VOID:
							terrainMatrix[n.x][n.y] = 99;
							break;
						default:
							break;
					}					
				}
				/* */

				
				
				
				/* V3 */
				TerrainTile t;
				for ( i = 0; i < width; i++ ) {
					for ( j = 0; j < height; j++ ) {
						//System.out.println("S: "+bc+"\tF: "+Clock.getBytecodeNum()+"\t"+(Clock.getBytecodeNum()-bc));
						//bc = Clock.getBytecodeNum();
						t = rc.senseTerrainTile(new MapLocation(i,j));
						if ( t.equals(TerrainTile.NORMAL) ) terrainMatrix[i][j] = 1;
						else if ( t.equals(TerrainTile.VOID) ) terrainMatrix[i][j] = 99;
						else if ( t.equals(TerrainTile.ROAD) ) terrainMatrix[i][j] = 2;
					}
				}
				/* */
				
				

				// 2.   Build the adjacency matrix
				// First, null out the matrix
				for ( i = 0; i < width*height; i++) {
					for ( j = 0; j < width*height; j++) {
						adjacencyMatrix[i][j] = 999999999;
					}					
				}
				// First build 0,0
				i = 0;
				j = 0;
				if ( terrainMatrix[0][0] == 1 ) { // Normal space
					if ( terrainMatrix[0][1] != 99 ) { adjacencyMatrix[0][coordsToIndex(0,1)] = 50; }
					if ( terrainMatrix[1][0] != 99 ) { adjacencyMatrix[0][coordsToIndex(1,0)] = 50; }
					if ( terrainMatrix[1][1] != 99 ) { adjacencyMatrix[0][coordsToIndex(1,1)] = 70; }
				}

				
				
				
				for ( i = 1; i < width-1; i++ ) {
					for ( j = 1; j < height-1; j++ ) {
						
					}
				}

				while ( true ) { 
					if ( rc.isActive() ) {
						// Spawn a soldier
						if ( rc.canMove(dir) ) rc.spawn(dir);
					}
					
					rc.yield();
				}
			
			case SOLDIER:
				while ( true ) {
					if ( rc.isActive() ) {
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
	private int locationToIndex(MapLocation in) { return coordsToIndex(in.x,in.y); }
	private int coordsToIndex(int x,int y) { return (x*height)+y; }
	private MapLocation indexToLocation(int in) { return new MapLocation(in/height,in%height); }
}
