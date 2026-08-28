package nz.tate.worldgrid.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GridMathTest
{
	@Test
	public void anchorAlignsWithRegionsAtEveryPowerOfTwoSize()
	{
		// A region is 64 tiles. Divisibility by 256 means grid lines land on region
		// boundaries at every power-of-two size the config offers, not just at 64.
		assertEquals(0, GridAnchor.ANCHOR_X % 256);
		assertEquals(0, GridAnchor.ANCHOR_Y % 256);
	}

	@Test
	public void anchorTileIsTheFirstCell()
	{
		GridCell cell = GridMath.toCell(GridAnchor.ANCHOR_X, GridAnchor.ANCHOR_Y, 0, 64);
		assertEquals(0, cell.getColumn());
		assertEquals(0, cell.getRow());
	}

	@Test
	public void coordinatesWestOrSouthOfAnchorGoNegative()
	{
		// Plain integer division would truncate toward zero and report cell 0 here,
		// merging the squares either side of the anchor. floorDiv must not.
		GridCell west = GridMath.toCell(GridAnchor.ANCHOR_X - 1, GridAnchor.ANCHOR_Y, 0, 64);
		assertEquals(-1, west.getColumn());

		GridCell south = GridMath.toCell(GridAnchor.ANCHOR_X, GridAnchor.ANCHOR_Y - 1, 0, 64);
		assertEquals(-1, south.getRow());

		assertTrue(west.isNegative());
		assertTrue(south.isNegative());
	}

	@Test
	public void worksAtNonPowerOfTwoSizes()
	{
		// The reference plugin masks with ~(size-1), which is power-of-two only and
		// silently draws a wrong grid at sizes like 22.
		int size = 22;
		GridCell cell = GridMath.toCell(GridAnchor.ANCHOR_X + 43, GridAnchor.ANCHOR_Y + 45, 0, size);
		assertEquals(1, cell.getColumn());
		assertEquals(2, cell.getRow());

		assertEquals(GridAnchor.ANCHOR_X + 22, GridMath.originX(cell, size));
		assertEquals(GridAnchor.ANCHOR_Y + 44, GridMath.originY(cell, size));
	}

	@Test
	public void originIsTheLowestTileOfTheCell()
	{
		int size = 64;
		for (int offset = 0; offset < size; offset++)
		{
			GridCell cell = GridMath.toCell(
				GridAnchor.ANCHOR_X + 128 + offset, GridAnchor.ANCHOR_Y + 128 + offset, 0, size);
			assertEquals(GridAnchor.ANCHOR_X + 128, GridMath.originX(cell, size));
			assertEquals(GridAnchor.ANCHOR_Y + 128, GridMath.originY(cell, size));
		}
	}

	@Test
	public void centreSitsInsideTheCell()
	{
		int size = 64;
		GridCell cell = GridMath.toCell(3222, 3218, 0, size);
		assertTrue(GridMath.centreX(cell, size) >= GridMath.originX(cell, size));
		assertTrue(GridMath.centreX(cell, size) < GridMath.originX(cell, size) + size);
	}

	@Test
	public void sizeOfOneGivesEveryTileItsOwnCell()
	{
		GridCell a = GridMath.toCell(3222, 3218, 0, 1);
		GridCell b = GridMath.toCell(3223, 3218, 0, 1);
		assertEquals(a.getColumn() + 1, b.getColumn());
	}

	@Test
	public void gridSizeBoundsMatchTheConfigRange()
	{
		assertTrue(GridMath.isValidGridSize(1));
		assertTrue(GridMath.isValidGridSize(256));
		assertFalse(GridMath.isValidGridSize(0));
		assertFalse(GridMath.isValidGridSize(257));
	}

	@Test
	public void planeIsCarriedThrough()
	{
		assertEquals(2, GridMath.toCell(3222, 3218, 2, 64).getPlane());
	}
}
