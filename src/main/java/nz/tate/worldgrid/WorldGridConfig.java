package nz.tate.worldgrid;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/**
 * Config keys are permanent. Renaming one silently resets everybody's settings, so these
 * names cannot change once the plugin has shipped.
 */
@ConfigGroup(WorldGridConfig.GROUP)
public interface WorldGridConfig extends Config
{
	String GROUP = "world-grid";

	@ConfigSection(
		name = "Grid",
		description = "Size and where the grid is drawn",
		position = 0
	)
	String gridSection = "gridSection";

	@ConfigSection(
		name = "Appearance",
		description = "Colour and line thickness",
		position = 1
	)
	String appearanceSection = "appearanceSection";

	@Range(min = 1, max = 256)
	@ConfigItem(
		keyName = "gridSize",
		name = "Grid size",
		description = "Width of one square in tiles. Both players must use the same size"
			+ " for spoken references to agree.",
		position = 0,
		section = gridSection
	)
	default int gridSize()
	{
		return 64;
	}

	@ConfigItem(
		keyName = "drawOnWorldMap",
		name = "Draw grid on world map",
		description = "Overlay the grid on the world map.",
		position = 1,
		section = gridSection
	)
	default boolean drawOnWorldMap()
	{
		return true;
	}

	@ConfigItem(
		keyName = "drawInScene",
		name = "Draw grid in scene",
		description = "Overlay the grid on the game world, and label the square you are"
			+ " standing on.",
		position = 2,
		section = gridSection
	)
	default boolean drawInScene()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showRefReadout",
		name = "Show grid reference readout",
		description = "Show a movable readout of your current grid reference. Works with"
			+ " both grid overlays turned off.",
		position = 3,
		section = gridSection
	)
	default boolean showRefReadout()
	{
		return true;
	}

	/** Remembered so an exported file lands where the last one did. Not user-facing. */
	@ConfigItem(
		keyName = "lastFileDirectory",
		name = "Last import/export folder",
		description = "Remembered folder for import and export.",
		hidden = true
	)
	default String lastFileDirectory()
	{
		return "";
	}

	@Alpha
	@ConfigItem(
		keyName = "gridColour",
		name = "Grid colour",
		description = "Colour of the grid lines and labels.",
		position = 0,
		section = appearanceSection
	)
	default Color gridColour()
	{
		return new Color(255, 255, 255, 100);
	}

	@Range(min = 1, max = 5)
	@ConfigItem(
		keyName = "lineThickness",
		name = "Line thickness",
		description = "Thickness of the grid lines in pixels.",
		position = 1,
		section = appearanceSection
	)
	default int lineThickness()
	{
		return 1;
	}
}
