package team108.Orders;

import java.util.Stack;

import team108.Graph.MapRender;
import team108.Path.Path;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class NoiseTower_DepthFirstPathGenerator extends NoiseTower {

	
	int arraySize = width*height;
	int[] pathMap = new int[arraySize];
	private double validPaths = 0.0;
	private double totalPaths = 0.0;

	public NoiseTower_DepthFirstPathGenerator(RobotController in) { 
		super(in); 
		pathCenter = locationToIndex(myLoc);
	}

	
	@Override
	public void executeOrders() throws GameActionException {
		
		if ( !pathReady ) {
			if ( debugLevel >= 1 ) System.out.println("Path isnt ready yet (NUll)");
			return;
		}
		
		setUpPath();
		
		// Use the next location
		MapLocation targetLoc = indexToLoc(pathLoc);
		
		// If possible, try and shoot one space past that location (Pushing the cows in the proper direction).
		targetLoc = targetLoc.add(targetLoc.directionTo(myLoc).opposite());
		targetLoc = targetLoc.add(targetLoc.directionTo(indexToLoc(pathMap[pathLoc])).opposite());
		
		// If that is too far away, just shoot at the point.
		if ( targetLoc.distanceSquaredTo(myLoc) > 300.0 ) targetLoc = indexToLoc(pathLoc);
		

		if ( debugLevel >= 3 ) System.out.println("EO: "+Clock.getRoundNum()+", "+Clock.getBytecodeNum());
		
		
		
		if ( debugLevel >= 2 ) System.out.println("[OO]  M: "+myLoc.toString()+"\tT: "+targetLoc.toString()+"\tD: "+myLoc.distanceSquaredTo(targetLoc));
		rc.attackSquare(targetLoc);		
	}
	
	
	int pathCounter = 0;
	int pathLoc = -2;
	int pathCenter;
	private void setUpPath() {
		if ( pathLoc == -2 ) {
			// Set up the FIRST path
			pathCounter = 0;
			while ( !locIsOnMap(Perimeter[pathCounter]) || pathMap[locationToIndex(Perimeter[pathCounter])] == -1 ) {
				pathCounter = (pathCounter + 1) % Perimeter.length;
			}
			pathLoc = locationToIndex(Perimeter[pathCounter]);
			return;
		}	
		if ( pathLoc == -1 ) {
			// Get and set up the NEXT path
			pathCounter = ( pathCounter + 7 ) % Perimeter.length;
			//pathCounter = ( pathCounter + ((int)(13.0/96.0*validPaths)) ) % Perimeter.length;
			//pathCounter = ( pathCounter + 3 ) % Perimeter.length;
			while ( !locIsOnMap(Perimeter[pathCounter]) || pathMap[locationToIndex(Perimeter[pathCounter])] == -1 ) {
				pathCounter = (pathCounter + 1) % Perimeter.length;
			}
			pathLoc = locationToIndex(Perimeter[pathCounter]);
			return;
		}
		
		// Otherwise, bump the path up by one spot.
		pathLoc = pathMap[pathLoc];
		if ( pathLoc == pathCenter ) {
			pathLoc = -1;
			setUpPath();
		}
	}


	private void calculatePath(MapLocation start) {
		
		if ( rc.senseTerrainTile(start).equals(TerrainTile.VOID) ) return;
		
		Stack<MapLocation> queue = new Stack<MapLocation>();
		boolean[] visited = new boolean[width*height];
		int[] pathTrace = new int[width*height];
		
		queue.push(start);
		
		MapLocation current;
		int index;
		int i1,i2,i3,i4,i5,i6,i7,i8;
		MapLocation m1,m2,m3,m4,m5,m6,m7,m8;
		Direction dirS,dirL,dirR;
		while ( !queue.isEmpty() ) {
			current = queue.pop();
			index = locationToIndex(current);
			visited[index] = true;

			dirS = current.directionTo(myLoc);
			m8 = current.add(dirS);
			dirL = dirS.rotateLeft();
			m7 = current.add(dirL);
			dirR = dirS.rotateRight();
			m6 = current.add(dirR);
			dirL = dirL.rotateLeft();
			m5 = current.add(dirL);
			dirR = dirR.rotateRight();
			m4 = current.add(dirR);
			dirL = dirL.rotateLeft();
			m3 = current.add(dirL);
			dirR = dirR.rotateRight();
			m2 = current.add(dirR);
			dirS = dirS.opposite();
			m1 = current.add(dirS);
			
			
			// Try UL
			if ( locIsOnMap(m1) ) {
				i1 = locationToIndex(m1);
				if ( !visited[i1] ) {
					if ( !rc.senseTerrainTile(m1).equals(TerrainTile.VOID) ) {
						queue.push(m1);
						pathTrace[i1] = index;
						if ( m1.equals(myLoc) ) break;
					}
					visited[i1] = true;
				}
			}

		
		
			// Try L
			if ( locIsOnMap(m2) ) {
				i2 = locationToIndex(m2);
				if ( !visited[i2] ) {
					if ( !rc.senseTerrainTile(m2).equals(TerrainTile.VOID) ) {
						queue.push(m2);
						pathTrace[i2] = index;
						if ( m2.equals(myLoc) ) break;
					}
					visited[i2] = true;
				}
			}

		
			// Try BL
			if ( locIsOnMap(m3) ) {
				i3 = locationToIndex(m3);
				if ( !visited[i3] ) {
					if ( !rc.senseTerrainTile(m3).equals(TerrainTile.VOID) ) {
						queue.push(m3);
						pathTrace[i3] = index;
						if ( m3.equals(myLoc) ) break;
					}
					visited[i3] = true;
				}
			}

		
			// Try B
			if ( locIsOnMap(m4) ) {
				i4 = locationToIndex(m4);
				if ( !visited[i4] ) {
					if ( !rc.senseTerrainTile(m4).equals(TerrainTile.VOID) ) {
						queue.push(m4);
						pathTrace[i4] = index;
						if ( m4.equals(myLoc) ) break;
					}
					visited[i4] = true;
				}
			}
			
			
			// Try BR
			if ( locIsOnMap(m5) ) {
				i5 = locationToIndex(m5);
				if ( !visited[i5] ) {
					if ( !rc.senseTerrainTile(m5).equals(TerrainTile.VOID) ) {
						queue.push(m5);
						pathTrace[i5] = index;
						if ( m5.equals(myLoc) ) break;
					}
					visited[i5] = true;
				}
			}
			
			
			// Try R
			if ( locIsOnMap(m6) ) {
				i6 = locationToIndex(m6);
				if ( !visited[i6] ) {
					if ( !rc.senseTerrainTile(m6).equals(TerrainTile.VOID) ) {
						queue.push(m6);
						pathTrace[i6] = index;
						if ( m6.equals(myLoc) ) break;
					}
					visited[i6] = true;
				}
			}
			
			
			// Try UR
			if ( locIsOnMap(m7) ) {
				i7 = locationToIndex(m7);
				if ( !visited[i7] ) {
					if ( !rc.senseTerrainTile(m7).equals(TerrainTile.VOID) ) {
						queue.push(m7);
						pathTrace[i7] = index;
						if ( m7.equals(myLoc) ) break;
					}
					visited[i7] = true;
				}
			}
			
			
			// Try U
			if ( locIsOnMap(m8) ) {
				i8 = locationToIndex(m8);
				if ( !visited[i8] ) {
					if ( !rc.senseTerrainTile(m8).equals(TerrainTile.VOID) ) {
						queue.push(m8);
						pathTrace[i8] = index;
						if ( m8.equals(myLoc) ) break;
					}
					visited[i8] = true;
				}
			}
		}
		if ( queue.isEmpty() ) return;
		
		// Build the path
		int curInt = locationToIndex(myLoc);
		int fin = locationToIndex(start);
		while ( curInt != fin ) {
			//System.out.println("Build>>  "+current.toString());
			
			pathMap[pathTrace[curInt]] = curInt;
			curInt = pathTrace[curInt];
		}
	}
	
	int sTurn;
	MapLocation[] Perimeter = null;
	@Override
	public void processData() {
		sTurn = Clock.getRoundNum();
		if ( Perimeter == null ) {
			Perimeter = new MapLocation[96];
			
			Perimeter[0] = new MapLocation(myLoc.x-17,myLoc.y+3);
			Perimeter[1] = new MapLocation(myLoc.x-17,myLoc.y+2);
			Perimeter[2] = new MapLocation(myLoc.x-17,myLoc.y+1);
			Perimeter[3] = new MapLocation(myLoc.x-17,myLoc.y-0);
			Perimeter[4] = new MapLocation(myLoc.x-17,myLoc.y-1);
			Perimeter[5] = new MapLocation(myLoc.x-17,myLoc.y-2);
			Perimeter[6] = new MapLocation(myLoc.x-17,myLoc.y-3);
			Perimeter[7] = new MapLocation(myLoc.x-16,myLoc.y-4);
			Perimeter[8] = new MapLocation(myLoc.x-16,myLoc.y-5);
			Perimeter[9] = new MapLocation(myLoc.x-16,myLoc.y-6);
			Perimeter[10] = new MapLocation(myLoc.x-15,myLoc.y-7);
			Perimeter[11] = new MapLocation(myLoc.x-15,myLoc.y-8);
			Perimeter[12] = new MapLocation(myLoc.x-14,myLoc.y-9);
			Perimeter[13] = new MapLocation(myLoc.x-14,myLoc.y-10);
			Perimeter[14] = new MapLocation(myLoc.x-13,myLoc.y-11);
			Perimeter[15] = new MapLocation(myLoc.x-12,myLoc.y-12);
			Perimeter[16] = new MapLocation(myLoc.x-11,myLoc.y-13);
			Perimeter[17] = new MapLocation(myLoc.x-10,myLoc.y-14);
			Perimeter[18] = new MapLocation(myLoc.x-9,myLoc.y-14);
			Perimeter[19] = new MapLocation(myLoc.x-8,myLoc.y-15);
			Perimeter[20] = new MapLocation(myLoc.x-7,myLoc.y-15);
			Perimeter[21] = new MapLocation(myLoc.x-6,myLoc.y-16);
			Perimeter[22] = new MapLocation(myLoc.x-5,myLoc.y-16);
			Perimeter[23] = new MapLocation(myLoc.x-4,myLoc.y-16);
			Perimeter[24] = new MapLocation(myLoc.x-3,myLoc.y-17);
			Perimeter[25] = new MapLocation(myLoc.x-2,myLoc.y-17);
			Perimeter[26] = new MapLocation(myLoc.x-1,myLoc.y-17);
			Perimeter[27] = new MapLocation(myLoc.x,myLoc.y-17);
			Perimeter[28] = new MapLocation(myLoc.x+1,myLoc.y-17);
			Perimeter[29] = new MapLocation(myLoc.x+2,myLoc.y-17);
			Perimeter[30] = new MapLocation(myLoc.x+3,myLoc.y-17);
			Perimeter[31] = new MapLocation(myLoc.x+4,myLoc.y-16);
			Perimeter[32] = new MapLocation(myLoc.x+5,myLoc.y-16);
			Perimeter[33] = new MapLocation(myLoc.x+6,myLoc.y-16);
			Perimeter[34] = new MapLocation(myLoc.x+7,myLoc.y-15);
			Perimeter[35] = new MapLocation(myLoc.x+8,myLoc.y-15);
			Perimeter[36] = new MapLocation(myLoc.x+9,myLoc.y-14);
			Perimeter[37] = new MapLocation(myLoc.x+10,myLoc.y-14);
			Perimeter[38] = new MapLocation(myLoc.x+11,myLoc.y-13);
			Perimeter[39] = new MapLocation(myLoc.x+12,myLoc.y-12);
			Perimeter[40] = new MapLocation(myLoc.x+13,myLoc.y-11);
			Perimeter[41] = new MapLocation(myLoc.x+14,myLoc.y-10);
			Perimeter[42] = new MapLocation(myLoc.x+14,myLoc.y-9);
			Perimeter[43] = new MapLocation(myLoc.x+15,myLoc.y-8);
			Perimeter[44] = new MapLocation(myLoc.x+15,myLoc.y-7);
			Perimeter[45] = new MapLocation(myLoc.x+16,myLoc.y-6);
			Perimeter[46] = new MapLocation(myLoc.x+16,myLoc.y-5);
			Perimeter[47] = new MapLocation(myLoc.x+16,myLoc.y-4);
			Perimeter[48] = new MapLocation(myLoc.x+17,myLoc.y-3);
			Perimeter[49] = new MapLocation(myLoc.x+17,myLoc.y-2);
			Perimeter[50] = new MapLocation(myLoc.x+17,myLoc.y-1);
			Perimeter[51] = new MapLocation(myLoc.x+17,myLoc.y);
			Perimeter[52] = new MapLocation(myLoc.x+17,myLoc.y+1);
			Perimeter[53] = new MapLocation(myLoc.x+17,myLoc.y+2);
			Perimeter[54] = new MapLocation(myLoc.x+17,myLoc.y+3);
			Perimeter[55] = new MapLocation(myLoc.x+16,myLoc.y+4);
			Perimeter[56] = new MapLocation(myLoc.x+16,myLoc.y+5);
			Perimeter[57] = new MapLocation(myLoc.x+16,myLoc.y+6);
			Perimeter[58] = new MapLocation(myLoc.x+15,myLoc.y+7);
			Perimeter[59] = new MapLocation(myLoc.x+15,myLoc.y+8);
			Perimeter[60] = new MapLocation(myLoc.x+14,myLoc.y+9);
			Perimeter[61] = new MapLocation(myLoc.x+14,myLoc.y+10);
			Perimeter[62] = new MapLocation(myLoc.x+13,myLoc.y+11);
			Perimeter[63] = new MapLocation(myLoc.x+12,myLoc.y+12);
			Perimeter[64] = new MapLocation(myLoc.x+11,myLoc.y+13);
			Perimeter[65] = new MapLocation(myLoc.x+10,myLoc.y+14);
			Perimeter[66] = new MapLocation(myLoc.x+9,myLoc.y+14);
			Perimeter[67] = new MapLocation(myLoc.x+8,myLoc.y+15);
			Perimeter[68] = new MapLocation(myLoc.x+7,myLoc.y+15);
			Perimeter[69] = new MapLocation(myLoc.x+6,myLoc.y+16);
			Perimeter[70] = new MapLocation(myLoc.x+5,myLoc.y+16);
			Perimeter[71] = new MapLocation(myLoc.x+4,myLoc.y+16);
			Perimeter[72] = new MapLocation(myLoc.x+3,myLoc.y+17);
			Perimeter[73] = new MapLocation(myLoc.x+2,myLoc.y+17);
			Perimeter[74] = new MapLocation(myLoc.x+1,myLoc.y+17);
			Perimeter[75] = new MapLocation(myLoc.x,myLoc.y+17);
			Perimeter[76] = new MapLocation(myLoc.x-1,myLoc.y+17);
			Perimeter[77] = new MapLocation(myLoc.x-2,myLoc.y+17);
			Perimeter[78] = new MapLocation(myLoc.x-3,myLoc.y+17);
			Perimeter[79] = new MapLocation(myLoc.x-4,myLoc.y+16);
			Perimeter[80] = new MapLocation(myLoc.x-5,myLoc.y+16);
			Perimeter[81] = new MapLocation(myLoc.x-6,myLoc.y+16);
			Perimeter[82] = new MapLocation(myLoc.x-7,myLoc.y+15);
			Perimeter[83] = new MapLocation(myLoc.x-8,myLoc.y+15);
			Perimeter[84] = new MapLocation(myLoc.x-9,myLoc.y+14);
			Perimeter[85] = new MapLocation(myLoc.x-10,myLoc.y+14);
			Perimeter[86] = new MapLocation(myLoc.x-11,myLoc.y+13);
			Perimeter[87] = new MapLocation(myLoc.x-12,myLoc.y+12);
			Perimeter[88] = new MapLocation(myLoc.x-13,myLoc.y+11);
			Perimeter[89] = new MapLocation(myLoc.x-14,myLoc.y+10);
			Perimeter[90] = new MapLocation(myLoc.x-14,myLoc.y+9);
			Perimeter[91] = new MapLocation(myLoc.x-15,myLoc.y+8);
			Perimeter[92] = new MapLocation(myLoc.x-15,myLoc.y+7);
			Perimeter[93] = new MapLocation(myLoc.x-16,myLoc.y+6);
			Perimeter[94] = new MapLocation(myLoc.x-16,myLoc.y+5);
			Perimeter[95] = new MapLocation(myLoc.x-16,myLoc.y+4);
			
			for (int i = 0; i < Perimeter.length; i++) {
				MapLocation n = Perimeter[i];
				//if ( locIsOnMap(n) ) pathMap[locationToIndex(n)] = -1;
				while ( (!locIsOnMap(n) || rc.senseTerrainTile(n).equals(TerrainTile.VOID) ) && !Perimeter[i].equals(myLoc) ) {
					n = Perimeter[i] = Perimeter[i].add(Perimeter[i].directionTo(myLoc));
				}
				pathMap[locationToIndex(n)] = -1;
			}
			
			calcPathCounter = 0;
			validPaths = Perimeter.length;
			totalPaths = Perimeter.length;
			
		}
		

		
		boolean result;
		while (calcPathCounter < Perimeter.length) {
			if ( sTurn < Clock.getRoundNum() ) {
				if ( debugLevel >= 3 ) System.out.println("<< Returning");
				return;
			}
			if ( debugLevel >= 2 ) System.out.println("PD: "+Clock.getRoundNum()+", "+Clock.getBytecodeNum());
			int bcs = (10000*Clock.getRoundNum())+Clock.getBytecodeNum();
			if ( debugLevel >= 2 ) System.out.println("PROCESSING: "+calcPathCounter);
			result = processData_calculatePath(Perimeter[calcPathCounter]);
			if ( !result ) calcPathCounter++;
			int bcf = (10000*Clock.getRoundNum())+Clock.getBytecodeNum();
			if ( debugLevel >= 2 ) System.out.println("PD_Fin:  "+bcs+" -> "+bcf+"  == "+(bcf-bcs));
			if ( debugLevel >= 1 ) if ( calcPathCounter == Perimeter.length ) System.out.println("Path Calculations Completed.");
		}
		
	}
	
	boolean pd_cp = false;
	Stack<MapLocation> queue;
	MapLocation current;
	MapLocation currentCP = null;
	boolean[] visited;
	int[] pathTrace;
	int index;
	int calcPathCounter; 
	boolean pathReady = false;
	private boolean processData_calculatePath(MapLocation start) {
		int i1,i2,i3,i4,i5,i6,i7,i8;
		MapLocation m1,m2,m3,m4,m5,m6,m7,m8;
		Direction dirS,dirL,dirR;			
		if ( pd_cp == false || !currentCP.equals(start) ) {
			if ( start.equals(myLoc) || start.isAdjacentTo(myLoc) || rc.senseTerrainTile(start).equals(TerrainTile.VOID) ) {
				if ( start.equals(myLoc) || start.isAdjacentTo(myLoc) ) totalPaths--;
				validPaths--;
				if ( debugLevel >= 3 ) System.out.println("FR1: ");
				return pd_cp;
			}
			currentCP = start;
			pd_cp = true;
			queue = new Stack<MapLocation>();
			visited = new boolean[width*height];
			pathTrace = new int[width*height];
			
			
			queue.push(start);
			
		}
		while ( !queue.isEmpty() ) {
			current = queue.pop();
			
			if ( ( pathMap[locationToIndex(current)] > 0 ) || ( current.equals(myLoc) ) ) {
				break;
			}


			index = locationToIndex(current);
			visited[index] = true;

			dirS = current.directionTo(myLoc);
			m8 = current.add(dirS);
			dirL = dirS.rotateLeft();
			m7 = current.add(dirL);
			dirR = dirS.rotateRight();
			m6 = current.add(dirR);
			dirL = dirL.rotateLeft();
			m5 = current.add(dirL);
			dirR = dirR.rotateRight();
			m4 = current.add(dirR);
			dirL = dirL.rotateLeft();
			m3 = current.add(dirL);
			dirR = dirR.rotateRight();
			m2 = current.add(dirR);
			dirS = dirS.opposite();
			m1 = current.add(dirS);
			
			
			/*
			// Try UL
			if ( locIsOnMap(m1) && m1.distanceSquaredTo(myLoc) <= 300 ) {
				i1 = locationToIndex(m1);
				if ( !visited[i1] ) {
					if ( !rc.senseTerrainTile(m1).equals(TerrainTile.VOID) ) {
						queue.push(m1);
						pathTrace[i1] = index;
					}
					visited[i1] = true;
				}
			}

		
		
			// Try L
			if ( locIsOnMap(m2) && m2.distanceSquaredTo(myLoc) <= 300 ) {
				i2 = locationToIndex(m2);
				if ( !visited[i2] ) {
					if ( !rc.senseTerrainTile(m2).equals(TerrainTile.VOID) ) {
						queue.push(m2);
						pathTrace[i2] = index;
					}
					visited[i2] = true;
				}
			}

		
			// Try BL
			if ( locIsOnMap(m3) && m3.distanceSquaredTo(myLoc) <= 300 ) {
				i3 = locationToIndex(m3);
				if ( !visited[i3] ) {
					if ( !rc.senseTerrainTile(m3).equals(TerrainTile.VOID) ) {
						queue.push(m3);
						pathTrace[i3] = index;
					}
					visited[i3] = true;
				}
			}

		
			// Try B
			if ( locIsOnMap(m4) && m4.distanceSquaredTo(myLoc) <= 300 ) {
				i4 = locationToIndex(m4);
				if ( !visited[i4] ) {
					if ( !rc.senseTerrainTile(m4).equals(TerrainTile.VOID) ) {
						if ( !queue.isEmpty() ) queue.pop();
						queue.push(m4);
						pathTrace[i4] = index;
					}
					visited[i4] = true;
				}
			}
			
			
			// Try BR
			if ( locIsOnMap(m5) && m5.distanceSquaredTo(myLoc) <= 300 ) {
				i5 = locationToIndex(m5);
				if ( !visited[i5] ) {
					if ( !rc.senseTerrainTile(m5).equals(TerrainTile.VOID) ) {
						if ( !queue.isEmpty() ) queue.pop();
						queue.push(m5);
						pathTrace[i5] = index;
					}
					visited[i5] = true;
				}
			}
			/* */
			
			
			// Try R
			if ( locIsOnMap(m6) && m6.distanceSquaredTo(myLoc) <= 300 ) {
				i6 = locationToIndex(m6);
				if ( !visited[i6] ) {
					if ( !rc.senseTerrainTile(m6).equals(TerrainTile.VOID) ) {
						if ( !queue.isEmpty() ) queue.pop();
						queue.push(m6);
						pathTrace[i6] = index;
					}
					visited[i6] = true;
				}
			}
			
			
			// Try UR
			if ( locIsOnMap(m7) && m7.distanceSquaredTo(myLoc) <= 300 ) {
				i7 = locationToIndex(m7);
				if ( !visited[i7] ) {
					if ( !rc.senseTerrainTile(m7).equals(TerrainTile.VOID) ) {
						if ( !queue.isEmpty() ) queue.pop();
						queue.push(m7);
						pathTrace[i7] = index;
					}
					visited[i7] = true;
				}
			}
			
			
			// Try U
			if ( locIsOnMap(m8) && m8.distanceSquaredTo(myLoc) <= 300 ) {
				i8 = locationToIndex(m8);
				if ( !visited[i8] ) {
					if ( !rc.senseTerrainTile(m8).equals(TerrainTile.VOID) ) {
						if ( !queue.isEmpty() ) queue.pop();
						queue.push(m8);
						pathTrace[i8] = index;
					}
					visited[i8] = true;
				}
			}
			
			
			if ( sTurn < Clock.getRoundNum() ) return pd_cp;
		}
		if ( !(( ( pathMap[locationToIndex(current)] > 0 ) || ( current.equals(myLoc) ) )) ) {
			pd_cp = false;
			validPaths--;
			if ( debugLevel >= 3 ) System.out.println("FR2: ");
			return pd_cp;
		}
		
		// Build the path
		if ( debugLevel >= 2 ) System.out.println("Valid Path: "+start);
		int curInt = locationToIndex(current);
		int fin = locationToIndex(start);
		while ( curInt != fin ) {
			//System.out.println("Build>>  "+current.toString());
			
			if ( debugLevel >= 3 ) System.out.println(indexToLoc(pathTrace[curInt])+" -->> "+indexToLoc(curInt));
			
			pathMap[pathTrace[curInt]] = curInt;
			curInt = pathTrace[curInt];
		}
		
		// Mark the path as Ready
		pathReady = true;
		pd_cp = false;
		return pd_cp;
	}
	
	public double getPathDensity() {
		if (Perimeter == null ) return 1;
		else return (validPaths)/(totalPaths);
	}

	
	private int locationToIndex(MapLocation in) {
		return (in.x*height)+in.y;
	}
	private MapLocation indexToLoc(int i) {
		return new MapLocation(i/height,i%height);
	}


}
