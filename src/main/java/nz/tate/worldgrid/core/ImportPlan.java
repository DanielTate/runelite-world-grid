package nz.tate.worldgrid.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Works out what an import would produce, without applying it.
 *
 * <p>Merge is purely additive: it never changes an existing entry, so it needs no
 * backup and is safe to offer as the common case. Replace discards everything, so the
 * caller must write a backup first.
 *
 * <p>Imported entries always arrive unpinned. Pinning is a statement about the viewer's
 * current session rather than a property of the place, so a shared file can never light
 * up someone else's map.
 */
public final class ImportPlan
{
	public enum Mode
	{
		MERGE,
		REPLACE
	}

	private final List<SavedLocation> result;
	private final int addedCount;
	private final int skippedCount;
	private final int discardedCount;
	private final Mode mode;

	private ImportPlan(List<SavedLocation> result, int addedCount, int skippedCount,
		int discardedCount, Mode mode)
	{
		this.result = result;
		this.addedCount = addedCount;
		this.skippedCount = skippedCount;
		this.discardedCount = discardedCount;
		this.mode = mode;
	}

	public static ImportPlan create(List<SavedLocation> existing, List<SavedLocation> incoming,
		Mode mode)
	{
		if (mode == Mode.REPLACE)
		{
			return createReplace(existing, incoming);
		}
		return createMerge(existing, incoming);
	}

	private static ImportPlan createReplace(List<SavedLocation> existing,
		List<SavedLocation> incoming)
	{
		List<SavedLocation> result = new ArrayList<>();
		for (SavedLocation location : incoming)
		{
			result.add(location.withPinned(false));
		}
		return new ImportPlan(result, incoming.size(), 0, existing.size(), Mode.REPLACE);
	}

	private static ImportPlan createMerge(List<SavedLocation> existing,
		List<SavedLocation> incoming)
	{
		List<SavedLocation> result = new ArrayList<>(existing);
		int added = 0;
		int skipped = 0;

		for (SavedLocation candidate : incoming)
		{
			if (containsPlace(result, candidate))
			{
				// The existing entry wins, name and all. Merge promised to keep yours.
				skipped++;
			}
			else
			{
				result.add(candidate.withPinned(false));
				added++;
			}
		}
		return new ImportPlan(result, added, skipped, 0, Mode.MERGE);
	}

	private static boolean containsPlace(List<SavedLocation> locations, SavedLocation candidate)
	{
		for (SavedLocation location : locations)
		{
			if (location.isSamePlaceAs(candidate))
			{
				return true;
			}
		}
		return false;
	}

	public List<SavedLocation> getResult()
	{
		return result;
	}

	public int getAddedCount()
	{
		return addedCount;
	}

	/** Incoming entries that were already present. Merge only. */
	public int getSkippedCount()
	{
		return skippedCount;
	}

	/** Existing entries that Replace would throw away. Replace only. */
	public int getDiscardedCount()
	{
		return discardedCount;
	}

	public Mode getMode()
	{
		return mode;
	}

	/**
	 * An import where everything collided otherwise looks like a silent no-op, so the
	 * outcome is always reported.
	 */
	public String describeOutcome()
	{
		if (mode == Mode.REPLACE)
		{
			return "Replaced " + discardedCount + " with " + addedCount + " imported.";
		}
		if (skippedCount == 0)
		{
			return "Imported " + addedCount + " new.";
		}
		return "Imported " + addedCount + " new, skipped " + skippedCount + " already present.";
	}
}
