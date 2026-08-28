package nz.tate.worldgrid.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The on-disk envelope: a schema version and the saved locations.
 *
 * <p>The version costs nothing now and turns a later format change into a migration
 * rather than a breakage. Everything ships in one release, so this shape is permanent.
 */
public final class LocationFile
{
	/** Bump only alongside a migration that can still read older files. */
	public static final int CURRENT_VERSION = 1;

	private int version;
	private List<SavedLocation> locations;

	public LocationFile()
	{
		this.version = CURRENT_VERSION;
		this.locations = new ArrayList<>();
	}

	public LocationFile(int version, List<SavedLocation> locations)
	{
		this.version = version;
		this.locations = locations;
	}

	public int getVersion()
	{
		return version;
	}

	public List<SavedLocation> getLocations()
	{
		return locations == null ? new ArrayList<>() : locations;
	}
}
