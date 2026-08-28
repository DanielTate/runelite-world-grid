package nz.tate.worldgrid;

import nz.tate.worldgrid.core.GridCell;
import nz.tate.worldgrid.core.GridMath;
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
		assertEquals(1000, GridMath.firstBoundaryAtOrAfter(1000, 1000, 64));

		// One past a boundary rolls forward to the next.
		assertEquals(1064, GridMath.firstBoundaryAtOrAfter(1001, 1000, 64));

		// Exactly on the next boundary stays put.
		assertEquals(1064, GridMath.firstBoundaryAtOrAfter(1064, 1000, 64));
	}

	@Test
	public void worksBelowTheAnchor()
	{
		// Plain division truncates toward zero here and would return a boundary that
		// sits before the requested point.
		assertEquals(936, GridMath.firstBoundaryAtOrAfter(900, 1000, 64));
		assertEquals(872, GridMath.firstBoundaryAtOrAfter(872, 1000, 64));
	}

	@Test
	public void resultIsAlwaysOnTheGridAndNotBeforeTheStart()
	{
		int anchor = 768;
		for (int size : new int[]{1, 22, 63, 64, 256})
		{
			for (int from = 700; from < 1400; from++)
			{
				int boundary = GridMath.firstBoundaryAtOrAfter(from, anchor, size);
				assertTrue("not before start", boundary >= from);
				assertTrue("less than one cell past", boundary < from + size);
				assertEquals("on the grid", 0, Math.floorMod(boundary - anchor, size));
			}
		}
	}

	@Test
	public void compassLabelsSitOnTheCorrectEdges()
	{
		GridCell cell = new GridCell(10, 10, 0);
		int size = 64;

		int[] north = SceneGridOverlay.compassLabelPoint(cell, size, 'N');
		int[] south = SceneGridOverlay.compassLabelPoint(cell, size, 'S');
		int[] east = SceneGridOverlay.compassLabelPoint(cell, size, 'E');
		int[] west = SceneGridOverlay.compassLabelPoint(cell, size, 'W');

		// North is larger y, east is larger x. These are world directions, which is
		// what keeps the labels correct as the camera rotates.
		assertTrue("north is above south", north[1] > south[1]);
		assertTrue("east is right of west", east[0] > west[0]);

		// Opposite pairs share the other axis, so they line up across the square.
		assertEquals(north[0], south[0]);
		assertEquals(east[1], west[1]);
	}

	@Test
	public void compassLabelsStayInsideTheirCell()
	{
		int size = 64;
		GridCell cell = new GridCell(10, 10, 0);

		for (char direction : new char[]{'N', 'S', 'E', 'W'})
		{
			int[] point = SceneGridOverlay.compassLabelPoint(cell, size, direction);
			assertEquals("label " + direction + " belongs to its own cell",
				cell, GridMath.toCell(point[0], point[1], 0, size));
		}
	}
}
