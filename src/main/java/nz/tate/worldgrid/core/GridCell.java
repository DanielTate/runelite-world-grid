package nz.tate.worldgrid.core;

import java.util.Objects;

/**
 * One square of the grid.
 *
 * <p>Column counts east from the anchor, row counts north from it, both zero-based.
 * Plane is the game's height level, 0 at ground.
 */
public final class GridCell
{
	private final int column;
	private final int row;
	private final int plane;

	public GridCell(int column, int row, int plane)
	{
		this.column = column;
		this.row = row;
		this.plane = plane;
	}

	public int getColumn()
	{
		return column;
	}

	public int getRow()
	{
		return row;
	}

	public int getPlane()
	{
		return plane;
	}

	/**
	 * A cell is outside the grid if either index is negative. That only happens for
	 * coordinates west or south of the anchor, which no playable surface should reach.
	 */
	public boolean isNegative()
	{
		return column < 0 || row < 0;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof GridCell))
		{
			return false;
		}
		GridCell that = (GridCell) other;
		return column == that.column && row == that.row && plane == that.plane;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(column, row, plane);
	}

	@Override
	public String toString()
	{
		return "GridCell(column=" + column + ", row=" + row + ", plane=" + plane + ")";
	}
}
