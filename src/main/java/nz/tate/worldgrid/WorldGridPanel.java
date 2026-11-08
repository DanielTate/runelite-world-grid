package nz.tate.worldgrid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import nz.tate.worldgrid.core.SavedLocation;

/**
 * The side panel: where you are on the grid right now, switches for where the grid is drawn,
 * and the saved-location library.
 *
 * <p>There is no search box here on purpose: searching lives on the world map's
 * right-click menu, so the library is only ever a list of places you have kept.
 *
 * <p>Everything in this class runs on the Swing event thread. Reading the player's
 * position goes through {@link ClientThread}, and the answer comes back with
 * {@link SwingUtilities#invokeLater}.
 */
@Singleton
public class WorldGridPanel extends PluginPanel
{
	/** Must match runelite-plugin.properties; a test checks it does. */
	static final String VERSION = "1.1";

	private final Client client;
	private final ClientThread clientThread;
	private final WorldGridConfig config;
	private final ConfigManager configManager;
	private final LocationRepository repository;
	private final ImportExport importExport;

	private final JLabel reference = new JLabel(WorldGridPlugin.NO_REFERENCE);
	private final JLabel referenceDetail = PanelStyle.small("");
	private final PanelStyle.Switch drawInScene;
	private final PanelStyle.Switch drawOnWorldMap;
	private final PanelStyle.Switch showReadout;
	private final PanelStyle.Stepper gridSize;
	private final PanelStyle.Stepper lineThickness;
	private final PanelStyle.Swatch gridColour;
	private final ColorPickerManager colorPickerManager;
	private final JLabel savedCount = PanelStyle.small("");
	private final JPanel listPanel = PanelStyle.column(4);
	private final PanelStyle.TickBox selectAll = new PanelStyle.TickBox();
	private final JLabel selectAllLabel = PanelStyle.small("Select all");
	private final JLabel deleteSelected = PanelStyle.link("Delete selected", "Delete every ticked location", this::deleteSelected);

	/**
	 * Transient view state. Rows are held by coordinate rather than by object, because
	 * pinning replaces the entry object while still meaning the same place. Never saved.
	 */
	private Set<String> selectedKeys = new LinkedHashSet<>();

	@Inject
	WorldGridPanel(Client client, ClientThread clientThread, WorldGridConfig config, ConfigManager configManager,
		ColorPickerManager colorPickerManager, LocationRepository repository, ImportExport importExport)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.configManager = configManager;
		this.colorPickerManager = colorPickerManager;
		drawInScene = new PanelStyle.Switch(config.drawInScene());
		drawOnWorldMap = new PanelStyle.Switch(config.drawOnWorldMap());
		showReadout = new PanelStyle.Switch(config.showRefReadout());
		gridSize = new PanelStyle.Stepper(config.gridSize(), 1, 256);
		lineThickness = new PanelStyle.Stepper(config.lineThickness(), 1, 5);
		gridColour = new PanelStyle.Swatch(config.gridColour(), this::pickColour);
		this.repository = repository;
		this.importExport = importExport;

		setLayout(new BorderLayout(0, PanelStyle.GAP));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel sections = PanelStyle.column(PanelStyle.GAP);
		sections.add(header());
		sections.add(PanelStyle.card("You are in", null, referenceArea()));
		sections.add(PanelStyle.card("Grid", null, gridArea()));
		sections.add(PanelStyle.card("Appearance", null, appearanceArea()));
		sections.add(PanelStyle.card("Saved locations", savedCount, savedArea()));
		add(sections, BorderLayout.NORTH);
		add(versionLabel(), BorderLayout.SOUTH);

