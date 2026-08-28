package nz.tate.worldgrid.core;

/**
 * Converts between world coordinates and grid cells.
 *
 * <p>Every division here uses {@link Math#floorDiv}. Plain integer division truncates
 * toward zero, which folds the two squares either side of the anchor into one and
 * produces a wrong cell for anything west or south of it. Bit masking is also wrong:
 * it only works for power-of-two sizes, and DECISIONS D2 allows any size from 1 to 256.
 */
public final class GridMath
{
	private GridMath()
	{
	}

	public static GridCell toCell(int worldX, int worldY, int plane, int gridSize)
	{
		int column = Math.floorDiv(worldX - GridAnchor.ANCHOR_X, gridSize);
		int row = Math.floorDiv(worldY - GridAnchor.ANCHOR_Y, gridSize);
		return new GridCell(column, row, plane);
	}

	/** West edge of the cell, in world coordinates. */
	public static int originX(GridCell cell, int gridSize)
	{
		return GridAnchor.ANCHOR_X + cell.getColumn() * gridSize;
	}

	/** South edge of the cell, in world coordinates. */
	public static int originY(GridCell cell, int gridSize)
	{
		return GridAnchor.ANCHOR_Y + cell.getRow() * gridSize;
	}

	/**
	 * Centre of the cell, rounded down. Used when a searched reference has to become a
	 * single point, since a reference names a square rather than a tile.
	 */
	public static int centreX(GridCell cell, int gridSize)
	{
		return originX(cell, gridSize) + gridSize / 2;
	}

	public static int centreY(GridCell cell, int gridSize)
	{
		return originY(cell, gridSize) + gridSize / 2;
	}

	/**
	 * Lowest grid boundary at or after {@code from}. Used by both overlays to find
	 * where to start drawing lines across the visible area.
	 */
	public static int firstBoundaryAtOrAfter(int from, int anchor, int gridSize)
	{
		int index = Math.floorDiv(from - anchor, gridSize);
		int boundary = anchor + index * gridSize;
		return boundary < from ? boundary + gridSize : boundary;
	}

	public static boolean isValidGridSize(int gridSize)
	{
		return gridSize >= GridAnchor.MIN_GRID_SIZE && gridSize <= GridAnchor.MAX_GRID_SIZE;
	}
}
