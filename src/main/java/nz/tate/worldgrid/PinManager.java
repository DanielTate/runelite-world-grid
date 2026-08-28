package nz.tate.worldgrid;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import nz.tate.worldgrid.core.SavedLocation;

/**
 * Puts a world map pin on every saved location the user has lit up.
 *
 * <p>The list is your library; the pinned subset is what appears on the map. That is
 * what keeps a forty-entry imported clan file from carpeting the world map.
 *
 * <p>Toggles arrive on the Swing event thread and {@link WorldMapPointManager} belongs
 * to the client, so every change is marshalled with {@link ClientThread#invoke}.
 */
@Singleton
public class PinManager
{
	private final ClientThread clientThread;
	private final WorldMapPointManager worldMapPointManager;
	private final WorldGridConfig config;
	private final LocationRepository repository;

	@Inject
	PinManager(ClientThread clientThread, WorldMapPointManager worldMapPointManager,
		WorldGridConfig config, LocationRepository repository)
	{
		this.clientThread = clientThread;
		this.worldMapPointManager = worldMapPointManager;
		this.config = config;
		this.repository = repository;

		repository.addListener(this::refresh);
	}

	public void shutDown()
	{
		// Clears the searched pin as well as the saved ones, and nobody else's.
		clientThread.invoke(() ->
			worldMapPointManager.removeIf(point -> point instanceof WorldGridMapPoint));
	}

	/**
	 * Rebuilds the saved pins from the current list. Only ever runs on a user action,
	 * never per frame, so rebuilding the handful of pinned entries is cheaper than
	 * working out which ones changed.
	 */
	public void refresh()
	{
		final List<SavedLocation> pinned = LocationEdits.pinnedOnly(repository.getLocations());
		final int gridSize = config.gridSize();

		clientThread.invoke(() ->
		{
			worldMapPointManager.removeIf(point -> point instanceof SavedLocationMapPoint);
			for (SavedLocation location : pinned)
			{
				worldMapPointManager.add(build(location, gridSize));
			}
		});
	}

	private SavedLocationMapPoint build(SavedLocation location, int gridSize)
	{
		WorldPoint worldPoint =
			new WorldPoint(location.getX(), location.getY(), location.getPlane());

		SavedLocationMapPoint point = new SavedLocationMapPoint(worldPoint, PinIcons.SAVED);
		point.setName(location.getLabel());
		point.setTooltip(LocationDisplay.tooltip(location, gridSize));
		point.setTarget(worldPoint);
		point.setJumpOnClick(true);
		// An off-screen pin clings to the edge of the map pointing towards itself, which
		// is what you want when someone reads out a place on the far side of the world.
		point.setSnapToEdge(true);
		return point;
	}
}
