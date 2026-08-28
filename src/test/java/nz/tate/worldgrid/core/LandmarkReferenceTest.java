package nz.tate.worldgrid.core;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

/**
 * Pins landmark references against the anchor.
 *
 * <p>These are the tripwire. Changing GridAnchor renames every square in the world and
 * invalidates every reference anyone has saved or written down, so it must never happen
 * by accident -- these assertions turn that into a loud failure.
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
		assertReference("W39", 3222, 3218);
	}

	@Test
	public void varrockSquare()
	{
		assertReference("Z39", 3213, 3428);
	}

	@Test
	public void grandExchange()
	{
		assertReference("AA38", 3165, 3489);
	}

	@Test
	public void falador()
	{
		assertReference("Y35", 2965, 3380);
	}

	@Test
	public void edgeville()
	{
		assertReference("AA37", 3087, 3500);
	}

	@Test
	public void kourendCastle()
	{
		assertReference("AD14", 1628, 3673);
	}

	@Test
	public void southernSailingContentIsNameable()
	{
		// Sailing pushed the surface south of the old anchor, which made these points
		// produce a negative row index -- unrepresentable, so literally unnameable.
		assertReference("H29", 2567, 2295);
		assertReference("G41", 3357, 2203);
	}

	@Test
	public void theSouthWestMostSurfaceSquareIsInsideTheGrid()
	{
		GridCell corner = GridMath.toCell(960, 2048, 0, SIZE);
		assertFalse(corner.isNegative());
		assertReference("E4", 960, 2048);
	}

	@Test
	public void theNorthernOceanBandIsInsideTheGrid()
	{
		// The surface now reaches y=4223, above Constants.OVERWORLD_MAX_Y (4160). That
		// constant is stale, so the plugin never uses it as a bound.
		assertFalse(GridMath.toCell(3000, 4223, 0, SIZE).isNegative());
	}
}
