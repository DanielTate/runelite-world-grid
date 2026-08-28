package nz.tate.worldgrid;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nz.tate.worldgrid.core.SavedLocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class LocationEditsTest
{
	private static final SavedLocation WINE =
		new SavedLocation("Wine spot", 3095, 3494, 0, true);
	private static final SavedLocation MEET =
		new SavedLocation("Clan meet", 3222, 3218, 0, false);
	private static final SavedLocation UPSTAIRS =
		new SavedLocation("Castle roof", 3222, 3218, 2, true);

	private static List<SavedLocation> library()
	{
		return Arrays.asList(WINE, MEET, UPSTAIRS);
	}

	@Test
	public void theKeyIsTheCoordinateNotTheLabel()
	{
		assertEquals("3095,3494,0", LocationEdits.placeKey(WINE));
		assertEquals(LocationEdits.placeKey(WINE),
			LocationEdits.placeKey(new SavedLocation("a different name", 3095, 3494, 0, false)));
	}

	@Test
	public void thePlaneIsPartOfTheKey()
	{
		assertFalse(LocationEdits.placeKey(MEET).equals(LocationEdits.placeKey(UPSTAIRS)));
	}

	@Test
	public void pinningReplacesOnlyTheMatchingEntry()
	{
		List<SavedLocation> result =
			LocationEdits.withPinnedKey(library(), LocationEdits.placeKey(MEET), true);

		assertEquals(3, result.size());
		assertTrue(result.get(1).isPinned());
		assertEquals("Clan meet", result.get(1).getLabel());
		assertEquals(WINE, result.get(0));
		assertEquals(UPSTAIRS, result.get(2));
	}

	@Test
	public void unpinningLeavesTheEntryInPlace()
	{
		List<SavedLocation> result =
			LocationEdits.withPinnedKey(library(), LocationEdits.placeKey(WINE), false);

		assertEquals(3, result.size());
		assertFalse(result.get(0).isPinned());
		assertEquals(0, LocationEdits.countPinned(
			LocationEdits.withPinnedKey(result, LocationEdits.placeKey(UPSTAIRS), false)));
	}

	@Test
	public void anUnknownKeyChangesNothing()
	{
		assertEquals(library(), LocationEdits.withPinnedKey(library(), "1,2,0", true));
	}

	@Test
	public void deletingRemovesEverySelectedRowAndKeepsTheRest()
	{
		Set<String> selected = new HashSet<>();
		selected.add(LocationEdits.placeKey(WINE));
		selected.add(LocationEdits.placeKey(UPSTAIRS));

		List<SavedLocation> result = LocationEdits.withoutKeys(library(), selected);

		assertEquals(1, result.size());
		assertEquals(MEET, result.get(0));
	}

	@Test
	public void deletingNothingKeepsEverything()
	{
		assertEquals(library(),
			new ArrayList<>(LocationEdits.withoutKeys(library(), Collections.emptySet())));
	}

	@Test
	public void addingAppendsToTheEnd()
	{
		SavedLocation extra = new SavedLocation("New", 1000, 1000, 0, false);
		List<SavedLocation> result = LocationEdits.withAdded(library(), extra);

		assertEquals(4, result.size());
		assertEquals(extra, result.get(3));
	}

	@Test
	public void exportingEverythingKeepsTheWholeList()
	{
		assertEquals(library(), new ArrayList<>(LocationEdits.forExport(library(), false)));
	}

	@Test
	public void exportingPinnedOnlyKeepsJustThePinnedRows()
	{
		List<SavedLocation> result = LocationEdits.forExport(library(), true);

		assertEquals(2, result.size());
		assertEquals(WINE, result.get(0));
		assertEquals(UPSTAIRS, result.get(1));
	}

	@Test
	public void exportingPinnedOnlyFromAnUnpinnedListIsEmpty()
	{
		List<SavedLocation> nonePinned = Collections.singletonList(MEET);

		assertTrue(LocationEdits.forExport(nonePinned, true).isEmpty());
		assertEquals(0, LocationEdits.countPinned(nonePinned));
	}

	@Test
	public void countPinnedMatchesTheFilteredSize()
	{
		assertEquals(2, LocationEdits.countPinned(library()));
	}

	@Test
	public void containsKeyFindsAPlaceRegardlessOfLabel()
	{
		assertTrue(LocationEdits.containsKey(library(), "3222,3218,0"));
		assertFalse(LocationEdits.containsKey(library(), "3222,3218,1"));
	}

	@Test
	public void theBackupNameCarriesADateAndTime()
	{
		Instant when = Instant.parse("2026-08-29T09:34:12Z");

		assertEquals("locations.backup-20260829-093412.json",
			LocationEdits.backupFileName(when, ZoneId.of("UTC")));
	}

	@Test
	public void backupNamesSortIntoTimeOrder()
	{
		String earlier = LocationEdits.backupFileName(
			Instant.parse("2026-08-29T09:34:12Z"), ZoneId.of("UTC"));
		String later = LocationEdits.backupFileName(
			Instant.parse("2026-12-01T00:00:00Z"), ZoneId.of("UTC"));

		assertTrue(earlier.compareTo(later) < 0);
	}
}
