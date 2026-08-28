package nz.tate.worldgrid;

import java.awt.Component;
import java.awt.GridLayout;
import java.io.File;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.runelite.client.config.ConfigManager;
import nz.tate.worldgrid.core.ImportPlan;
import nz.tate.worldgrid.core.LocationReadResult;
import nz.tate.worldgrid.core.SavedLocation;

/**
 * Import and export, both through {@link JFileChooser} so the user picks the path.
 *
 * <p>The working file lives under {@code .runelite}; anything the user means to send to
 * someone else goes wherever they say. The last directory they used is remembered, since
 * an exported file is meant to be found again.
 *
 * <p>All of this runs on the Swing event thread. Reading and writing the chosen file is
 * handed to the repository's background worker.
 */
@Singleton
public class ImportExport
{
	private final ConfigManager configManager;
	private final LocationRepository repository;

	@Inject
	ImportExport(ConfigManager configManager, LocationRepository repository)
	{
		this.configManager = configManager;
		this.repository = repository;
	}

	public void export(Component parent, List<SavedLocation> locations)
	{
		if (locations.isEmpty())
		{
			JOptionPane.showMessageDialog(parent, "There is nothing to export.",
				"Export locations", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		Boolean pinnedOnly = askExportScope(parent, locations);
		if (pinnedOnly == null)
		{
			return;
		}

		List<SavedLocation> toExport = LocationEdits.forExport(locations, pinnedOnly);

		JFileChooser chooser = newChooser("Export locations");
		chooser.setSelectedFile(new File(lastDirectory(),
			ImportMessages.defaultExportFileName(pinnedOnly)));
		if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}

		File chosen = new File(chooser.getSelectedFile().getParentFile(),
			ImportMessages.ensureJsonName(chooser.getSelectedFile().getName()));
		rememberDirectory(chosen);

		repository.writeExportFile(chosen, toExport, error ->
		{
			if (error == null)
			{
				JOptionPane.showMessageDialog(parent,
					"Exported " + toExport.size() + " to " + chosen.getName() + ".",
					"Export locations", JOptionPane.INFORMATION_MESSAGE);
			}
			else
			{
				JOptionPane.showMessageDialog(parent, "Could not write the file: " + error,
					"Export failed", JOptionPane.ERROR_MESSAGE);
			}
		});
	}

	/** @return true for pinned only, false for everything, null when cancelled */
	private Boolean askExportScope(Component parent, List<SavedLocation> locations)
	{
		int pinnedCount = LocationEdits.countPinned(locations);

		JRadioButton all = new JRadioButton(
			ImportMessages.exportOptionLabel(false, locations.size()), true);
		JRadioButton pinned = new JRadioButton(
			ImportMessages.exportOptionLabel(true, pinnedCount));
		// An empty file helps nobody, so the option is shown but not selectable.
		pinned.setEnabled(pinnedCount > 0);

		ButtonGroup group = new ButtonGroup();
		group.add(all);
		group.add(pinned);

		JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
		panel.add(new JLabel("What should be exported?"));
		panel.add(all);
		panel.add(pinned);

		int choice = JOptionPane.showConfirmDialog(parent, panel, "Export locations",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (choice != JOptionPane.OK_OPTION)
		{
			return null;
		}
		return pinned.isSelected();
	}

	public void startImport(Component parent, Runnable afterChange)
	{
		JFileChooser chooser = newChooser("Import locations");
		chooser.setCurrentDirectory(lastDirectory());
		if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}

		File chosen = chooser.getSelectedFile();
		rememberDirectory(chosen);

		// The whole file is validated before anything is offered, so a half-applied
		// import is not a state that can exist.
		repository.readImportFile(chosen, result -> finishImport(parent, result, afterChange));
	}

	private void finishImport(Component parent, LocationReadResult result, Runnable afterChange)
	{
		if (!result.isSuccess())
		{
			JOptionPane.showMessageDialog(parent, result.getError(),
				"Import failed", JOptionPane.ERROR_MESSAGE);
			return;
		}

		List<SavedLocation> incoming = result.getLocations();
		if (incoming.isEmpty())
		{
			JOptionPane.showMessageDialog(parent, "That file has no locations in it.",
				"Import locations", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		List<SavedLocation> existing = repository.getLocations();
		ImportPlan.Mode mode = askImportMode(parent, incoming.size(), existing.size());
		if (mode == null)
		{
			return;
		}

		ImportPlan plan = ImportPlan.create(existing, incoming, mode);
		// Replace throws away everything the user had, so it gets a backup first.
		repository.replaceAll(plan.getResult(), mode == ImportPlan.Mode.REPLACE);
		afterChange.run();

		JOptionPane.showMessageDialog(parent, plan.describeOutcome(),
			"Import locations", JOptionPane.INFORMATION_MESSAGE);
	}

	/** @return the chosen mode, or null when cancelled */
	private ImportPlan.Mode askImportMode(Component parent, int incoming, int existing)
	{
		JRadioButton merge = new JRadioButton("Merge - keep mine, add new", true);
		JRadioButton replace = new JRadioButton("Replace - discard mine");

		ButtonGroup group = new ButtonGroup();
		group.add(merge);
		group.add(replace);

		JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
		panel.add(new JLabel(ImportMessages.importCounts(incoming, existing)));

		// The large-file caution is a line in this dialog rather than a second dialog.
		String caution = ImportMessages.largeImportCaution(incoming);
		if (caution != null)
		{
			panel.add(new JLabel(caution));
		}

		panel.add(merge);
		panel.add(replace);

		int choice = JOptionPane.showConfirmDialog(parent, panel, "Import locations",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (choice != JOptionPane.OK_OPTION)
		{
			return null;
		}
		return replace.isSelected() ? ImportPlan.Mode.REPLACE : ImportPlan.Mode.MERGE;
	}

	private JFileChooser newChooser(String title)
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(title);
		chooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
		return chooser;
	}

	private File lastDirectory()
	{
		String remembered =
			configManager.getConfiguration(WorldGridConfig.GROUP, "lastFileDirectory");
		if (remembered != null && !remembered.isEmpty())
		{
			File directory = new File(remembered);
			if (directory.isDirectory())
			{
				return directory;
			}
		}
		return new File(System.getProperty("user.home"));
	}

	private void rememberDirectory(File chosen)
	{
		File directory = chosen.getParentFile();
		if (directory != null)
		{
			configManager.setConfiguration(WorldGridConfig.GROUP, "lastFileDirectory",
				directory.getAbsolutePath());
		}
	}
}
