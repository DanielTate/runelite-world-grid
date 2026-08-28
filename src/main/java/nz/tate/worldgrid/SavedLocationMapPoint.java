package nz.tate.worldgrid;

import java.awt.image.BufferedImage;
import net.runelite.api.coords.WorldPoint;

/** A pin for a saved location the user has lit up. */
public class SavedLocationMapPoint extends WorldGridMapPoint
{
	SavedLocationMapPoint(WorldPoint worldPoint, BufferedImage image)
	{
		super(worldPoint, image);
	}
}
