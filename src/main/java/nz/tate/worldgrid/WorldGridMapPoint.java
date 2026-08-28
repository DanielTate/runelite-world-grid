package nz.tate.worldgrid;

import java.awt.image.BufferedImage;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

/**
 * Base class for every pin this plugin puts on the world map.
 *
 * <p>It exists so shutting the plugin down can clear exactly our own pins with a single
 * {@code removeIf}, without disturbing any other plugin's. A leaked pin otherwise stays
 * on the map until the client restarts.
 */
public abstract class WorldGridMapPoint extends WorldMapPoint
{
	WorldGridMapPoint(WorldPoint worldPoint, BufferedImage image)
	{
		super(worldPoint, image);
	}
}
