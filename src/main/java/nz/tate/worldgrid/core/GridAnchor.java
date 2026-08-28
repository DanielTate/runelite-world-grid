package nz.tate.worldgrid.core;

/**
 * The permanently fixed origin of the grid, and the limits that depend on it.
 *
 * <p>This value can never change once the plugin has shipped. Grid references are
 * computed relative to it, so moving the anchor renames every square in the world and
 * invalidates every reference a player has saved, shared or written down.
 *
 * <p>The anchor sits south-west of all playable surface with room to spare. The map has
 * expanded south-west three times in a decade -- Zeah in 2016, Varlamore in 2024, and
 * Sailing's oceans in 2025 -- and each expansion moved the true corner. A margin is the
 * only defence available, because the constant cannot be corrected afterwards.
 *
 * <p>Both coordinates are divisible by 256, so grid lines coincide with region
 * boundaries at every power-of-two grid size the config offers.
 */
public final class GridAnchor
{
	/** West edge of the grid. 768 = region column 12, three regions west of the map. */
	public static final int ANCHOR_X = 768;

	/** South edge of the grid. 1792 = region row 28, four regions south of the map. */
	public static final int ANCHOR_Y = 1792;

	/** Highest valid world coordinate on either axis. The space is 16384 x 16384. */
	public static final int WORLD_MAX = 16383;

	/** Grid size bounds offered by the config, per DECISIONS D2. */
	public static final int MIN_GRID_SIZE = 1;
	public static final int MAX_GRID_SIZE = 256;

	private GridAnchor()
	{
	}
}
