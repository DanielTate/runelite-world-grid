package nz.tate.worldgrid;

import net.runelite.api.coords.WorldPoint;

/**
 * The pin dropped by a grid reference search.
 *
 * <p>Its own class, separate from {@link SavedLocationMapPoint}, so the right-click menu
 * can offer "Save location" on this one and not on pins that are already saved.
 */
public class SearchMapPoint extends WorldGridMapPoint
{
	SearchMapPoint(WorldPoint worldPoint)
	{
		super(worldPoint, PinIcons.SEARCH);
	}
}
