package nz.tate.worldgrid.core;

/**
 * Renders a cell as the reference players say out loud, such as {@code L35} or
 * {@code GA12+2}.
 *
 * <p>Letters encode the row and increase going north, so row A is southernmost.
 * Numbers encode the column and increase going east. Planes above ground are appended
 * as {@code +1} to {@code +3}; ground level has no suffix.
 */
public final class GridRefFormatter
{
	private static final int ALPHABET_SIZE = 26;

	private GridRefFormatter()
	{
	}

	/**
	 * @return the reference, or null if the cell sits outside the grid
	 */
	public static String format(GridCell cell)
	{
		if (cell.isNegative())
		{
			return null;
		}

		StringBuilder reference = new StringBuilder();
		reference.append(toLetters(cell.getRow()));
		reference.append(cell.getColumn() + 1);

		if (cell.getPlane() > 0)
		{
			reference.append('+').append(cell.getPlane());
		}
		return reference.toString();
	}

	/**
	 * Converts a zero-based row index to spreadsheet-style letters: 0 is A, 25 is Z,
	 * 26 is AA. Counting starts at one internally because the sequence has no zero
	 * digit, the same way spreadsheet columns work.
	 */
	public static String toLetters(int rowIndex)
	{
		if (rowIndex < 0)
		{
			throw new IllegalArgumentException("Row index cannot be negative: " + rowIndex);
		}

		StringBuilder letters = new StringBuilder();
		int remaining = rowIndex + 1;

		while (remaining > 0)
		{
			int digit = (remaining - 1) % ALPHABET_SIZE;
			letters.append((char) ('A' + digit));
			remaining = (remaining - 1) / ALPHABET_SIZE;
		}
		return letters.reverse().toString();
	}

	/** Inverse of {@link #toLetters(int)}. Input must already be upper case. */
	public static int fromLetters(String letters)
	{
		int index = 0;
		for (int i = 0; i < letters.length(); i++)
		{
			index = index * ALPHABET_SIZE + (letters.charAt(i) - 'A' + 1);
		}
		return index - 1;
	}
}
