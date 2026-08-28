package nz.tate.worldgrid.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GridRefParserTest
{
	private static final int SIZE = 64;

	@Test
	public void parsesASimpleReference()
	{
		GridRefParseResult result = GridRefParser.parse("L35", SIZE);
		assertTrue(result.isSuccess());
		assertEquals(11, result.getCell().getRow());
		assertEquals(34, result.getCell().getColumn());
		assertEquals(0, result.getCell().getPlane());
	}

	@Test
	public void isCaseInsensitive()
	{
		assertEquals(
			GridRefParser.parse("L35", SIZE).getCell(),
			GridRefParser.parse("l35", SIZE).getCell());
	}

	@Test
	public void toleratesSurroundingAndInternalSpaces()
	{
		assertEquals(
			GridRefParser.parse("L35", SIZE).getCell(),
			GridRefParser.parse("  l 35 ", SIZE).getCell());
	}

	@Test
	public void parsesMultiLetterRows()
	{
		GridRefParseResult result = GridRefParser.parse("GA12", SIZE);
		assertTrue(result.isSuccess());
		assertEquals(GridRefFormatter.fromLetters("GA"), result.getCell().getRow());
		assertEquals(11, result.getCell().getColumn());
	}

	@Test
	public void parsesThePlaneSuffix()
	{
		assertEquals(2, GridRefParser.parse("L35+2", SIZE).getCell().getPlane());
	}

	@Test
	public void rejectsEmptyInput()
	{
		assertEquals(GridRefParseResult.Failure.EMPTY, GridRefParser.parse("", SIZE).getFailure());
		assertEquals(GridRefParseResult.Failure.EMPTY, GridRefParser.parse("   ", SIZE).getFailure());
		assertEquals(GridRefParseResult.Failure.EMPTY, GridRefParser.parse(null, SIZE).getFailure());
	}

	@Test
	public void rejectsMalformedInput()
	{
		String[] bad = {"35L", "L", "35", "L35+", "L-35", "!!", "L35+2+2", "L35x"};
		for (String input : bad)
		{
			GridRefParseResult result = GridRefParser.parse(input, SIZE);
			assertFalse("should reject " + input, result.isSuccess());
		}
	}

	@Test
	public void rejectsPlaneOutsideOneToThree()
	{
		assertEquals(
			GridRefParseResult.Failure.PLANE_OUT_OF_RANGE,
			GridRefParser.parse("L35+0", SIZE).getFailure());
		assertEquals(
			GridRefParseResult.Failure.PLANE_OUT_OF_RANGE,
			GridRefParser.parse("L35+4", SIZE).getFailure());
	}

	@Test
	public void rejectsColumnZero()
	{
		assertEquals(
			GridRefParseResult.Failure.COLUMN_OUT_OF_RANGE,
			GridRefParser.parse("L0", SIZE).getFailure());
	}

	@Test
	public void rejectsReferencesOutsideTheWorld()
	{
		assertEquals(
			GridRefParseResult.Failure.OUTSIDE_WORLD,
			GridRefParser.parse("L9999", SIZE).getFailure());
		assertEquals(
			GridRefParseResult.Failure.OUTSIDE_WORLD,
			GridRefParser.parse("ZZZ1", SIZE).getFailure());
	}

	@Test
	public void hugeInputDoesNotOverflowIntoAValidCell()
	{
		// A column large enough to wrap a 32-bit int must be refused, not silently
		// folded back into the playable world.
		GridRefParseResult result = GridRefParser.parse("A999999999999", SIZE);
		assertFalse(result.isSuccess());
	}

	@Test
	public void everyFailureCarriesAMessage()
	{
		String[] bad = {"", "35L", "L0", "L35+9", "L9999"};
		for (String input : bad)
		{
			assertNotNull("needs a message: " + input, GridRefParser.parse(input, SIZE).getMessage());
		}
	}
}
