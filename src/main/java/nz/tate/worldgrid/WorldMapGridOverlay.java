package nz.tate.worldgrid;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import nz.tate.worldgrid.core.GridAnchor;
import nz.tate.worldgrid.core.GridCell;
import nz.tate.worldgrid.core.GridMath;
import nz.tate.worldgrid.core.GridRefFormatter;

/**
 * Draws the grid over the world map.
 *
 * <p>Unlike the scene overlay, labelling many squares at once is the point here -- the
 * map is where you look up someone else's reference.
 */
public class WorldMapGridOverlay extends Overlay
{
	/**
	 * Below this many pixels per square the lines sit on top of each other and the map
	 * becomes unreadable, so the grid is not drawn at all.
	 */
	private static final int MIN_PIXELS_PER_CELL = 3;

	/** Labels need more room than lines, so they appear only once squares are big enough. */
	private static final int MIN_PIXELS_FOR_LABEL = 30;

	private final Client client;
	private final WorldGridConfig config;

	@Inject
	WorldMapGridOverlay(Client client, WorldGridConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.MANUAL);
		drawAfterLayer(InterfaceID.Worldmap.MAP_CONTAINER);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.drawOnWorldMap())
		{
			return null;
		}

		WorldMap worldMap = client.getWorldMap();
		Widget mapView = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		if (worldMap == null || mapView == null || mapView.isHidden())
		{
			return null;
		}

		int gridSize = config.gridSize();
		float pixelsPerTile = worldMap.getWorldMapZoom();
		float pixelsPerCell = pixelsPerTile * gridSize;
		if (pixelsPerCell < MIN_PIXELS_PER_CELL)
		{
			return null;
		}

		Rectangle bounds = mapView.getBounds();
		Point centreTile = worldMap.getWorldMapPosition();
		if (bounds == null || centreTile == null)
		{
			return null;
		}

		Shape previousClip = graphics.getClip();
		Stroke previousStroke = graphics.getStroke();
		graphics.setClip(bounds);
		graphics.setColor(config.gridColour());
		graphics.setStroke(new BasicStroke(config.lineThickness()));

		drawGrid(graphics, bounds, centreTile, pixelsPerTile, gridSize, pixelsPerCell);

		graphics.setStroke(previousStroke);
		graphics.setClip(previousClip);
		return null;
	}

	private void drawGrid(Graphics2D graphics, Rectangle bounds, Point centreTile,
		float pixelsPerTile, int gridSize, float pixelsPerCell)
	{
		// Tiles visible either side of the centre, rounded outward so partly visible
		// squares still get their lines.
		int halfTilesAcross = (int) (bounds.width / pixelsPerTile / 2) + gridSize;
		int halfTilesDown = (int) (bounds.height / pixelsPerTile / 2) + gridSize;

		int minWorldX = centreTile.getX() - halfTilesAcross;
		int maxWorldX = centreTile.getX() + halfTilesAcross;
		int minWorldY = centreTile.getY() - halfTilesDown;
		int maxWorldY = centreTile.getY() + halfTilesDown;

		boolean drawLabels = pixelsPerCell >= MIN_PIXELS_FOR_LABEL;

		for (int worldX = GridMath.firstBoundaryAtOrAfter(minWorldX, GridAnchor.ANCHOR_X, gridSize);
			worldX <= maxWorldX; worldX += gridSize)
		{
			int screenX = toScreenX(bounds, centreTile, pixelsPerTile, worldX);
			graphics.drawLine(screenX, bounds.y, screenX, bounds.y + bounds.height);
		}

		for (int worldY = GridMath.firstBoundaryAtOrAfter(minWorldY, GridAnchor.ANCHOR_Y, gridSize);
			worldY <= maxWorldY; worldY += gridSize)
		{
			int screenY = toScreenY(bounds, centreTile, pixelsPerTile, worldY);
			graphics.drawLine(bounds.x, screenY, bounds.x + bounds.width, screenY);
		}

		if (drawLabels)
		{
			drawLabels(graphics, bounds, centreTile, pixelsPerTile, gridSize,
				minWorldX, maxWorldX, minWorldY, maxWorldY);
		}
	}

	private void drawLabels(Graphics2D graphics, Rectangle bounds, Point centreTile,
		float pixelsPerTile, int gridSize, int minWorldX, int maxWorldX, int minWorldY, int maxWorldY)
	{
		for (int worldX = GridMath.firstBoundaryAtOrAfter(minWorldX, GridAnchor.ANCHOR_X, gridSize);
			worldX <= maxWorldX; worldX += gridSize)
		{
			for (int worldY = GridMath.firstBoundaryAtOrAfter(minWorldY, GridAnchor.ANCHOR_Y, gridSize);
				worldY <= maxWorldY; worldY += gridSize)
			{
				GridCell cell = GridMath.toCell(worldX, worldY, 0, gridSize);
				String reference = GridRefFormatter.format(cell);
				if (reference == null)
				{
					continue;
				}

				// Sits just inside the square's south-west corner on screen, which is
				// the top-left going down the map.
				int screenX = toScreenX(bounds, centreTile, pixelsPerTile, worldX) + 2;
				int screenY = toScreenY(bounds, centreTile, pixelsPerTile, worldY + gridSize) + 12;
				if (bounds.contains(screenX, screenY))
				{
					graphics.drawString(reference, screenX, screenY);
				}
			}
		}
	}

	private static int toScreenX(Rectangle bounds, Point centreTile, float pixelsPerTile, int worldX)
	{
		return (int) (bounds.x + bounds.width / 2.0 + (worldX - centreTile.getX()) * pixelsPerTile);
	}

	private static int toScreenY(Rectangle bounds, Point centreTile, float pixelsPerTile, int worldY)
	{
		// Screen y grows downward while world y grows north, so the sign flips.
		return (int) (bounds.y + bounds.height / 2.0 - (worldY - centreTile.getY()) * pixelsPerTile);
	}
}
