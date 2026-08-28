package nz.tate.worldgrid;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import nz.tate.worldgrid.core.GridAnchor;
import nz.tate.worldgrid.core.GridCell;
import nz.tate.worldgrid.core.GridMath;
import nz.tate.worldgrid.core.GridRefFormatter;

/**
 * Draws the grid over the game world and labels the square the player is standing in.
 *
 * <p>Only the loaded scene is considered, never the whole world.
 */
public class SceneGridOverlay extends Overlay
{
	private static final int HALF_TILE = Perspective.LOCAL_TILE_SIZE / 2;
	private static final int SCENE_RADIUS = Constants.SCENE_SIZE / 2;

	/**
	 * Most boundary lines drawn per axis. Each line costs one projection per tile it
	 * spans, so without this a grid size of 1 would ask for over twenty thousand
	 * projections every frame. When the limit bites, the grid is drawn closer in around
	 * the player rather than being dropped entirely.
	 */
	private static final int MAX_LINES_PER_AXIS = 24;

	private static final char[] DIRECTIONS = {'N', 'S', 'E', 'W'};

	private final Client client;
	private final WorldGridPlugin plugin;
	private final WorldGridConfig config;

	@Inject
	SceneGridOverlay(Client client, WorldGridPlugin plugin, WorldGridConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.drawInScene())
		{
			return null;
		}

		GridCell cell = plugin.currentCell();
		if (cell == null)
		{
			// In an instance the coordinates do not match where the player appears to
			// be, so drawing a grid there would be actively misleading.
			return null;
		}

		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		int gridSize = config.gridSize();
		WorldPoint location = player.getWorldLocation();
		int radius = drawRadius(gridSize);

		Stroke previousStroke = graphics.getStroke();
		graphics.setColor(config.gridColour());
		graphics.setStroke(new BasicStroke(config.lineThickness()));

		int minX = location.getX() - radius;
		int maxX = location.getX() + radius;
		int minY = location.getY() - radius;
		int maxY = location.getY() + radius;

		for (int x = firstBoundaryAtOrAfter(minX, GridAnchor.ANCHOR_X, gridSize); x <= maxX; x += gridSize)
		{
			drawBoundary(graphics, x, minY, maxY, true);
		}
		for (int y = firstBoundaryAtOrAfter(minY, GridAnchor.ANCHOR_Y, gridSize); y <= maxY; y += gridSize)
		{
			drawBoundary(graphics, y, minX, maxX, false);
		}

		graphics.setStroke(previousStroke);
		drawCellLabel(graphics, cell, gridSize);
		drawCompassLabels(graphics, cell, gridSize);
		return null;
	}

	/**
	 * Labels the four edges of the current square with the compass direction they face.
	 *
	 * <p>Each label's position is worked out in world coordinates -- north is simply
	 * larger y -- and only then projected to the screen. Because the projection already
	 * accounts for where the camera is, the labels follow the world as it rotates
	 * instead of being pinned to the screen. That is the whole point of them: they tell
	 * you which way is north.
	 */
	private void drawCompassLabels(Graphics2D graphics, GridCell cell, int gridSize)
	{
		for (char direction : DIRECTIONS)
		{
			int[] point = compassLabelPoint(cell, gridSize, direction);
			drawWorldText(graphics, String.valueOf(direction), point[0], point[1]);
		}
	}

	private void drawWorldText(Graphics2D graphics, String text, int worldX, int worldY)
	{
		LocalPoint local = LocalPoint.fromWorld(client, worldX, worldY);
		if (local == null)
		{
			return;
		}

		Point canvas = Perspective.getCanvasTextLocation(client, graphics, local, text, 0);
		if (canvas != null)
		{
			OverlayUtil.renderTextLocation(graphics, canvas, text, config.gridColour());
		}
	}

	/**
	 * How far out to draw, in tiles. Normally the whole loaded scene, shrunk when a
	 * small grid size would otherwise put too many lines on screen.
	 */
	private static int drawRadius(int gridSize)
	{
		int affordable = gridSize * MAX_LINES_PER_AXIS / 2;
		return Math.min(SCENE_RADIUS, affordable);
	}

	/**
	 * Draws one boundary line. A vertical boundary runs north-south at a fixed world x;
	 * a horizontal one runs east-west at a fixed world y.
	 */
	private void drawBoundary(Graphics2D graphics, int fixed, int from, int to, boolean vertical)
	{
		Point previous = null;
		for (int along = from; along <= to; along++)
		{
			Point current = vertical
				? cornerToCanvas(fixed, along)
				: cornerToCanvas(along, fixed);

			// Either end may be off-screen or behind the camera, in which case that
			// segment simply is not drawn.
			if (previous != null && current != null)
			{
				graphics.drawLine(previous.getX(), previous.getY(), current.getX(), current.getY());
			}
			previous = current;
		}
	}

	/**
	 * Boundaries run along tile corners, but LocalPoint.fromWorld gives tile centres,
	 * so half a tile comes off each axis.
	 */
	private Point cornerToCanvas(int worldX, int worldY)
	{
		LocalPoint centre = LocalPoint.fromWorld(client, worldX, worldY);
		if (centre == null)
		{
			return null;
		}
		LocalPoint corner = new LocalPoint(centre.getX() - HALF_TILE, centre.getY() - HALF_TILE);
		return Perspective.localToCanvas(client, corner, client.getPlane());
	}

	private void drawCellLabel(Graphics2D graphics, GridCell cell, int gridSize)
	{
		String reference = GridRefFormatter.format(cell);
		if (reference == null)
		{
			return;
		}

		LocalPoint local = LocalPoint.fromWorld(client,
			GridMath.centreX(cell, gridSize), GridMath.centreY(cell, gridSize));
		if (local == null)
		{
			return;
		}

		Point canvas = Perspective.getCanvasTextLocation(client, graphics, local, reference, 0);
		if (canvas != null)
		{
			OverlayUtil.renderTextLocation(graphics, canvas, reference, config.gridColour());
		}
	}

	/** Edge midpoints of a cell, in world coordinates. Pure, so it is unit tested. */
	static int[] compassLabelPoint(GridCell cell, int gridSize, char direction)
	{
		int westX = GridMath.originX(cell, gridSize);
		int southY = GridMath.originY(cell, gridSize);
		int centreX = GridMath.centreX(cell, gridSize);
		int centreY = GridMath.centreY(cell, gridSize);

		switch (direction)
		{
			case 'N':
				return new int[]{centreX, southY + gridSize - 1};
			case 'S':
				return new int[]{centreX, southY};
			case 'E':
				return new int[]{westX + gridSize - 1, centreY};
			case 'W':
				return new int[]{westX, centreY};
			default:
				throw new IllegalArgumentException("Not a direction: " + direction);
		}
	}

	/** Lowest grid boundary at or after {@code from}. */
	static int firstBoundaryAtOrAfter(int from, int anchor, int gridSize)
	{
		int index = Math.floorDiv(from - anchor, gridSize);
		int boundary = anchor + index * gridSize;
		return boundary < from ? boundary + gridSize : boundary;
	}
}
