package nz.tate.worldgrid.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Saved locations store coordinates and recompute the reference on display, so the
 * formatter and parser must be exact inverses. If they ever drift, a shared file starts
 * pointing somewhere other than where its author stood.
 */
public class GridRefRoundTripTest
{
	private static final int[] SIZES = {1, 22, 63, 64, 100, 256};

	@Test
	public void formatThenParseReturnsTheSameCellAcrossTheWorld()
	{
		for (int size : SIZES)
		{
			for (int x = GridAnchor.ANCHOR_X; x <= GridAnchor.WORLD_MAX; x += 337)
			{
				for (int y = GridAnchor.ANCHOR_Y; y <= GridAnchor.WORLD_MAX; y += 337)
				{
					GridCell original = GridMath.toCell(x, y, 0, size);
					String reference = GridRefFormatter.format(original);
					assertTrue("no reference for " + x + "," + y, reference != null);

					GridRefParseResult parsed = GridRefParser.parse(reference, size);
					assertTrue(reference + " at size " + size + " should parse", parsed.isSuccess());
					assertEquals(reference + " at size " + size, original, parsed.getCell());
				}
			}
		}
	}

	@Test
	public void roundTripHoldsForEveryPlane()
	{
		for (int plane = 0; plane <= 3; plane++)
		{
			GridCell original = GridMath.toCell(3222, 3218, plane, 64);
			String reference = GridRefFormatter.format(original);
			assertEquals(original, GridRefParser.parse(reference, 64).getCell());
		}
	}

	@Test
	public void parsedCellOriginLandsBackInsideTheSameCell()
	{
		// Parsing yields a square, not a tile. Converting the square's origin back must
		// return the same square, or a searched reference would pin the wrong place.
		for (int size : SIZES)
		{
			GridRefParseResult parsed = GridRefParser.parse("L35", size);
			if (!parsed.isSuccess())
			{
				continue;
			}
			GridCell cell = parsed.getCell();
			int x = GridMath.originX(cell, size);
			int y = GridMath.originY(cell, size);
			assertEquals(cell, GridMath.toCell(x, y, 0, size));

			int cx = GridMath.centreX(cell, size);
			int cy = GridMath.centreY(cell, size);
			assertEquals(cell, GridMath.toCell(cx, cy, 0, size));
		}
	}
}
