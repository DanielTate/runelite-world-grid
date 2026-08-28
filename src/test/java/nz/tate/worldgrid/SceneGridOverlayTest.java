package nz.tate.worldgrid;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Covers the boundary arithmetic the scene overlay uses to decide which grid lines
 * cross the loaded scene. The drawing itself needs a running client and is verified
 * in-game.
 */
public class SceneGridOverlayTest
{
	@Test
	public void findsTheFirstBoundaryAtOrAfterAPoint()
	{
		// Anchor itself is a boundary.
		assertEquals(1000, SceneGridOverlay.firstBoundaryAtOrAfter(1000, 1000, 64));

		// One past a boundary rolls forward to the next.
		assertEquals(1064, SceneGridOverlay.firstBoundaryAtOrAfter(1001, 1000, 64));

		// Exactly on the next boundary stays put.
		assertEquals(1064, SceneGridOverlay.firstBoundaryAtOrAfter(1064, 1000, 64));
	}

	@Test
	public void worksBelowTheAnchor()
	{
		// Plain division truncates toward zero here and would return a boundary that
		// sits before the requested point.
		assertEquals(936, SceneGridOverlay.firstBoundaryAtOrAfter(900, 1000, 64));
		assertEquals(872, SceneGridOverlay.firstBoundaryAtOrAfter(872, 1000, 64));
	}

	@Test
	public void resultIsAlwaysOnTheGridAndNotBeforeTheStart()
	{
		int anchor = 768;
		for (int size : new int[]{1, 22, 63, 64, 256})
		{
			for (int from = 700; from < 1400; from++)
			{
				int boundary = SceneGridOverlay.firstBoundaryAtOrAfter(from, anchor, size);
				assertTrue("not before start", boundary >= from);
				assertTrue("less than one cell past", boundary < from + size);
				assertEquals("on the grid", 0, Math.floorMod(boundary - anchor, size));
			}
		}
	}
}
