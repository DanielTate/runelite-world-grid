package nz.tate.worldgrid;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import nz.tate.worldgrid.core.GridCell;
import nz.tate.worldgrid.core.GridMath;
import nz.tate.worldgrid.core.GridRefFormatter;

@Slf4j
@PluginDescriptor(
	name = "World Grid",
	description = "A labelled grid over the world so players can name an exact location"
		+ " to each other",
	tags = {"grid", "coordinates", "location", "map", "navigation"}
)
public class WorldGridPlugin extends Plugin
{
	/**
	 * Shown instead of a reference inside an instance. Instanced regions report
	 * coordinates that do not correspond to where you appear to be, so any reference
	 * computed there would be confidently wrong.
	 */
	public static final String NO_REFERENCE = "—";

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WorldGridConfig config;

	@Inject
	private GridRefHudOverlay hudOverlay;

	@Provides
	WorldGridConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WorldGridConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(hudOverlay);
		log.debug("World Grid started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(hudOverlay);
		log.debug("World Grid stopped");
	}

	/**
	 * @return the cell the player is standing in, or null in an instance or before the
	 * player has loaded
	 */
	public GridCell currentCell()
	{
		if (client.isInInstancedRegion())
		{
			return null;
		}

		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		WorldPoint location = player.getWorldLocation();
		if (location == null)
		{
			return null;
		}
		return GridMath.toCell(location.getX(), location.getY(), location.getPlane(),
			config.gridSize());
	}

	/**
	 * @return the reference to display, never null -- an em dash stands in wherever a
	 * real reference cannot be computed
	 */
	public String currentReference()
	{
		GridCell cell = currentCell();
		if (cell == null)
		{
			return NO_REFERENCE;
		}

		String reference = GridRefFormatter.format(cell);
		return reference == null ? NO_REFERENCE : reference;
	}
}
