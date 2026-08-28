package nz.tate.worldgrid.core;

import java.util.Objects;

/**
 * One saved place.
 *
 * <p>Stores the raw world coordinate, never the grid reference. The reference is a
 * display layer over a location rather than the location itself, so recomputing it from
 * the viewer's own grid size means a shared file always points at the right physical
 * place whatever size either player runs.
 *
 * <p>Field names are part of the on-disk format and cannot be renamed.
 */
public final class SavedLocation
{
	private final String label;
	private final int x;
	private final int y;
	private final int plane;
	private final boolean pinned;

	public SavedLocation(String label, int x, int y, int plane, boolean pinned)
	{
		this.label = label;
		this.x = x;
		this.y = y;
		this.plane = plane;
		this.pinned = pinned;
	}

	public String getLabel()
	{
		return label;
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public int getPlane()
	{
		return plane;
	}

	public boolean isPinned()
	{
		return pinned;
	}

	public SavedLocation withPinned(boolean newPinned)
	{
		return new SavedLocation(label, x, y, plane, newPinned);
	}

	/**
	 * Two entries at the same tile are the same place regardless of what each calls it,
	 * so identity is the coordinate and the label is only a name for it.
	 */
	public boolean isSamePlaceAs(SavedLocation other)
	{
		return x == other.x && y == other.y && plane == other.plane;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof SavedLocation))
		{
			return false;
		}
		SavedLocation that = (SavedLocation) other;
		return x == that.x
			&& y == that.y
			&& plane == that.plane
			&& pinned == that.pinned
			&& Objects.equals(label, that.label);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(label, x, y, plane, pinned);
	}

	@Override
	public String toString()
	{
		return "SavedLocation(" + label + " @ " + x + "," + y + "," + plane + ", pinned=" + pinned + ")";
	}
}
