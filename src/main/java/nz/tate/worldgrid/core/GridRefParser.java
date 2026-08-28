package nz.tate.worldgrid.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a typed grid reference such as {@code l35}, {@code GA12} or {@code L35+2}.
 *
 * <p>This is the only place a reference is typed, so its validation carries the whole
 * burden of rejecting bad input clearly. Parsing is the exact inverse of
 * {@link GridRefFormatter#format(GridCell)}: formatting a cell then parsing the result
 * must return the same cell.
 */
public final class GridRefParser
{
	private static final Pattern REFERENCE = Pattern.compile("^([A-Z]+)([0-9]+)(?:\\+([0-9]))?$");

	/** Guards against letter runs long enough to overflow while decoding. */
	private static final int MAX_LETTERS = 4;

	private GridRefParser()
	{
	}

	public static GridRefParseResult parse(String input, int gridSize)
	{
		if (input == null || input.trim().isEmpty())
		{
			return GridRefParseResult.failure(
				GridRefParseResult.Failure.EMPTY,
				"Enter a grid reference, for example L35.");
		}

		String cleaned = input.trim().replace(" ", "").toUpperCase();
		Matcher matcher = REFERENCE.matcher(cleaned);
		if (!matcher.matches())
		{
			return GridRefParseResult.failure(
				GridRefParseResult.Failure.BAD_FORMAT,
				"'" + input.trim() + "' is not a grid reference. Try L35, or L35+2 for upstairs.");
		}

		String letters = matcher.group(1);
		if (letters.length() > MAX_LETTERS)
		{
			return GridRefParseResult.failure(
				GridRefParseResult.Failure.BAD_FORMAT,
				"'" + input.trim() + "' has too many letters to be a grid reference.");
		}

		int row = GridRefFormatter.fromLetters(letters);

		// Numbers are one-based on screen, so 1 is the first column.
		long typedColumn = Long.parseLong(matcher.group(2));
		if (typedColumn < 1)
		{
			return GridRefParseResult.failure(
				GridRefParseResult.Failure.COLUMN_OUT_OF_RANGE,
				"Grid columns start at 1.");
		}
		int column = (int) Math.min(typedColumn - 1, Integer.MAX_VALUE);

		int plane = 0;
		if (matcher.group(3) != null)
		{
			plane = Integer.parseInt(matcher.group(3));
			if (plane < 1 || plane > 3)
			{
				return GridRefParseResult.failure(
					GridRefParseResult.Failure.PLANE_OUT_OF_RANGE,
					"Height must be +1, +2 or +3.");
			}
		}

		GridCell cell = new GridCell(column, row, plane);
		if (!isInsideWorld(cell, gridSize))
		{
			return GridRefParseResult.failure(
				GridRefParseResult.Failure.OUTSIDE_WORLD,
				"'" + cleaned + "' is outside the world at grid size " + gridSize + ".");
		}
		return GridRefParseResult.success(cell);
	}

	private static boolean isInsideWorld(GridCell cell, int gridSize)
	{
		// Compute in long so a huge typed column cannot silently wrap around.
		long x = (long) GridAnchor.ANCHOR_X + (long) cell.getColumn() * gridSize;
		long y = (long) GridAnchor.ANCHOR_Y + (long) cell.getRow() * gridSize;
		return x >= 0 && x <= GridAnchor.WORLD_MAX && y >= 0 && y <= GridAnchor.WORLD_MAX;
	}
}
