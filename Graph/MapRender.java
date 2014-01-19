package team108.Graph;

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
	//LinkedList<MapLocation> voids = new LinkedList<MapLocation>();
	public short[][] terrainMatrix;
	short[][] directAdjMatrix;
	
	int totalCount = 0;
	int reuseCount = 0;

	public MapRender(RobotController in) {
		rc = in;
		width = rc.getMapWidth();
		height = rc.getMapHeight();
		terrainMatrix = new short[width][height];
		directAdjMatrix = new short[width*height][width*height];
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
		TerrainTile t;
		MapLocation m;
		for ( int i = 0; i < width; i++ ) {
			for ( int j = 0; j < height; j++ ) {
				m = new MapLocation(i,j);
				t = rc.senseTerrainTile(m);
				if ( t.equals(TerrainTile.NORMAL) ) terrainMatrix[i][j] = 1;
				else if ( t.equals(TerrainTile.VOID) ) {
					terrainMatrix[i][j] = 99;
					//voids.add(m);
				}
				else if ( t.equals(TerrainTile.ROAD) ) terrainMatrix[i][j] = 2;
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
	
	//public LinkedList<MapLocation> getVoids() {
	//	return voids;
	//}
	
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
	

}
