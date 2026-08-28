package nz.tate.worldgrid.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ImportPlanTest
{
	private static SavedLocation at(String label, int x, int y)
	{
		return new SavedLocation(label, x, y, 0, false);
	}

	@Test
	public void mergeAddsNewEntries()
	{
		List<SavedLocation> existing = Arrays.asList(at("mine", 3000, 3000));
		List<SavedLocation> incoming = Arrays.asList(at("theirs", 3100, 3100));

		ImportPlan plan = ImportPlan.create(existing, incoming, ImportPlan.Mode.MERGE);

		assertEquals(2, plan.getResult().size());
		assertEquals(1, plan.getAddedCount());
		assertEquals(0, plan.getSkippedCount());
	}

	@Test
	public void mergeKeepsTheExistingLabelOnACollision()
	{
		List<SavedLocation> existing = Arrays.asList(at("my wine spot", 3095, 3494));
		List<SavedLocation> incoming = Arrays.asList(at("Wine of Zamorak", 3095, 3494));

		ImportPlan plan = ImportPlan.create(existing, incoming, ImportPlan.Mode.MERGE);

		assertEquals(1, plan.getResult().size());
		assertEquals("my wine spot", plan.getResult().get(0).getLabel());
		assertEquals(0, plan.getAddedCount());
		assertEquals(1, plan.getSkippedCount());
	}

	@Test
	public void mergeNeverMutatesAnExistingEntry()
	{
		SavedLocation mine = new SavedLocation("mine", 3000, 3000, 0, true);
		ImportPlan plan = ImportPlan.create(
			Arrays.asList(mine),
			Arrays.asList(new SavedLocation("theirs", 3000, 3000, 0, false)),
			ImportPlan.Mode.MERGE);

		// Same object, pinned state and label untouched.
		assertEquals(mine, plan.getResult().get(0));
		assertTrue(plan.getResult().get(0).isPinned());
	}

	@Test
	public void identityIsTheCoordinateNotTheLabel()
	{
		List<SavedLocation> existing = Arrays.asList(at("a", 3000, 3000));

		// Same tile, different name: a duplicate.
		assertEquals(1, ImportPlan.create(existing, Arrays.asList(at("b", 3000, 3000)),
			ImportPlan.Mode.MERGE).getResult().size());

		// Same name, different tile: not a duplicate.
		assertEquals(2, ImportPlan.create(existing, Arrays.asList(at("a", 3001, 3000)),
			ImportPlan.Mode.MERGE).getResult().size());
	}

	@Test
	public void planeIsPartOfIdentity()
	{
		List<SavedLocation> existing = Arrays.asList(new SavedLocation("ground", 3000, 3000, 0, false));
		List<SavedLocation> incoming = Arrays.asList(new SavedLocation("upstairs", 3000, 3000, 1, false));

		assertEquals(2, ImportPlan.create(existing, incoming, ImportPlan.Mode.MERGE)
			.getResult().size());
	}

	@Test
	public void importedEntriesArriveUnpinned()
	{
		List<SavedLocation> incoming = Arrays.asList(
			new SavedLocation("theirs", 3100, 3100, 0, true));

		ImportPlan merged = ImportPlan.create(new ArrayList<>(), incoming, ImportPlan.Mode.MERGE);
		assertFalse(merged.getResult().get(0).isPinned());

		ImportPlan replaced = ImportPlan.create(new ArrayList<>(), incoming, ImportPlan.Mode.REPLACE);
		assertFalse(replaced.getResult().get(0).isPinned());
	}

	@Test
	public void replaceDiscardsEverythingExisting()
	{
		List<SavedLocation> existing = Arrays.asList(at("a", 3000, 3000), at("b", 3001, 3000));
		List<SavedLocation> incoming = Arrays.asList(at("c", 3100, 3100));

		ImportPlan plan = ImportPlan.create(existing, incoming, ImportPlan.Mode.REPLACE);

		assertEquals(1, plan.getResult().size());
		assertEquals("c", plan.getResult().get(0).getLabel());
		assertEquals(2, plan.getDiscardedCount());
	}

	@Test
	public void mergeWithAnEmptyFileChangesNothing()
	{
		List<SavedLocation> existing = Arrays.asList(at("a", 3000, 3000));
		ImportPlan plan = ImportPlan.create(existing, new ArrayList<>(), ImportPlan.Mode.MERGE);
		assertEquals(existing, plan.getResult());
		assertEquals(0, plan.getAddedCount());
	}

	@Test
	public void duplicatesInsideTheIncomingFileAreCollapsed()
	{
		List<SavedLocation> incoming = Arrays.asList(at("a", 3000, 3000), at("b", 3000, 3000));
		ImportPlan plan = ImportPlan.create(new ArrayList<>(), incoming, ImportPlan.Mode.MERGE);
		assertEquals(1, plan.getResult().size());
		assertEquals(1, plan.getSkippedCount());
	}

	@Test
	public void outcomeIsAlwaysReported()
	{
		ImportPlan allDuplicates = ImportPlan.create(
			Arrays.asList(at("a", 3000, 3000)),
			Arrays.asList(at("b", 3000, 3000)),
			ImportPlan.Mode.MERGE);

		// An import where everything collided must not read as a silent no-op.
		assertEquals("Imported 0 new, skipped 1 already present.",
			allDuplicates.describeOutcome());

		assertEquals("Imported 1 new.", ImportPlan.create(
			new ArrayList<>(), Arrays.asList(at("a", 3000, 3000)), ImportPlan.Mode.MERGE)
			.describeOutcome());
	}
}
