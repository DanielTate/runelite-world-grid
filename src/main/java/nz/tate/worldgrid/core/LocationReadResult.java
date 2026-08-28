package nz.tate.worldgrid.core;

import java.util.List;

/**
 * Outcome of reading a locations file. Imported files are untrusted input, so reading
 * reports a failure rather than throwing, and never returns a partial list.
 */
public final class LocationReadResult
{
	private final List<SavedLocation> locations;
	private final String error;

	private LocationReadResult(List<SavedLocation> locations, String error)
	{
		this.locations = locations;
		this.error = error;
	}

	public static LocationReadResult success(List<SavedLocation> locations)
	{
		return new LocationReadResult(locations, null);
	}

	public static LocationReadResult failure(String error)
	{
		return new LocationReadResult(null, error);
	}

	public boolean isSuccess()
	{
		return error == null;
	}

	public List<SavedLocation> getLocations()
	{
		return locations;
	}

	public String getError()
	{
		return error;
	}
}
