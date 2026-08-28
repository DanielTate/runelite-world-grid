package nz.tate.worldgrid;

import java.awt.Rectangle;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ColorUtil;
import nz.tate.worldgrid.core.GridCell;
import nz.tate.worldgrid.core.GridMath;
import nz.tate.worldgrid.core.GridRefParseResult;
import nz.tate.worldgrid.core.GridRefParser;

/**
 * "Search grid ref" on the world map's right-click menu.
 *
 * <p>The entry is a {@link MenuAction#RUNELITE} entry, which is client-side only and
 * sends nothing to the server -- the same mechanism core RuneLite's own "Focus on" uses.
 *
 * <p>There is at most one search pin at a time: a second search moves the existing one
 * rather than adding another, so "Remove pin" is never ambiguous and a session of
 * searching does not litter the map.
 *
 * <p>Everything here runs on the client thread, both the menu callback and the chatbox
 * prompt, so nothing needs marshalling.
 */
@Slf4j
@Singleton
public class GridSearch
{
	private static final String SEARCH_OPTION = "Search";
	private static final String SEARCH_TARGET = "grid ref";

	private final Client client;
	private final ChatboxPanelManager chatboxPanelManager;
	private final WorldMapPointManager worldMapPointManager;
	private final WorldGridConfig config;

	private SearchMapPoint searchPin;

	@Inject
	GridSearch(Client client, ChatboxPanelManager chatboxPanelManager,
		WorldMapPointManager worldMapPointManager, WorldGridConfig config)
	{
		this.client = client;
		this.chatboxPanelManager = chatboxPanelManager;
		this.worldMapPointManager = worldMapPointManager;
		this.config = config;
	}

	public void shutDown()
	{
		searchPin = null;
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (!isMouseOverWorldMap())
		{
			return;
		}

		client.createMenuEntry(-1)
			.setOption(SEARCH_OPTION)
			.setTarget(ColorUtil.wrapWithColorTag(SEARCH_TARGET, JagexColors.MENU_TARGET))
			.setType(MenuAction.RUNELITE)
			.onClick(entry -> openPrompt());
	}

	private boolean isMouseOverWorldMap()
	{
		Widget mapView = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		if (mapView == null || mapView.isHidden())
		{
			return false;
		}

		Rectangle bounds = mapView.getBounds();
		Point mouse = client.getMouseCanvasPosition();
		return bounds != null && mouse != null && bounds.contains(mouse.getX(), mouse.getY());
	}

	private void openPrompt()
	{
		Consumer<String> onDone = this::search;
		chatboxPanelManager.openTextInput("Grid ref:")
			// Only the characters a reference can contain, so a malformed one is hard to
			// type in the first place.
			.addCharValidator(typed -> Character.isLetterOrDigit(typed) || typed == '+')
			.onDone(onDone)
			.build();
	}

	private void search(String typed)
	{
		int gridSize = config.gridSize();
		GridRefParseResult result = GridRefParser.parse(typed, gridSize);
		if (!result.isSuccess())
		{
			message(result.getMessage());
			return;
		}

		GridCell cell = result.getCell();
		// A reference names a square, so the pin goes to the middle of it.
		WorldPoint target = new WorldPoint(
			GridMath.centreX(cell, gridSize),
			GridMath.centreY(cell, gridSize),
			cell.getPlane());

		WorldMap worldMap = client.getWorldMap();
		if (worldMap != null)
		{
			worldMap.setWorldMapPositionTarget(target);
		}

		dropPin(typed.trim().toUpperCase(), target);
	}

	private void dropPin(String reference, WorldPoint target)
	{
		worldMapPointManager.removeIf(point -> point instanceof SearchMapPoint);

		searchPin = new SearchMapPoint(target);
		searchPin.setName(reference);
		searchPin.setTooltip(reference + " (searched)");
		searchPin.setTarget(target);
		searchPin.setJumpOnClick(true);
		searchPin.setSnapToEdge(true);
		worldMapPointManager.add(searchPin);
	}

	private void message(String text)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "World Grid: " + text, null);
	}
}
