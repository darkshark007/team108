package team108.Path;

import battlecode.common.MapLocation;
import team108.I_Debugger;
import team108.Graph.MapRender;

public interface I_PathGenerator extends I_Debugger {
	
	public Path getPath(MapLocation from, MapLocation to);

}