		repository.addListener(this::rebuild);
		rebuild();
	}

	private static JComponent header()
	{
		JLabel title = new JLabel("World Grid");
		title.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
		title.setForeground(Color.WHITE);
		JPanel header = PanelStyle.column(2);
		header.add(title);
		header.add(PanelStyle.small("Name any spot in the world by its square"));
		return header;
	}

	private static JLabel versionLabel()
	{
		JLabel version = new JLabel("v" + VERSION, SwingConstants.RIGHT);
		version.setFont(FontManager.getRunescapeSmallFont());
		version.setForeground(ColorScheme.LIGHT_GRAY_COLOR.darker());
		return version;
	}

	private JComponent referenceArea()
	{
		// The plain font: the bold one leaves a gap before every 0.
		reference.setFont(FontManager.getRunescapeFont().deriveFont(32f));
		reference.setForeground(PanelStyle.ACCENT);
		JPanel area = PanelStyle.column(2);
		area.add(reference);
		area.add(referenceDetail);
		showReference(WorldGridPlugin.NO_REFERENCE, null);
		return area;
	}

	private JComponent gridArea()
	{
		drawInScene.onChange(on -> set("drawInScene", on));
		drawOnWorldMap.onChange(on -> set("drawOnWorldMap", on));
		showReadout.onChange(on -> set("showRefReadout", on));
		gridSize.onChange(size -> set("gridSize", size));
		JPanel area = PanelStyle.column(10);
		area.add(PanelStyle.toggle(drawInScene, "Show in game", "Lines and labels on the ground"));
		area.add(PanelStyle.toggle(drawOnWorldMap, "Show on world map", "Lines and labels on the map"));
		area.add(PanelStyle.toggle(showReadout, "Reference box", "Your square, in a movable box"));
		gridSize.setToolTipText("Friends must use the same size for your references to match");
		area.add(PanelStyle.setting("Square size", "Tiles per square", gridSize));
		return area;
	}

	private JComponent appearanceArea()
	{
		lineThickness.onChange(px -> set("lineThickness", px));
		JPanel area = PanelStyle.column(10);
		area.add(PanelStyle.setting("Colour", "Lines and labels", gridColour));
		area.add(PanelStyle.setting("Line thickness", "In pixels, 1 to 5", lineThickness));
		return area;
	}

	private void set(String key, Object value)
	{
		configManager.setConfiguration(WorldGridConfig.GROUP, key, value);
	}

	/** Opens RuneLite's own colour picker, saving the colour as it changes. */
	private void pickColour()
	{
		RuneliteColorPicker picker = colorPickerManager.create(SwingUtilities.getWindowAncestor(this),
			config.gridColour(), "Grid colour", false);
		picker.setLocationRelativeTo(this);
		picker.setOnColorChange(colour ->
		{
			gridColour.setColour(colour);
			set("gridColour", colour);
		});
		picker.setVisible(true);
	}

	private JComponent savedArea()
	{
		JComponent saveCurrent = PanelStyle.button("+  Save current location",
			"Name the tile you're standing on and keep it here", this::saveCurrentLocation);

		selectAll.onChange(on -> toggleSelectAll(on));
		JPanel tickAll = new JPanel(new java.awt.FlowLayout(FlowLayout.LEFT, 6, 0));
		tickAll.setOpaque(false);
		tickAll.add(selectAll);
		tickAll.add(selectAllLabel);
		JPanel bulk = new JPanel(new BorderLayout());
		bulk.setOpaque(false);
		bulk.add(tickAll, BorderLayout.WEST);
		bulk.add(deleteSelected, BorderLayout.EAST);

		JLabel importLink = PanelStyle.link("Import", "Add locations from a file",
			() -> importExport.startImport(this, this::clearSelection));
		JLabel exportLink = PanelStyle.link("Export", "Save your locations to a file to share",
			() -> importExport.export(this, repository.getLocations()));
		JPanel transfer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		transfer.setOpaque(false);
		transfer.add(importLink);
		transfer.add(dot());
		transfer.add(exportLink);

		JPanel area = PanelStyle.column(6);
		area.add(saveCurrent);
		area.add(listPanel);
		area.add(PanelStyle.fixedHeight(bulk));
		area.add(PanelStyle.fixedHeight(transfer));
		return area;
	}

	private static JLabel dot()
	{
		JLabel dot = PanelStyle.small(" \u00B7 ");
		dot.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		return dot;
	}

	/** Shows the square you're standing in, or a dash with why there's none. */
	void showReference(String ref, WorldPoint where)
	{
		boolean known = ref != null && !WorldGridPlugin.NO_REFERENCE.equals(ref);
		reference.setText(known ? ref : WorldGridPlugin.NO_REFERENCE);
		reference.setForeground(known ? PanelStyle.ACCENT : PanelStyle.MUTED);
		referenceDetail.setText(known && where != null
			? config.gridSize() + "-tile squares \u00B7 tile " + where.getX() + ", " + where.getY()
			: "Not available here (logged out or in an instance)");
	}

	/** Keeps the switches in step when a setting changes from the config panel. */
	void settingsChanged()
	{
		drawInScene.setOn(config.drawInScene());
		drawOnWorldMap.setOn(config.drawOnWorldMap());
		showReadout.setOn(config.showRefReadout());
		gridSize.setValue(config.gridSize());
		lineThickness.setValue(config.lineThickness());
		gridColour.setColour(config.gridColour());
	}

	/** Rebuilds every row. Cheap enough for a list of this size and impossible to get stale. */
	void rebuild()
	{
		listPanel.removeAll();

		String error = repository.getLoadError();
		if (error != null)
		{
			listPanel.add(createMessage("Could not read your saved locations: " + error));
		}

		List<SavedLocation> locations = repository.getLocations();
		selectedKeys = LocationEdits.retainExisting(selectedKeys, locations);

		if (locations.isEmpty())
		{
			listPanel.add(createMessage("Nothing saved yet."));
		}
		else
		{
			int gridSize = config.gridSize();
			for (SavedLocation location : locations)
			{
				listPanel.add(createRow(location, gridSize));
			}
		}

		savedCount.setText(locations.size() == 1 ? "1 place" : locations.size() + " places");
		updateSelectionControls(locations);
		listPanel.revalidate();
		listPanel.repaint();
	}

	private JLabel createMessage(String text)
	{
		JLabel message = new JLabel("<html><body style='width:150px'>" + text + "</body></html>");
		message.setFont(FontManager.getRunescapeSmallFont());
		message.setForeground(PanelStyle.MUTED);
		return message;
	}

	/** A saved place: a tick box for bulk delete, its name and square, then its actions. */
	private JComponent createRow(SavedLocation location, int gridSize)
	{
		PanelStyle.TickBox select = new PanelStyle.TickBox();
		select.setToolTipText("Tick to delete several at once");
		select.setTicked(selectedKeys.contains(LocationEdits.placeKey(location)));
		select.onChange(on -> setSelected(location, on));

		JLabel name = PanelStyle.text(location.getLabel());
		name.setToolTipText(location.getLabel());
		JLabel ref = PanelStyle.small(LocationDisplay.reference(location, gridSize)
			+ (location.isPinned() ? " \u00B7 pinned" : ""));
		if (location.isPinned())
		{
			ref.setForeground(PanelStyle.ACCENT);
		}

		JLabel pin = PanelStyle.link(location.isPinned() ? "Unpin" : "Pin",
			location.isPinned() ? "Remove this location's world map pin" : "Show this location on the world map",
			() -> togglePinned(location));
		JLabel map = PanelStyle.link("Map", "Centre the world map here", () -> showOnMap(location));
		JLabel delete = PanelStyle.link("Delete", "Delete this location", () -> deleteOne(location));
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		actions.setOpaque(false);
		actions.add(pin);
		actions.add(dot());
		actions.add(map);
		actions.add(dot());
		actions.add(delete);

		JPanel text = PanelStyle.column(2);
		text.add(name);
		text.add(ref);
		text.add(actions);

		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ColorScheme.DARK_GRAY_COLOR);
		row.setBorder(new EmptyBorder(7, 8, 7, 8));
		JPanel tick = new JPanel(new BorderLayout());
		tick.setOpaque(false);
		tick.add(select, BorderLayout.NORTH);
		tick.setBorder(new EmptyBorder(1, 0, 0, 4));
		row.add(tick, BorderLayout.WEST);
		row.add(text, BorderLayout.CENTER);
		return PanelStyle.fixedHeight(row);
	}

	private void setSelected(SavedLocation location, boolean selected)
	{
		String key = LocationEdits.placeKey(location);
		if (selected)
		{
			selectedKeys.add(key);
		}
		else
		{
			selectedKeys.remove(key);
		}
		updateSelectionControls(repository.getLocations());
	}

	private void toggleSelectAll(boolean on)
	{
		selectedKeys = on ? LocationEdits.allKeys(repository.getLocations()) : new LinkedHashSet<>();
		rebuild();
	}

	private void updateSelectionControls(List<SavedLocation> locations)
	{
		deleteSelected.setText(selectedKeys.isEmpty() ? "Delete selected" : "Delete " + selectedKeys.size() + " selected");
		PanelStyle.enable(deleteSelected, !selectedKeys.isEmpty());
		selectAll.setEnabled(!locations.isEmpty());
		selectAll.setTicked(!locations.isEmpty() && selectedKeys.size() == locations.size());
		selectAllLabel.setForeground(locations.isEmpty() ? ColorScheme.MEDIUM_GRAY_COLOR : PanelStyle.MUTED);
	}

	private void deleteSelected()
	{
		int count = selectedKeys.size();
		if (count == 0)
		{
			return;
		}

		if (LocationEdits.needsDeleteConfirmation(count)
			&& JOptionPane.showConfirmDialog(this,
				"Delete " + count + " saved locations?\n"
					+ "A backup of the current file is written first.",
				"Delete locations", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
		{
			return;
		}

		repository.replaceAll(
			LocationEdits.withoutKeys(repository.getLocations(), selectedKeys), true);
		selectedKeys = new LinkedHashSet<>();
	}

	/** Selection is view state only, so closing the panel forgets it. */
	@Override
	public void onDeactivate()
	{
		clearSelection();
	}

	private void clearSelection()
	{
		selectedKeys = new LinkedHashSet<>();
		rebuild();
	}

	private void togglePinned(SavedLocation location)
	{
		repository.replaceAll(LocationEdits.withPinnedKey(repository.getLocations(),
			LocationEdits.placeKey(location), !location.isPinned()), false);
	}

	private void showOnMap(SavedLocation location)
	{
		WorldPoint target =
			new WorldPoint(location.getX(), location.getY(), location.getPlane());
		clientThread.invoke(() ->
		{
			WorldMap worldMap = client.getWorldMap();
			if (worldMap != null)
			{
				worldMap.setWorldMapPositionTarget(target);
			}
		});
	}

	private void deleteOne(SavedLocation location)
	{
		int choice = JOptionPane.showConfirmDialog(this,
			"Delete \"" + location.getLabel() + "\"?",
			"Delete location", JOptionPane.YES_NO_OPTION);
		if (choice != JOptionPane.YES_OPTION)
		{
			return;
		}

		repository.replaceAll(LocationEdits.withoutKeys(repository.getLocations(),
			Collections.singleton(LocationEdits.placeKey(location))), true);
	}

	/**
	 * Reads where the player is standing before asking for a name, so the entry records
	 * the spot you clicked from rather than wherever you drifted to while typing.
	 */
	private void saveCurrentLocation()
	{
		clientThread.invoke(() ->
		{
			final WorldPoint here = playerLocation();
			SwingUtilities.invokeLater(() -> promptAndSave(here));
		});
	}

	private WorldPoint playerLocation()
	{
		if (client.isInInstancedRegion())
		{
			return null;
		}

		Player player = client.getLocalPlayer();
		return player == null ? null : player.getWorldLocation();
	}

	private void promptAndSave(WorldPoint point)
	{
		if (point == null)
		{
			JOptionPane.showMessageDialog(this,
				"Your position is not available here. Instanced areas report coordinates"
					+ " that do not match where you are.",
				"Cannot save", JOptionPane.WARNING_MESSAGE);
			return;
		}

		String label = JOptionPane.showInputDialog(this, "Name for this location:",
			"Save location", JOptionPane.PLAIN_MESSAGE);
		SavedLocation saved = buildSaved(label, point);
		if (saved == null)
		{
			return;
		}

		if (LocationEdits.containsKey(repository.getLocations(), LocationEdits.placeKey(saved)))
		{
			JOptionPane.showMessageDialog(this, "That exact tile is already saved.",
				"Already saved", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		repository.replaceAll(LocationEdits.withAdded(repository.getLocations(), saved), false);
	}

	/** @return the entry to save, or null when the user cancelled or typed nothing usable */
	private SavedLocation buildSaved(String label, WorldPoint point)
	{
		String trimmed = LocationEdits.cleanLabel(label);
		if (trimmed == null)
		{
			return null;
		}

		return new SavedLocation(trimmed, point.getX(), point.getY(), point.getPlane(), false);
	}
}
