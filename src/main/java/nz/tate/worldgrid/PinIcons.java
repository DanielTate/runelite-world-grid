package nz.tate.worldgrid;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * The two map pin images.
 *
 * <p>Saved and searched pins must be told apart at a glance -- "is this saved yet?" is
 * otherwise unanswerable -- so they differ in both shape and colour. They are drawn once
 * at class load rather than shipped as files, since they are a circle and a diamond.
 */
final class PinIcons
{
	private static final int SIZE = 13;

	/** Solid cyan disc: a place already in your library. */
	static final BufferedImage SAVED = drawDisc();

	/** Hollow yellow diamond: a searched reference, not saved yet. */
	static final BufferedImage SEARCH = drawDiamond();

	private PinIcons()
	{
	}

	private static BufferedImage drawDisc()
	{
		BufferedImage image = blank();
		Graphics2D graphics = start(image);
		graphics.setColor(new Color(0, 190, 240));
		graphics.fillOval(1, 1, SIZE - 3, SIZE - 3);
		graphics.setColor(Color.BLACK);
		graphics.drawOval(1, 1, SIZE - 3, SIZE - 3);
		graphics.dispose();
		return image;
	}

	private static BufferedImage drawDiamond()
	{
		BufferedImage image = blank();
		Graphics2D graphics = start(image);
		int mid = SIZE / 2;
		int[] x = {mid, SIZE - 2, mid, 1};
		int[] y = {1, mid, SIZE - 2, mid};

		graphics.setColor(new Color(0, 0, 0, 160));
		graphics.fillPolygon(x, y, 4);
		graphics.setColor(new Color(255, 210, 60));
		graphics.setStroke(new BasicStroke(2f));
		graphics.drawPolygon(x, y, 4);
		graphics.dispose();
		return image;
	}

	private static BufferedImage blank()
	{
		return new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
	}

	private static Graphics2D start(BufferedImage image)
	{
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
		return graphics;
	}
}
