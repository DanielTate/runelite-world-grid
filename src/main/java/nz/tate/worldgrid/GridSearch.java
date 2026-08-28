package nz.tate.worldgrid;

import java.awt.Rectangle;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
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
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ColorUtil;
import nz.tate.worldgrid.core.GridCell;
import nz.tate.worldgrid.core.GridMath;
import nz.tate.worldgrid.core.GridRefParseResult;
import nz.tate.worldgrid.core.GridRefParser;
import nz.tate.worldgrid.core.SavedLocation;

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
 * <p>The menu callbacks and the chatbox prompt both run on the client thread. Only
 * saving crosses over: the list is edited on the Swing thread, and the confirmation
 * comes back to chat through {@link ClientThread}.
 */
@Slf4j
@Singleton
public class GridSearch
{
	private static final String SEARCH_OPTION = "Search";
	private static final String SEARCH_TARGET = "grid ref";
	private static final String SAVE_OPTION = "Save location";
	private static final String REMOVE_OPTION = "Remove pin";

	private final Client client;
	private final ChatboxPanelManager chatboxPanelManager;
	private final WorldMapPointManager worldMapPointManager;
	private final WorldGridConfig config;
	private final LocationRepository repository;
	private final ClientThread clientThread;

	private SearchMapPoint searchPin;

	@Inject
	GridSearch(Client client, ChatboxPanelManager chatboxPanelManager,
		WorldMapPointManager worldMapPointManager, WorldGridConfig config,
		LocationRepository repository, ClientThread clientThread)
	{
		this.client = client;
		this.chatboxPanelManager = chatboxPanelManager;
		this.worldMapPointManager = worldMapPointManager;
		this.config = config;
		this.repository = repository;
		this.clientThread = clientThread;
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

		if (searchPin != null)
		{
			// Offered on the map rather than only on the pin itself, so a pin that has
			// snapped to the edge of the view is still reachable.
			final SearchMapPoint pin = searchPin;
			client.createMenuEntry(-1)
				.setOption(REMOVE_OPTION)
				.setTarget(target(pin.getName()))
				.setType(MenuAction.RUNELITE)
				.onClick(entry -> removePin());

			client.createMenuEntry(-1)
				.setOption(SAVE_OPTION)
				.setTarget(target(pin.getName()))
				.setType(MenuAction.RUNELITE)
				.onClick(entry -> promptForName(pin.getWorldPoint()));
		}

		client.createMenuEntry(-1)
			.setOption(SEARCH_OPTION)
			.setTarget(target(SEARCH_TARGET))
			.setType(MenuAction.RUNELITE)
			.onClick(entry -> openPrompt());
	}

	private static String target(String text)
	{
		return ColorUtil.wrapWithColorTag(text, JagexColors.MENU_TARGET);
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

	private void removePin()
	{
		worldMapPointManager.removeIf(point -> point instanceof SearchMapPoint);
		searchPin = null;
	}

	/**
	 * Promotes a searched reference into the saved list. Without this the only route to
	 * a saved entry would be to travel to the place and press save, which defeats the
	 * point of searching for somewhere you are not.
	 */
	private void promptForName(WorldPoint point)
	{
		Consumer<String> onDone = typed -> saveSearched(typed, point);
		chatboxPanelManager.openTextInput("Name:")
			.addCharValidator(typed -> typed >= ' ' && typed <= '~')
			.onDone(onDone)
			.build();
	}

	private void saveSearched(String typed, WorldPoint point)
	{
		String label = LocationEdits.cleanLabel(typed);
		if (label == null)
		{
			message("A location needs a name.");
			return;
		}

		// Saving from a search stores the square's centre tile, which is coarser than
		// save-current-location's exact tile. Both are valid places.
		SavedLocation saved = new SavedLocation(
			label, point.getX(), point.getY(), point.getPlane(), false);

		// The repository is edited from the Swing thread; the file write is its own job.
		SwingUtilities.invokeLater(() ->
		{
			if (LocationEdits.containsKey(repository.getLocations(),
				LocationEdits.placeKey(saved)))
			{
				report("That square is already saved.");
				return;
			}

			repository.replaceAll(
				LocationEdits.withAdded(repository.getLocations(), saved), false);
			report("Saved " + label + ".");
		});
	}

	/** Chat messages belong to the client thread, and this is called from Swing. */
	private void report(String text)
	{
		clientThread.invoke(() -> message(text));
	}

	private void message(String text)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "World Grid: " + text, null);
	}
}
