package nz.tate.worldgrid;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import nz.tate.worldgrid.core.SavedLocation;

/**
 * Every edit the panel can make to the saved list, as plain list-in list-out functions.
 *
 * <p>Swing and the client cannot be unit tested, so the decisions live here instead of
 * inside a button handler. Nothing in this class imports RuneLite or touches disk.
 *
 * <p>Rows are addressed by their coordinate rather than by object identity, because a
 * pin toggle replaces the entry object while still meaning the same place.
 */
public final class LocationEdits
{
	/** Longest label the codec will accept back off disk. */
	public static final int MAX_LABEL_LENGTH = 100;

	private static final DateTimeFormatter BACKUP_STAMP =
		DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	private LocationEdits()
	{
	}

	/**
	 * Tidies a name typed by the user.
	 *
	 * @return the name to store, or null when nothing usable was typed
	 */
	public static String cleanLabel(String typed)
	{
		if (typed == null)
		{
			return null;
		}

		String trimmed = typed.trim();
		if (trimmed.isEmpty())
		{
			return null;
		}
		if (trimmed.length() > MAX_LABEL_LENGTH)
		{
			// Truncating keeps the entry; rejecting it would throw the location away.
			return trimmed.substring(0, MAX_LABEL_LENGTH);
		}
		return trimmed;
	}

	/** Stable identity of a place: the coordinate, never the label. */
	public static String placeKey(SavedLocation location)
	{
		return location.getX() + "," + location.getY() + "," + location.getPlane();
	}

	public static List<SavedLocation> withAdded(List<SavedLocation> locations, SavedLocation added)
	{
		List<SavedLocation> result = new ArrayList<>(locations);
		result.add(added);
		return Collections.unmodifiableList(result);
	}

	public static List<SavedLocation> withoutKeys(List<SavedLocation> locations, Set<String> keys)
	{
		List<SavedLocation> result = new ArrayList<>();
		for (SavedLocation location : locations)
		{
			if (!keys.contains(placeKey(location)))
			{
				result.add(location);
			}
		}
		return Collections.unmodifiableList(result);
	}

	public static List<SavedLocation> withPinnedKey(List<SavedLocation> locations, String key,
		boolean pinned)
	{
		List<SavedLocation> result = new ArrayList<>();
		for (SavedLocation location : locations)
		{
			if (placeKey(location).equals(key))
			{
				result.add(location.withPinned(pinned));
			}
			else
			{
				result.add(location);
			}
		}
		return Collections.unmodifiableList(result);
	}

	public static List<SavedLocation> pinnedOnly(List<SavedLocation> locations)
	{
		List<SavedLocation> result = new ArrayList<>();
		for (SavedLocation location : locations)
		{
			if (location.isPinned())
			{
				result.add(location);
			}
		}
		return Collections.unmodifiableList(result);
	}

	/** What an export writes: the whole list, or only the pinned subset. */
	public static List<SavedLocation> forExport(List<SavedLocation> locations, boolean pinnedOnly)
	{
		if (pinnedOnly)
		{
			return pinnedOnly(locations);
		}
		return Collections.unmodifiableList(new ArrayList<>(locations));
	}

	public static int countPinned(List<SavedLocation> locations)
	{
		return pinnedOnly(locations).size();
	}

	public static boolean containsKey(List<SavedLocation> locations, String key)
	{
		for (SavedLocation location : locations)
		{
			if (placeKey(location).equals(key))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Name of the safety copy written before anything destructive. The timestamp keeps
	 * successive backups from overwriting each other, so recovery works the same however
	 * the data was lost.
	 */
	public static String backupFileName(Instant when, ZoneId zone)
	{
		return "locations.backup-" + BACKUP_STAMP.format(when.atZone(zone)) + ".json";
	}
}
