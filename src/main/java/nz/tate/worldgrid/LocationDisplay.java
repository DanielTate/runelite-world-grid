package nz.tate.worldgrid;

import nz.tate.worldgrid.core.GridMath;
import nz.tate.worldgrid.core.GridRefFormatter;
import nz.tate.worldgrid.core.SavedLocation;

/**
 * Turns a saved location into the text shown beside it.
 *
 * <p>References are never stored, only computed, so a location saved by someone on a
 * different grid size still reads correctly here. That means this runs every time a row
 * or a pin is built, which is why it is two divisions and a string concatenation.
 */
public final class LocationDisplay
{
	/** Shown where a reference cannot be formed, matching the HUD readout. */
	public static final String NO_REFERENCE = "—";

	private LocationDisplay()
	{
	}

	public static String reference(SavedLocation location, int gridSize)
	{
		String reference = GridRefFormatter.format(
			GridMath.toCell(location.getX(), location.getY(), location.getPlane(), gridSize));
		return reference == null ? NO_REFERENCE : reference;
	}

	/** What a map pin says on hover: the name the user gave it, plus the live reference. */
	public static String tooltip(SavedLocation location, int gridSize)
	{
		return location.getLabel() + " (" + reference(location, gridSize) + ")";
	}
}
