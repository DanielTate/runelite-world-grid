package nz.tate.worldgrid.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Pins the landmark references documented during scoping.
 *
 * <p>These are the tripwire for the anchor. Changing GridAnchor renames every square in
 * the world and invalidates every reference anyone has saved or written down, so it must
 * never happen by accident -- these assertions are what turn that into a loud failure.
 *
 * <p>Only landmarks whose world coordinate has been cross-checked appear here. The
 * remaining documented samples are listed in RISKS.md until their coordinates are
 * confirmed.
 */
public class LandmarkReferenceTest
{
	private static final int SIZE = 64;

	private static void assertReference(String expected, int x, int y)
	{
		assertEquals(expected, GridRefFormatter.format(GridMath.toCell(x, y, 0, SIZE)));
	}

	@Test
	public void lumbridge()
	{
		assertReference("L35", 3222, 3218);
	}

	@Test
	public void varrockSquare()
	{
		assertReference("O35", 3213, 3428);
	}

	@Test
	public void grandExchange()
	{
		assertReference("P34", 3165, 3489);
	}

	@Test
	public void falador()
	{
		assertReference("N31", 2965, 3380);
	}

	@Test
	public void edgeville()
	{
		assertReference("P33", 3087, 3500);
	}

	@Test
	public void theGridCoversTheOverworldInSingleLetters()
	{
		// 26 rows of 64 tiles reach exactly OVERWORLD_MAX_Y, which is why every surface
		// reference fits in one letter. Losing that property means the anchor is wrong.
		int northEdge = GridAnchor.ANCHOR_Y + 26 * SIZE;
		assertEquals(4160, northEdge);

		assertEquals("Z1", GridRefFormatter.format(
			GridMath.toCell(GridAnchor.ANCHOR_X, northEdge - 1, 0, SIZE)));
	}
}
