package nz.tate.worldgrid;

import nz.tate.worldgrid.core.LocationCodec;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ImportMessagesTest
{
	@Test
	public void theExportOptionsShowTheirCounts()
	{
		assertEquals("All saved (30)", ImportMessages.exportOptionLabel(false, 30));
		assertEquals("Pinned only (4)", ImportMessages.exportOptionLabel(true, 4));
		assertEquals("Pinned only (0)", ImportMessages.exportOptionLabel(true, 0));
	}

	@Test
	public void theTwoExportsGetDifferentDefaultNames()
	{
		String all = ImportMessages.defaultExportFileName(false);
		String pinned = ImportMessages.defaultExportFileName(true);

		assertEquals("world-grid-locations.json", all);
		assertEquals("world-grid-pinned.json", pinned);
		assertTrue(!all.equals(pinned));
	}

	@Test
	public void anExportAlwaysEndsUpAsJson()
	{
		assertEquals("route.json", ImportMessages.ensureJsonName("route"));
		assertEquals("route.json", ImportMessages.ensureJsonName("route.json"));
		assertEquals("route.JSON", ImportMessages.ensureJsonName("route.JSON"));
	}

	@Test
	public void theImportDialogNamesBothCounts()
	{
		assertEquals("This file has 12 locations. You have 4 saved.",
			ImportMessages.importCounts(12, 4));
	}

	@Test
	public void asingleIncomingLocationReadsAsSingular()
	{
		assertEquals("This file has 1 location. You have 0 saved.",
			ImportMessages.importCounts(1, 0));
	}

	@Test
	public void anOrdinarySizedFileGetsNoCaution()
	{
		assertNull(ImportMessages.largeImportCaution(1));
		assertNull(ImportMessages.largeImportCaution(
			LocationCodec.LARGE_FILE_WARNING_THRESHOLD));
	}

	@Test
	public void aLargeFileIsCautionedAboutButNeverRefused()
	{
		String caution = ImportMessages.largeImportCaution(
			LocationCodec.LARGE_FILE_WARNING_THRESHOLD + 1);

		assertNotNull(caution);
		assertTrue(caution.contains("201"));
	}
}
