package nz.tate.worldgrid;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import nz.tate.worldgrid.core.SavedLocation;

/**
 * The saved-location library.
 *
 * <p>There is no search box here on purpose: searching lives on the world map's
 * right-click menu, so this panel is only ever a list of places you have kept.
 *
 * <p>Everything in this class runs on the Swing event thread. Reading the player's
 * position goes through {@link ClientThread}, and the answer comes back with
 * {@link SwingUtilities#invokeLater}.
 */
@Singleton
public class WorldGridPanel extends PluginPanel
{
	private final Client client;
	private final ClientThread clientThread;
	private final WorldGridConfig config;
	private final LocationRepository repository;
	private final ImportExport importExport;

	private final JPanel listPanel = new JPanel(new GridLayout(0, 1, 0, 5));
	private final JCheckBox selectAll = new JCheckBox("Select all");
	private final JButton deleteSelected = new JButton("Delete selected (0)");

	/**
	 * Transient view state. Rows are held by coordinate rather than by object, because
	 * pinning replaces the entry object while still meaning the same place. Never saved.
	 */
	private Set<String> selectedKeys = new LinkedHashSet<>();

	@Inject
	WorldGridPanel(Client client, ClientThread clientThread, WorldGridConfig config,
		LocationRepository repository, ImportExport importExport)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.repository = repository;
		this.importExport = importExport;

		setLayout(new BorderLayout(0, 10));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(createHeader(), BorderLayout.NORTH);

		// The list sits at the top of a stretchy panel so short lists do not have their
		// rows spread down the whole panel.
		JPanel listHolder = new JPanel(new BorderLayout());
		listHolder.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listHolder.add(listPanel, BorderLayout.NORTH);
		add(listHolder, BorderLayout.CENTER);

		repository.addListener(this::rebuild);
		rebuild();
	}

	private JPanel createHeader()
	{
		JPanel header = new JPanel(new GridLayout(0, 1, 0, 5));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Saved locations");
		title.setFont(title.getFont().deriveFont(Font.BOLD));
		header.add(title);

		JButton saveCurrent = new JButton("Save current location");
		saveCurrent.setFocusPainted(false);
		saveCurrent.addActionListener(event -> saveCurrentLocation());
		header.add(saveCurrent);

		selectAll.setBackground(ColorScheme.DARK_GRAY_COLOR);
		selectAll.setFocusPainted(false);
		selectAll.addActionListener(event -> toggleSelectAll());
		header.add(selectAll);

		deleteSelected.setFocusPainted(false);
		deleteSelected.addActionListener(event -> deleteSelected());
		header.add(deleteSelected);

		JPanel transfer = new JPanel(new GridLayout(1, 2, 5, 0));
		transfer.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JButton importButton = new JButton("Import");
		importButton.setFocusPainted(false);
		importButton.addActionListener(event ->
			importExport.startImport(this, this::clearSelection));
		transfer.add(importButton);

		JButton exportButton = new JButton("Export");
		exportButton.setFocusPainted(false);
		exportButton.addActionListener(event ->
			importExport.export(this, repository.getLocations()));
		transfer.add(exportButton);

		header.add(transfer);

		return header;
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

		updateSelectionControls(locations);
		listPanel.revalidate();
		listPanel.repaint();
	}

	private JLabel createMessage(String text)
	{
		JLabel message = new JLabel("<html><body style='width:170px'>" + text + "</body></html>");
		message.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return message;
	}

	private JPanel createRow(SavedLocation location, int gridSize)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(6, 8, 6, 4));

		JPanel text = new JPanel(new GridLayout(2, 1));
		text.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel name = new JLabel(location.getLabel());
		name.setToolTipText(location.getLabel());
		text.add(name);

		JLabel reference = new JLabel(LocationDisplay.reference(location, gridSize));
		reference.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		text.add(reference);

		JCheckBox select = new JCheckBox();
		select.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		select.setToolTipText("Select for bulk delete");
		select.setSelected(selectedKeys.contains(LocationEdits.placeKey(location)));
		select.addActionListener(event -> setSelected(location, select.isSelected()));

		row.add(select, BorderLayout.WEST);
		row.add(text, BorderLayout.CENTER);
		row.add(createRowButtons(location), BorderLayout.EAST);
		return row;
	}

	private JPanel createRowButtons(SavedLocation location)
	{
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		buttons.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JButton pin = compactButton(location.isPinned() ? "Pinned" : "Pin",
			location.isPinned()
				? "Remove this location's world map pin"
				: "Show this location on the world map");
		// A lit background rather than a tick, so the pin never reads as a selection
		// checkbox sitting next to it.
		pin.setBackground(location.isPinned()
			? ColorScheme.BRAND_ORANGE
			: ColorScheme.DARK_GRAY_COLOR);
		pin.addActionListener(event -> togglePinned(location));
		buttons.add(pin);

		JButton showOnMap = compactButton("Map", "Centre the world map here");
		showOnMap.addActionListener(event -> showOnMap(location));
		buttons.add(showOnMap);

		JButton delete = compactButton("X", "Delete this location");
		delete.addActionListener(event -> deleteOne(location));
		buttons.add(delete);

		return buttons;
	}

	private JButton compactButton(String text, String tooltip)
	{
		JButton button = new JButton(text);
		button.setToolTipText(tooltip);
		button.setFocusPainted(false);
		button.setMargin(new Insets(2, 4, 2, 4));
		button.setPreferredSize(new Dimension(text.length() > 1 ? 8 + text.length() * 7 : 24, 22));
		return button;
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

	private void toggleSelectAll()
	{
		if (selectAll.isSelected())
		{
			selectedKeys = LocationEdits.allKeys(repository.getLocations());
		}
		else
		{
			selectedKeys = new LinkedHashSet<>();
		}
		rebuild();
	}

	private void updateSelectionControls(List<SavedLocation> locations)
	{
		deleteSelected.setText("Delete selected (" + selectedKeys.size() + ")");
		deleteSelected.setEnabled(!selectedKeys.isEmpty());
		selectAll.setEnabled(!locations.isEmpty());
		selectAll.setSelected(!locations.isEmpty() && selectedKeys.size() == locations.size());
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
