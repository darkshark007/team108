package team108.Graph;

import java.util.ArrayList;
import java.util.LinkedList;

import team108.I_Debugger;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class MapRender implements I_Debugger {
		
	int width;
	int height;
	RobotController rc;
	LinkedList<MapLocation> voids = null;
	public short[][] terrainMatrix;
	public double[][] cowSpawnRate;
	short[][] directAdjMatrix;
	
	int totalCount = 0;
	int reuseCount = 0;
	boolean FLAG_CollectVoids = false;

	public MapRender(RobotController in) {
		rc = in;
		width = rc.getMapWidth();
		height = rc.getMapHeight();
		terrainMatrix = new short[width][height];
		directAdjMatrix = new short[width*height][width*height];
		cowSpawnRate = rc.senseCowGrowth();
		// TODO Auto-generated constructor stub
	}
	
	public void init() {

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
		if ( FLAG_CollectVoids ) voids = new SimpleLinkedList<MapLocation>();
		//if ( FLAG_CollectVoids ) voids = new LinkedList<MapLocation>();
		TerrainTile t;
		MapLocation m;
		for ( int i = 0; i < width; i++ ) {
			for ( int j = 0; j < height; j++ ) {
				m = new MapLocation(i,j);
				t = rc.senseTerrainTile(m);
				if ( t.equals(TerrainTile.NORMAL) ) {
					terrainMatrix[i][j] = 1;
				}
				else if ( t.equals(TerrainTile.VOID) ) {
					terrainMatrix[i][j] = 99;
					if ( FLAG_CollectVoids ) voids.add(m);
				}
				else if ( t.equals(TerrainTile.ROAD) ) {
					terrainMatrix[i][j] = 2;
				}
			}
		}		
		/* */
		
		// Look up both HQs and flag them as voids to indicate that it's never possible to move through them.
		m = rc.senseHQLocation();
		terrainMatrix[m.x][m.y] = 99;
		m = rc.senseEnemyHQLocation();
		terrainMatrix[m.x][m.y] = 99;

		if ( debugLevel >= 1 ) System.out.println("[>] Map Render Complete");
	}
	
	public void blacklistEnemyBasePerimeter() {
		MapLocation eHQ = rc.senseEnemyHQLocation();
		MapLocation temp;
		int x = eHQ.x;
		int y = eHQ.y;
		
		// Row 1
		temp = new MapLocation(x,y-5); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;

		// Row 2
		temp = new MapLocation(x-3,y-4); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-2,y-4); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-1,y-4); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x,y-4); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+1,y-4); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+2,y-4); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+3,y-4); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		
		// Row 3
		temp = new MapLocation(x-4,y-3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-3,y-3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-2,y-3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-1,y-3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x,y-3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+1,y-3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+2,y-3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+3,y-3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+4,y-3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;

		// Row 4
		temp = new MapLocation(x-4,y-2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-3,y-2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-2,y-2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-1,y-2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x,y-2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+1,y-2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+2,y-2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+3,y-2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+4,y-2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;

		// Row 5
		temp = new MapLocation(x-4,y-1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-3,y-1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-2,y-1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-1,y-1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x,y-1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+1,y-1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+2,y-1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+3,y-1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+4,y-1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;

		
		// Row 6
		temp = new MapLocation(x-5,y); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-4,y); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-3,y); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-2,y); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-1,y); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+1,y); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+2,y); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+3,y); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+4,y); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+5,y); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;

		// Row 7
		temp = new MapLocation(x-4,y+1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-3,y+1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-2,y+1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-1,y+1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x,y+1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+1,y+1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+2,y+1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+3,y+1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+4,y+1); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;

		// Row 8
		temp = new MapLocation(x-4,y+2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-3,y+2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-2,y+2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-1,y+2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x,y+2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+1,y+2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+2,y+2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+3,y+2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+4,y+2); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;

		// Row 9
		temp = new MapLocation(x-4,y+3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-3,y+3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-2,y+3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-1,y+3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x,y+3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+1,y+3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+2,y+3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+3,y+3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+4,y+3); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;

		// Row 10
		temp = new MapLocation(x-3,y+4); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-2,y+4); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x-1,y+4); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x,y+4); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+1,y+4); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+2,y+4); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
		temp = new MapLocation(x+3,y+4); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;

		// Row 11
		temp = new MapLocation(x,y+5); if ( locIsOnMap(temp) ) terrainMatrix[temp.x][temp.y] = 99;
	}
	
	public void voidPad(int padAmt) {
		int ulX,ulY,brX,brY;
		if ( voids == null ) {
			if ( debugLevel >= 1 ) System.out.println("Padding the MapRender by {"+padAmt+"} using Matrix Search");
			for ( int i = 0; i < width; i++) {
				for ( int j = 0; j < height; j++) {
					if ( terrainMatrix[i][j] == 99 ) {
						MapLocation n = new MapLocation(i,j);
						if ( debugLevel >= 3 ) System.out.println("Padding "+n.toString());
						ulX = n.x-padAmt;
						ulY = n.y-padAmt;
						brX = n.x+padAmt;
						brY = n.y+padAmt;
						if ( ulX < 0 ) ulX = 0;
						if ( ulY < 0 ) ulY = 0;
						if ( brX >= width ) brX = width-1;
						if ( brY >= height ) brY = height-1;
						for (int i2 = ulX ; i2 <= brX; i2++) {
							for ( int j2 = ulY; j2 <= brY; j2++) {
								terrainMatrix[i][j] = 99;
							}
						}

					}
				}
			}
			
		}
		else {
			if ( debugLevel >= 1 ) System.out.println("Padding the MapRender by {"+padAmt+"} using Voids-List");
			for ( MapLocation n : voids ) {
				if ( debugLevel >= 3 ) System.out.println("Padding "+n.toString());
				ulX = n.x-padAmt;
				ulY = n.y-padAmt;
				brX = n.x+padAmt;
				brY = n.y+padAmt;
				if ( ulX < 0 ) ulX = 0;
				if ( ulY < 0 ) ulY = 0;
				if ( brX >= width ) brX = width-1;
				if ( brY >= height ) brY = height-1;
				for (int i = ulX ; i <= brX; i++) {
					for ( int j = ulY; j <= brY; j++) {
						terrainMatrix[i][j] = 99;
					}
				}
			}
		}
	}
	
	public void setFlag_CollectVoids(boolean in) {
		FLAG_CollectVoids = in;
	}
	
	public LinkedList<MapLocation> getVoids() {
		return voids;
	}
	
	public boolean isDirectPath(MapLocation from, MapLocation to) {
		
		int ltiF = locationToIndex(from);
		int ltiT = locationToIndex(to);
		
		//totalCount++;
		
		if ( directAdjMatrix[ltiF][ltiT] != 0) {
			//reuseCount++;
			//System.out.println("REUSED!!!!!    "+reuseCount+"/"+totalCount);
			if ( directAdjMatrix[ltiF][ltiT] == 1) return true;
			return false;
		}
		// Starting from 'from', determine if there is a simple direct path to 'to'.
		while ( true ) {
			if ( from.equals(to) ) {
				directAdjMatrix[ltiF][ltiT] = 1;
				directAdjMatrix[ltiT][ltiF] = 1;
				return true;
			}
			Direction dirS = from.directionTo(to);
			//Direction dirL = dirS.rotateLeft();
			//Direction dirR = dirS.rotateRight();
			
			MapLocation mS = from.add(dirS);
			//MapLocation mL = from.add(dirL);
			//MapLocation mR = from.add(dirR);
			
			if ( terrainMatrix[mS.x][mS.y] != 99 ) {
				from = mS;
				continue;
			}
			//else if ( terrainMatrix[mL.x][mL.y] != 99 ) {
			//	from = mL;
			//	continue;
			//}
			//else if ( terrainMatrix[mR.x][mR.y] != 99 ) {
			//	from = mR;
			//	continue;
			//}
			else break;
		}
		directAdjMatrix[ltiF][ltiT] = -1;
		directAdjMatrix[ltiT][ltiF] = -1;
		return false;
	}
	
	private int locationToIndex(MapLocation in) {
		return (in.x*height)+in.y;
	}

	private static short[][] deepCopyShortMatrix(short[][] input) {
	    if (input == null) return null;
	    short[][] result = new short[input.length][];
	    for (int r = 0; r < input.length; r++) {
	        result[r] = input[r].clone();
	    }
	    return result;
	}
	
	public MapRender clone() {
		MapRender newRend = new MapRender(rc);
		
		newRend.width = width;
		newRend.height = height;
		
		newRend.terrainMatrix = deepCopyShortMatrix(terrainMatrix);
		newRend.directAdjMatrix = deepCopyShortMatrix(directAdjMatrix);		
		if ( voids != null ) newRend.voids = (LinkedList<MapLocation>)voids.clone();
		
		return newRend;
	}
	
	/**
	 * Checks to see whether the specified location is on the map.  Checks only the map boundaries, does NOT take into account whether the location is a wall.
	 * @param in	The MapLocation to check
	 * @return
	 */
	public boolean locIsOnMap(MapLocation in) {
		if ( in.x < 0 ) return false;
		if ( in.y < 0 ) return false;
		if ( in.x >= rc.getMapWidth() ) return false;
		if ( in.y >= rc.getMapHeight() ) return false;
		return true;
	}


}
