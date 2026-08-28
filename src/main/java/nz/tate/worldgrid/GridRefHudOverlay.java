package nz.tate.worldgrid;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

/**
 * A movable readout of the square you are standing in.
 *
 * <p>Controlled independently of the two grid render toggles: knowing your reference is
 * the point of the plugin and does not require lines drawn over the world.
 */
public class GridRefHudOverlay extends OverlayPanel
{
	private final WorldGridPlugin plugin;
	private final WorldGridConfig config;

	@Inject
	GridRefHudOverlay(WorldGridPlugin plugin, WorldGridConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		// DETACHED is what makes the overlay draggable, and RuneLite persists where the
		// user puts it.
		setPosition(OverlayPosition.DETACHED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showRefReadout())
		{
			return null;
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Grid")
			.right(plugin.currentReference())
			.build());

		return super.render(graphics);
	}
}
