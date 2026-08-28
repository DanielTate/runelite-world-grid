package nz.tate.worldgrid.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GridRefFormatterTest
{
	@Test
	public void lumbridgeIsW39AtSize64()
	{
		// If this fails the anchor has moved, which renames every square in the world.
		GridCell cell = GridMath.toCell(3222, 3218, 0, 64);
		assertEquals("W39", GridRefFormatter.format(cell));
	}

	@Test
	public void lettersRunNorthAndNumbersRunEast()
	{
		// Row A is southernmost; the letter changes as you move north, not east.
		GridCell south = GridMath.toCell(3222, 3218, 0, 64);
		GridCell north = GridMath.toCell(3222, 3218 + 64, 0, 64);
		GridCell east = GridMath.toCell(3222 + 64, 3218, 0, 64);

		assertEquals("W39", GridRefFormatter.format(south));
		assertEquals("X39", GridRefFormatter.format(north));
		assertEquals("W40", GridRefFormatter.format(east));
	}

	@Test
	public void firstCellIsA1()
	{
		assertEquals("A1", GridRefFormatter.format(new GridCell(0, 0, 0)));
	}

	@Test
	public void lettersWrapPastZ()
	{
		assertEquals("A", GridRefFormatter.toLetters(0));
		assertEquals("Z", GridRefFormatter.toLetters(25));
		assertEquals("AA", GridRefFormatter.toLetters(26));
		assertEquals("AB", GridRefFormatter.toLetters(27));
		assertEquals("AZ", GridRefFormatter.toLetters(51));
		assertEquals("BA", GridRefFormatter.toLetters(52));
		assertEquals("ZZ", GridRefFormatter.toLetters(701));
		assertEquals("AAA", GridRefFormatter.toLetters(702));
	}

	@Test
	public void lettersRoundTripThroughFromLetters()
	{
		for (int i = 0; i < 5000; i++)
		{
			assertEquals(i, GridRefFormatter.fromLetters(GridRefFormatter.toLetters(i)));
		}
	}

	@Test
	public void groundLevelHasNoSuffix()
	{
		assertEquals("L35", GridRefFormatter.format(new GridCell(34, 11, 0)));
	}

	@Test
	public void upperPlanesAreSuffixed()
	{
		assertEquals("L35+1", GridRefFormatter.format(new GridCell(34, 11, 1)));
		assertEquals("L35+2", GridRefFormatter.format(new GridCell(34, 11, 2)));
		assertEquals("L35+3", GridRefFormatter.format(new GridCell(34, 11, 3)));
	}

	@Test
	public void negativeCellsHaveNoReference()
	{
		assertNull(GridRefFormatter.format(new GridCell(-1, 0, 0)));
		assertNull(GridRefFormatter.format(new GridCell(0, -1, 0)));
	}
}
