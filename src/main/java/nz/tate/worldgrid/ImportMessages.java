package nz.tate.worldgrid;

import nz.tate.worldgrid.core.LocationCodec;

/**
 * The wording and file names the import and export dialogs use.
 *
 * <p>Kept out of the Swing code so the counts, the caution threshold and the default
 * file names can be tested. Showing counts before acting is the point of both dialogs,
 * not decoration -- it is what makes Replace safe to offer at all.
 */
public final class ImportMessages
{
	public static final String ALL_FILE_NAME = "world-grid-locations.json";
	public static final String PINNED_FILE_NAME = "world-grid-pinned.json";

	private ImportMessages()
	{
	}

	public static String exportOptionLabel(boolean pinnedOnly, int count)
	{
		return (pinnedOnly ? "Pinned only" : "All saved") + " (" + count + ")";
	}

	/** Two exports must not silently overwrite each other in the file chooser. */
	public static String defaultExportFileName(boolean pinnedOnly)
	{
		return pinnedOnly ? PINNED_FILE_NAME : ALL_FILE_NAME;
	}

	public static String ensureJsonName(String name)
	{
		return name.toLowerCase().endsWith(".json") ? name : name + ".json";
	}

	public static String importCounts(int incoming, int existing)
	{
		return "This file has " + incoming + " location" + plural(incoming) + "."
			+ " You have " + existing + " saved.";
	}

	/**
	 * @return a caution to show alongside the counts, or null when the file is a normal
	 * size. A large list is warned about, never refused (D25).
	 */
	public static String largeImportCaution(int incoming)
	{
		if (!LocationCodec.isLargeImport(incoming))
		{
			return null;
		}
		return "Importing " + incoming + " locations may slow the panel and world map.";
	}

	private static String plural(int count)
	{
		return count == 1 ? "" : "s";
	}
}
