package nz.tate.worldgrid;

import nz.tate.worldgrid.core.SavedLocation;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class LocationDisplayTest
{
	private static final SavedLocation LUMBRIDGE =
		new SavedLocation("Lumbridge", 3222, 3218, 0, false);

	@Test
	public void theReferenceComesFromTheViewersOwnGridSize()
	{
		assertEquals("W39", LocationDisplay.reference(LUMBRIDGE, 64));
	}

	@Test
	public void theSamePlaceReadsDifferentlyAtADifferentGridSize()
	{
		String atSixtyFour = LocationDisplay.reference(LUMBRIDGE, 64);
		String atThirtyTwo = LocationDisplay.reference(LUMBRIDGE, 32);

		assertEquals("W39", atSixtyFour);
		assertEquals(false, atSixtyFour.equals(atThirtyTwo));
	}

	@Test
	public void aPlaceOutsideTheGridReadsAsNoReference()
	{
		SavedLocation southWestOfTheAnchor = new SavedLocation("Nowhere", 0, 0, 0, false);

		assertEquals(LocationDisplay.NO_REFERENCE,
			LocationDisplay.reference(southWestOfTheAnchor, 64));
	}

	@Test
	public void aPinTooltipCarriesTheNameAndTheLiveReference()
	{
		assertEquals("Lumbridge (W39)", LocationDisplay.tooltip(LUMBRIDGE, 64));
	}
}
