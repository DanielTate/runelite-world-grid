package nz.tate.worldgrid;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import nz.tate.worldgrid.core.LocationCodec;
import nz.tate.worldgrid.core.LocationReadResult;
import nz.tate.worldgrid.core.SavedLocation;

/**
 * Holds the saved locations and owns the file they live in.
 *
 * <p>Disk work never runs on the client thread. Reads and writes go to a single
 * background thread and the resulting list is published back on the Swing event thread,
 * which is where the panel and every edit live.
 *
 * <p>The in-memory list is always replaced wholesale by an unmodifiable copy rather than
 * mutated in place, so a reader on another thread can never see a half-applied edit.
 */
@Slf4j
@Singleton
public class LocationRepository
{
	private static final String DIRECTORY_NAME = "world-grid";
	private static final String FILE_NAME = "locations.json";

	private final LocationCodec codec;
	private final File directory;
	private final File workingFile;

	private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

	private volatile List<SavedLocation> locations = Collections.emptyList();
	private volatile String loadError;

	private ExecutorService executor;
	private Future<?> pendingLoad;

	@Inject
	LocationRepository(Gson gson)
	{
		this.codec = new LocationCodec(gson);
		this.directory = new File(RuneLite.RUNELITE_DIR, DIRECTORY_NAME);
		this.workingFile = new File(directory, FILE_NAME);
	}

	/** Starts the worker and kicks off the initial read. Never blocks. */
	public void startUp()
	{
		executor = Executors.newSingleThreadExecutor();
		pendingLoad = executor.submit(this::readWorkingFile);
	}

	public void shutDown()
	{
		if (pendingLoad != null)
		{
			pendingLoad.cancel(true);
			pendingLoad = null;
		}
		if (executor != null)
		{
			executor.shutdownNow();
			executor = null;
		}
		listeners.clear();
		locations = Collections.emptyList();
	}

	/** Called on the Swing event thread whenever the list changes. */
	public void addListener(Runnable listener)
	{
		listeners.add(listener);
	}

	public List<SavedLocation> getLocations()
	{
		return locations;
	}

	/** Why the working file could not be read, or null when all is well. */
	public String getLoadError()
	{
		return loadError;
	}

	public File getDirectory()
	{
		return directory;
	}

	/**
	 * Replaces the whole list and queues a write. Call from the Swing event thread.
	 *
	 * @param backupFirst true for destructive changes, so a mis-click stays recoverable
	 */
	public void replaceAll(List<SavedLocation> next, boolean backupFirst)
	{
		locations = Collections.unmodifiableList(next);
		notifyListeners();

		final String json = codec.write(locations);
		submit(() ->
		{
			if (backupFirst)
			{
				writeBackup();
			}
			writeWorkingFile(json);
		});
	}

	/** Reads a file the user picked. Runs on the worker; the result comes back on the EDT. */
	public void readImportFile(File file, Consumer<LocationReadResult> onResult)
	{
		submit(() ->
		{
			LocationReadResult result = readFile(file);
			SwingUtilities.invokeLater(() -> onResult.accept(result));
		});
	}

	/** Writes a file the user picked. The outcome message comes back on the EDT. */
	public void writeExportFile(File file, List<SavedLocation> toExport,
		Consumer<String> onError)
	{
		final String json = codec.write(toExport);
		submit(() ->
		{
			String error = null;
			try
			{
				Files.write(file.toPath(), json.getBytes(StandardCharsets.UTF_8));
			}
			catch (IOException e)
			{
				log.warn("World Grid could not export locations", e);
				error = e.getMessage();
			}

			final String reported = error;
			SwingUtilities.invokeLater(() -> onError.accept(reported));
		});
	}

	private void submit(Runnable task)
	{
		ExecutorService current = executor;
		if (current == null)
		{
			return;
		}
		current.submit(task);
	}

	private void notifyListeners()
	{
		for (Runnable listener : listeners)
		{
			listener.run();
		}
	}

	private void readWorkingFile()
	{
		if (!workingFile.exists())
		{
			// A first run is not an error -- there is simply nothing saved yet.
			return;
		}

		LocationReadResult result = readFile(workingFile);
		SwingUtilities.invokeLater(() ->
		{
			if (result.isSuccess())
			{
				loadError = null;
				locations = Collections.unmodifiableList(result.getLocations());
			}
			else
			{
				// The file is left untouched so the user can inspect or repair it.
				loadError = result.getError();
				log.warn("World Grid could not read {}: {}", workingFile, result.getError());
			}
			notifyListeners();
		});
	}

	private LocationReadResult readFile(File file)
	{
		try
		{
			byte[] bytes = Files.readAllBytes(file.toPath());
			return codec.read(new String(bytes, StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			log.warn("World Grid could not read {}", file, e);
			return LocationReadResult.failure("The file could not be read: " + e.getMessage());
		}
	}

	private void writeWorkingFile(String json)
	{
		try
		{
			if (!directory.exists() && !directory.mkdirs())
			{
				log.warn("World Grid could not create {}", directory);
				return;
			}
			Files.write(workingFile.toPath(), json.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			log.warn("World Grid could not save locations", e);
		}
	}

	/** Timestamped copy of the file as it stands, taken before anything destructive. */
	void writeBackup()
	{
		if (!workingFile.exists())
		{
			return;
		}

		String name = LocationEdits.backupFileName(Instant.now(), ZoneId.systemDefault());
		try
		{
			Files.copy(workingFile.toPath(), new File(directory, name).toPath());
			log.debug("World Grid wrote backup {}", name);
		}
		catch (IOException e)
		{
			log.warn("World Grid could not write a backup", e);
		}
	}
}
