package nz.tate.worldgrid.core;

/**
 * The permanently fixed origin of the grid, and the limits that depend on it.
 *
 * <p>This value can never change once the plugin has shipped. Grid references are
 * computed relative to it, so moving the anchor renames every square in the world and
 * invalidates every reference a player has saved, shared or written down.
 *
 * <p>Both coordinates are region-aligned (multiples of 64) and sit south-west of all
 * playable overworld surface, so no real location produces a negative index.
 */
public final class GridAnchor
{
	/** West edge of the grid. 1024 = region column 16. */
	public static final int ANCHOR_X = 1024;

	/** South edge of the grid. 2496 = region row 39. */
	public static final int ANCHOR_Y = 2496;

	/** Highest valid world coordinate on either axis. The space is 16384 x 16384. */
	public static final int WORLD_MAX = 16383;

	/** Grid size bounds offered by the config, per DECISIONS D2. */
	public static final int MIN_GRID_SIZE = 1;
	public static final int MAX_GRID_SIZE = 256;

	private GridAnchor()
	{
	}
}
