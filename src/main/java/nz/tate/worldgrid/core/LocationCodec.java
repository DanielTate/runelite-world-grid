package nz.tate.worldgrid.core;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes the locations file.
 *
 * <p>Every file is validated in full before any of it is used. A file that fails
 * validation is rejected outright, so a bad import can never half-apply or wipe what the
 * user already had.
 */
public final class LocationCodec
{
	/** Above this many entries an import warns before proceeding. Not a hard cap. */
	public static final int LARGE_FILE_WARNING_THRESHOLD = 200;

	private static final int MAX_LABEL_LENGTH = 100;

	private final Gson gson;

	public LocationCodec(Gson gson)
	{
		this.gson = gson;
	}

	public String write(List<SavedLocation> locations)
	{
		return gson.toJson(new LocationFile(LocationFile.CURRENT_VERSION, locations));
	}

	/**
	 * @return the locations exactly as written in the file. Callers decide what to do
	 * with the pinned flag; the import path clears it, loading our own file keeps it.
	 */
	public LocationReadResult read(String json)
	{
		if (json == null || json.trim().isEmpty())
		{
			return LocationReadResult.failure("The file is empty.");
		}

		LocationFile file;
		try
		{
			file = gson.fromJson(json, LocationFile.class);
		}
		catch (JsonSyntaxException e)
		{
			return LocationReadResult.failure("The file is not valid JSON.");
		}

		if (file == null)
		{
			return LocationReadResult.failure("The file is empty.");
		}

		if (file.getVersion() > LocationFile.CURRENT_VERSION)
		{
			return LocationReadResult.failure(
				"This file was written by a newer version of the plugin.");
		}

		List<SavedLocation> validated = new ArrayList<>();
		int index = 0;
		for (SavedLocation location : file.getLocations())
		{
			index++;
			String problem = validate(location);
			if (problem != null)
			{
				return LocationReadResult.failure("Location " + index + " is invalid: " + problem);
			}
			validated.add(location);
		}
		return LocationReadResult.success(validated);
	}

	/**
	 * @return null when the location is usable, otherwise a reason to show the user
	 */
	private String validate(SavedLocation location)
	{
		if (location == null)
		{
			return "it is missing";
		}
		if (location.getLabel() == null || location.getLabel().trim().isEmpty())
		{
			return "it has no name";
		}
		if (location.getLabel().length() > MAX_LABEL_LENGTH)
		{
			return "its name is too long";
		}
		if (location.getX() < 0 || location.getX() > GridAnchor.WORLD_MAX)
		{
			return "its x coordinate is outside the world";
		}
		if (location.getY() < 0 || location.getY() > GridAnchor.WORLD_MAX)
		{
			return "its y coordinate is outside the world";
		}
		if (location.getPlane() < 0 || location.getPlane() > 3)
		{
			return "its plane is not between 0 and 3";
		}
		return null;
	}

	public static boolean isLargeImport(int incomingCount)
	{
		return incomingCount > LARGE_FILE_WARNING_THRESHOLD;
	}
}
