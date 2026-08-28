package nz.tate.worldgrid.core;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LocationCodecTest
{
	private LocationCodec codec;

	@Before
	public void setUp()
	{
		codec = new LocationCodec(new Gson());
	}

	@Test
	public void writesAndReadsBack()
	{
		List<SavedLocation> original = Arrays.asList(
			new SavedLocation("Wine of Zamorak", 3095, 3494, 0, true),
			new SavedLocation("Clan meet", 3222, 3218, 1, false));

		LocationReadResult result = codec.read(codec.write(original));

		assertTrue(result.isSuccess());
		assertEquals(original, result.getLocations());
	}

	@Test
	public void writesTheSchemaVersion()
	{
		String json = codec.write(new ArrayList<>());
		assertTrue(json.contains("\"version\""));
		assertTrue(json.contains("1"));
	}

	@Test
	public void writesThePinnedFlag()
	{
		// The flag travels in the file even though import always clears it, so the
		// format is complete and a reader can see what the author had lit up.
		String json = codec.write(Arrays.asList(
			new SavedLocation("Wine spot", 3095, 3494, 0, true)));
		assertTrue(json.contains("\"pinned\""));
		assertTrue(json.contains("true"));
	}

	@Test
	public void rejectsMalformedJson()
	{
		String[] bad = {
			"{",
			"not json at all",
			"{\"version\":1,\"locations\":[",
			"[1,2,3",
		};
		for (String json : bad)
		{
			LocationReadResult result = codec.read(json);
			assertFalse("should reject: " + json, result.isSuccess());
			assertNotNull(result.getError());
		}
	}

	@Test
	public void rejectsEmptyInput()
	{
		assertFalse(codec.read("").isSuccess());
		assertFalse(codec.read("   ").isSuccess());
		assertFalse(codec.read(null).isSuccess());
	}

	@Test
	public void readsAFileWithNoLocationsArray()
	{
		LocationReadResult result = codec.read("{\"version\":1}");
		assertTrue(result.isSuccess());
		assertTrue(result.getLocations().isEmpty());
	}

	@Test
	public void rejectsCoordinatesOutsideTheWorld()
	{
		assertFalse(codec.read(
			"{\"version\":1,\"locations\":[{\"label\":\"x\",\"x\":-1,\"y\":3000,\"plane\":0}]}")
			.isSuccess());
		assertFalse(codec.read(
			"{\"version\":1,\"locations\":[{\"label\":\"x\",\"x\":99999,\"y\":3000,\"plane\":0}]}")
			.isSuccess());
		assertFalse(codec.read(
			"{\"version\":1,\"locations\":[{\"label\":\"x\",\"x\":3000,\"y\":-5,\"plane\":0}]}")
			.isSuccess());
	}

	@Test
	public void rejectsImpossiblePlanes()
	{
		assertFalse(codec.read(
			"{\"version\":1,\"locations\":[{\"label\":\"x\",\"x\":3000,\"y\":3000,\"plane\":9}]}")
			.isSuccess());
		assertFalse(codec.read(
			"{\"version\":1,\"locations\":[{\"label\":\"x\",\"x\":3000,\"y\":3000,\"plane\":-1}]}")
			.isSuccess());
	}

	@Test
	public void rejectsEntriesWithNoName()
	{
		assertFalse(codec.read(
			"{\"version\":1,\"locations\":[{\"x\":3000,\"y\":3000,\"plane\":0}]}").isSuccess());
		assertFalse(codec.read(
			"{\"version\":1,\"locations\":[{\"label\":\"  \",\"x\":3000,\"y\":3000,\"plane\":0}]}")
			.isSuccess());
	}

	@Test
	public void rejectsAFileFromANewerSchema()
	{
		LocationReadResult result = codec.read("{\"version\":99,\"locations\":[]}");
		assertFalse(result.isSuccess());
		assertTrue(result.getError().contains("newer"));
	}

	@Test
	public void rejectsTheWholeFileWhenOneEntryIsBad()
	{
		// Validate before mutating anything: a partial import is the worst outcome.
		String json = "{\"version\":1,\"locations\":["
			+ "{\"label\":\"good\",\"x\":3000,\"y\":3000,\"plane\":0},"
			+ "{\"label\":\"bad\",\"x\":99999,\"y\":3000,\"plane\":0}]}";
		assertFalse(codec.read(json).isSuccess());
	}

	@Test
	public void warnsOnlyAboveTheThreshold()
	{
		assertFalse(LocationCodec.isLargeImport(200));
		assertTrue(LocationCodec.isLargeImport(201));
	}
}
