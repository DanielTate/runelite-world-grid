package nz.tate.worldgrid.core;

/**
 * Outcome of parsing a typed grid reference.
 *
 * <p>The failure reason is a distinct value rather than only a message so that callers
 * can react differently and tests can assert on the cause rather than on wording.
 */
public final class GridRefParseResult
{
	public enum Failure
	{
		EMPTY,
		BAD_FORMAT,
		COLUMN_OUT_OF_RANGE,
		PLANE_OUT_OF_RANGE,
		OUTSIDE_WORLD
	}

	private final GridCell cell;
	private final Failure failure;
	private final String message;

	private GridRefParseResult(GridCell cell, Failure failure, String message)
	{
		this.cell = cell;
		this.failure = failure;
		this.message = message;
	}

	public static GridRefParseResult success(GridCell cell)
	{
		return new GridRefParseResult(cell, null, null);
	}

	public static GridRefParseResult failure(Failure failure, String message)
	{
		return new GridRefParseResult(null, failure, message);
	}

	public boolean isSuccess()
	{
		return cell != null;
	}

	public GridCell getCell()
	{
		return cell;
	}

	public Failure getFailure()
	{
		return failure;
	}

	/** Human-readable reason, suitable for showing in the chatbox. Null on success. */
	public String getMessage()
	{
		return message;
	}
}
