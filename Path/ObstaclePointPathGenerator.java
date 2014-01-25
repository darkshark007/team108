package team108.Path;

import java.util.LinkedList;

import team108.Graph.Graph;
import team108.Graph.MapRender;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ObstaclePointPathGenerator extends PathGenerator {

	MapLocation[] opArray = null;
	Graph gr = null;
	int size;
	
	public ObstaclePointPathGenerator(RobotController in) {
		super(in);
	}
	
	public ObstaclePointPathGenerator(RobotController in, MapRender mren) {
		super(in,mren);
	}

	public void init() {
		if ( opArray == null ) init_getObstaclePointArray();

		// Now i have all my obstacle points, create a graph and calculate their reachability.
		int[][] adjMat = new int[size][size];
		gr = new Graph(size+2);
		for ( int i = 0; i < size-1; i++ ) {
			for ( int j = i+1; j < size ; j++) {
				/* Version 1 - Generate a full graph */
				if ( mren.isDirectPath(opArray[i],opArray[j]) ) {
					if ( debugLevel >= 2 ) System.out.println(">> "+opArray[i].toString()+"  can get to  "+opArray[j].toString());
					gr.addEdge(i,j,((int)opArray[i].distanceSquaredTo(opArray[j])));
				}
				/* */

				/* Version 2 - Generate a sparse, spanning-tree-like graph
				if ( debugLevel >= 2 ) System.out.print(">> "+opArray[i].toString()+"  can get to  "+opArray[j].toString());
				boolean rej = true;
				for ( Node n : gr.nodes[j].edges ) {
					if ( gr.isEdgeBetween(i,n.id) ) {
						rej = false;
						//if ( ( gr.getWeight(i,n.id) + gr.getWeight(n.id,j) )>((int)opArray[i].distanceSquaredTo(opArray[j]))) gr.addEdge(i,j,((int)opArray[i].distanceSquaredTo(opArray[j])));
						//else { if ( debugLevel >= 2 ) System.out.print("    Rejected it."); }
						if ( debugLevel >= 2 ) System.out.print("    Rejected it.");
						break;
					}
				}
				if ( rej ) if ( mren.isDirectPath(opArray[i],opArray[j]) ) gr.addEdge(i,j,((int)opArray[i].distanceSquaredTo(opArray[j])));
				if ( debugLevel >= 2 ) System.out.println();
				/* */
				
				
				/* Version 3 - Adjacency Matrix
				// Only add the new link if there isnt already a simple path to it.
				if ( adjMat[i][j] == 0 ) {
					// If it can connect to it
					if ( mren.isDirectPath(opArray[i],opArray[j]) ) {
						gr.addEdge(i,j,((int)opArray[i].distanceSquaredTo(opArray[j])));
						
						adjMat[i][j] = 1;

						for ( Node n : gr.nodes[i].edges ) {
							adjMat[n.id][j] = 1;
							adjMat[j][n.id] = 1;
						}
						for ( Node n : gr.nodes[j].edges ) {
							adjMat[n.id][i] = 1;
							adjMat[i][n.id] = 1;
						}
					}
				}
				/* */

			}
		}
		if ( debugLevel >= 1 ) System.out.println("[>] Graph Built");

		//gr = gr.getMinimumSpanningTreeUsingPrims();
		//if ( debugLevel >= 1 ) System.out.println("[>] Graph Reduced");
		//gr.printGraph();
		
		
	}
	
	/* Version 2
	private void init_getObstaclePointArray() {
		// Construct the MapRender
		if ( mren == null ) {
			mren = new MapRender(rc);
			mren.init();
		}

		// Print all voids
		LinkedList<MapLocation> obps = new LinkedList<MapLocation>();
		LinkedList<MapLocation> voids = mren.getVoids();
		MapLocation tML;
		int width = rc.getMapWidth();
		int height = rc.getMapHeight();
		for ( MapLocation n : voids ) {
			if ( debugLevel >= 2 ) System.out.println("Void at "+n.x+","+n.y);
			//if (n.x == 0 || n.x == width-1) continue;
			//if (n.y == 0 || n.y == height-1) continue;

			// Check UL
			if ( mren.terrainMatrix[n.x-1][n.y-1] != 99 ) {
				if ( mren.terrainMatrix[n.x][n.y-1] != 99 && mren.terrainMatrix[n.x-1][n.y] != 99 ) {
					if ( debugLevel >= 2 ) System.out.println("Obstacle Point at: "+(n.x-1)+","+(n.y-1));
					tML = new MapLocation(n.x-1,n.y-1);
					if ( !obps.contains(tML) ) obps.add(tML);
				}
			}
			// Check DL
			if ( mren.terrainMatrix[n.x-1][n.y+1] != 99 ) {
				if ( mren.terrainMatrix[n.x][n.y+1] != 99 && mren.terrainMatrix[n.x-1][n.y] != 99 ) {
					if ( debugLevel >= 2 ) System.out.println("Obstacle Point at: "+(n.x-1)+","+(n.y+1));
					tML = new MapLocation(n.x-1,n.y+1);
					if ( !obps.contains(tML) ) obps.add(tML);
				}
			}
			// Check UR
			if ( mren.terrainMatrix[n.x+1][n.y-1] != 99 ) {
				if ( mren.terrainMatrix[n.x][n.y-1] != 99 && mren.terrainMatrix[n.x+1][n.y] != 99 ) {
					if ( debugLevel >= 2 ) System.out.println("Obstacle Point at: "+(n.x+1)+","+(n.y-1));
					tML = new MapLocation(n.x+1,n.y-1);
					if ( !obps.contains(tML) ) obps.add(tML);
				}
			}
			// Check DR
			if ( mren.terrainMatrix[n.x+1][n.y+1] != 99 ) {
				if ( mren.terrainMatrix[n.x][n.y+1] != 99 && mren.terrainMatrix[n.x+1][n.y] != 99 ) {
					if ( debugLevel >= 2 ) System.out.println("Obstacle Point at: "+(n.x+1)+","+(n.y+1));
					tML = new MapLocation(n.x+1,n.y+1);
					if ( !obps.contains(tML) ) obps.add(tML);
				}
			}
			
		}
		for ( int i = 1; i < rc.getMapWidth()-1; i++ ) {
			for ( int j = 1; j < rc.getMapHeight()-1; j++ ) {
				if ( mren.terrainMatrix[i][j] == 99 ) {
					
				}
			}
			
		}
		
		// Parse the Obstacle Points into an array for easier access
		size = obps.size();
		opArray = new MapLocation[size];
		int it = 0;
		while ( !obps.isEmpty() ) {
			opArray[it++] = obps.removeFirst();
		}
		
		if ( debugLevel >= 1 ) System.out.println("[>] Processed OP's, found "+opArray.length);
	}
	/* */
	
	/* OLD VERSION - Version 1 */
	private void init_getObstaclePointArray() {
		// Construct the MapRender
		if ( mren == null ) {
			mren = new MapRender(rc);
			mren.init();
		}

		// Print all voids
		LinkedList<MapLocation> obps = new LinkedList<MapLocation>();
		MapLocation tML;
		for ( int i = 1; i < rc.getMapWidth()-1; i++ ) {
			for ( int j = 1; j < rc.getMapHeight()-1; j++ ) {
				if ( mren.terrainMatrix[i][j] == 99 ) {
					if ( debugLevel >= 2 ) System.out.println("Void at "+i+","+j);
					
					// Check UL
					if ( mren.terrainMatrix[i-1][j-1] != 99 ) {
						if ( mren.terrainMatrix[i][j-1] != 99 && mren.terrainMatrix[i-1][j] != 99 ) {
							if ( debugLevel >= 2 ) System.out.println("Obstacle Point at: "+(i-1)+","+(j-1));
							tML = new MapLocation(i-1,j-1);
							if ( !obps.contains(tML) ) obps.add(tML);
						}
					}
					// Check DL
					if ( mren.terrainMatrix[i-1][j+1] != 99 ) {
						if ( mren.terrainMatrix[i][j+1] != 99 && mren.terrainMatrix[i-1][j] != 99 ) {
							if ( debugLevel >= 2 ) System.out.println("Obstacle Point at: "+(i-1)+","+(j+1));
							tML = new MapLocation(i-1,j+1);
							if ( !obps.contains(tML) ) obps.add(tML);
						}
					}
					// Check UR
					if ( mren.terrainMatrix[i+1][j-1] != 99 ) {
						if ( mren.terrainMatrix[i][j-1] != 99 && mren.terrainMatrix[i+1][j] != 99 ) {
							if ( debugLevel >= 2 ) System.out.println("Obstacle Point at: "+(i+1)+","+(j-1));
							tML = new MapLocation(i+1,j-1);
							if ( !obps.contains(tML) ) obps.add(tML);
						}
					}
					// Check DR
					if ( mren.terrainMatrix[i+1][j+1] != 99 ) {
						if ( mren.terrainMatrix[i][j+1] != 99 && mren.terrainMatrix[i+1][j] != 99 ) {
							if ( debugLevel >= 2 ) System.out.println("Obstacle Point at: "+(i+1)+","+(j+1));
							tML = new MapLocation(i+1,j+1);
							if ( !obps.contains(tML) ) obps.add(tML);
						}
					}
				}
			}
			
		}
		
		// Parse the Obstacle Points into an array for easier access
		size = obps.size();
		opArray = new MapLocation[size];
		int it = 0;
		while ( !obps.isEmpty() ) {
			opArray[it++] = obps.removeFirst();
		}
		
		if ( debugLevel >= 1 ) System.out.println("[>] Processed OP's, found "+opArray.length);
	}
	/* */
	
	public int getNumObstaclePoints() {
		if ( opArray == null ) {
			init_getObstaclePointArray();
		}
		return opArray.length;
	}
	
	public Path getPath(MapLocation from, MapLocation to) {
		// Check to make sure the graph has been created.
		if ( gr == null ) { init(); }
		
		// Clone the graph, because we dont want to modify it
		Graph tG = gr.clone();
		
		// Create the new temporary nodes in the graph
		// Compute reachability of the start point
		for ( int i = 0; i < size; i++ ) {
			if ( mren.isDirectPath(opArray[i],from) ) {
				//System.out.println(">> "+opArray[i].toString()+"  can get to  "+opArray[j].toString());
				tG.addEdge(i,size,((int)opArray[i].distanceSquaredTo(from)));
			}
		}
		// Compute reachability of the end point
		for ( int i = 0; i < size; i++ ) {
			if ( mren.isDirectPath(opArray[i],to) ) {
				//System.out.println(">> "+opArray[i].toString()+"  can get to  "+opArray[j].toString());
				tG.addEdge(i,size+1,((int)opArray[i].distanceSquaredTo(to)));
			}
		}

		// Perform Dijkstra's Algorithm on this simplified graph
		
		int[] path = tG.getShortestPathUsingDijkstras(size, size+1);
		if ( path == null ) return null;
		
		Path p = new Path();
		p.addLink(to);
		int current = size+1;
		System.out.println(to.toString());
		current = path[current];
		while ( current != size ) {
			p.addLink(opArray[current]);
			System.out.println(opArray[current].toString());
			current = path[current];
		}
		System.out.println(from.toString());
		
		
		
		
		return p;
	}

	public MapRender getMapRender() {
		if ( mren == null ) {
			mren = new MapRender(rc);
			mren.init();
		}
		return mren;
	}	

}
