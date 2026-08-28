package nz.tate.worldgrid;

import nz.tate.worldgrid.core.GridCell;
import nz.tate.worldgrid.core.GridMath;
import nz.tate.worldgrid.core.GridRefFormatter;
import nz.tate.worldgrid.core.GridRefParseResult;
import nz.tate.worldgrid.core.GridRefParser;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Pins the composition the world map search relies on: a typed reference becomes the
 * centre tile of its square, and that tile still reads back as the same reference.
 */
public class SearchTargetTest
{
	@Test
	public void aSearchedReferenceLandsInsideItsOwnSquare()
	{
		assertSearchLandsInTheSameSquare("W39", 64);
		assertSearchLandsInTheSameSquare("AA38", 64);
		assertSearchLandsInTheSameSquare("A1", 64);
	}

	@Test
	public void thisHoldsAtNonPowerOfTwoGridSizes()
	{
		assertSearchLandsInTheSameSquare("AG100", 22);
		assertSearchLandsInTheSameSquare("B7", 63);
	}

	@Test
	public void aPlaneSuffixSurvivesTheSearch()
	{
		assertSearchLandsInTheSameSquare("W39+2", 64);
	}

	@Test
	public void aBadReferenceIsRejectedWithAReasonToShowTheUser()
	{
		GridRefParseResult result = GridRefParser.parse("not a ref", 64);

		assertTrue(!result.isSuccess());
		assertTrue(result.getMessage() != null && !result.getMessage().isEmpty());
	}

	private static void assertSearchLandsInTheSameSquare(String typed, int gridSize)
	{
		GridRefParseResult result = GridRefParser.parse(typed, gridSize);
		assertTrue(typed + " should parse", result.isSuccess());

		GridCell cell = result.getCell();
		int centreX = GridMath.centreX(cell, gridSize);
		int centreY = GridMath.centreY(cell, gridSize);

		assertEquals(typed, GridRefFormatter.format(
			GridMath.toCell(centreX, centreY, cell.getPlane(), gridSize)));
	}
}
