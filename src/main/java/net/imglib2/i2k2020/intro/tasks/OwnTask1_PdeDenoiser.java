package net.imglib2.i2k2020.intro.tasks;

import ij.IJ;
import net.imglib2.img.Img;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccess;
import net.imglib2.Cursor;
import net.imglib2.view.Views;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.NativeType;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImgs;

import java.util.Random;

/**
 * Illustrates how to use an array in ImgLib2, how to use iteration, random
 * access
 * 
 * @author Stephan Preibisch
 *
 */
public class OwnTask1_PdeDenoiser {

	/**
	 * Compute finite differences in d-th dimension.
	 *
	 * @param source	Source image.
	 * @param d			Dimension along which differences are to be computed.
	 * @param target	Target image.
	 */
	private static <T extends NumericType<T> & NativeType<T>> void computeFiniteDifferences(Img<T> source, int d, Img<T> target) {
		final RandomAccessible<T> infiniteSource = Views.extendMirrorSingle(source);
		final RandomAccess<T> randomAccess = infiniteSource.randomAccess();
		final Cursor<T> sourceCursor = source.localizingCursor();
		final Cursor<T> targetCursor = target.localizingCursor();
		final long[] position = new long[source.numDimensions()];
		T diff = null;

		while (sourceCursor.hasNext()) {
			sourceCursor.fwd();
			targetCursor.fwd();

			// compute difference of pixel with position +1/-1 in dimension d
			sourceCursor.localize(position);
			position[d] += 1;
			diff = randomAccess.setPositionAndGet(position).copy();
			position[d] -= 2;
			diff.sub(randomAccess.setPositionAndGet(position));

			targetCursor.get().set(diff);
		}
	}

	/**
	 * Use ArrayImg to create a 5x5 float image and fill with random numbers,
	 *
	 * @return
	 */
	public static <T extends NumericType<T> & NativeType<T>> void main(String[] args) {
		// open grayscale image
		final String imgLocation = "/home/michael/data/Programming/Java/imglib2-intro/pictures/noisy-image.png";
		// final String imgLocation = "/home/michael/data/Programming/Java/imglib2-intro/pictures/plane-with-noise.png";
		final Img<T> img = ImagePlusImgs.from(IJ.openImage(imgLocation));
		final Img<T> diffx = img.factory().create(img);
		final Img<T> diffy = img.factory().create(img);

		computeFiniteDifferences(img, 0, diffx);
		computeFiniteDifferences(img, 1, diffy);
		ImageJFunctions.show(img);
		ImageJFunctions.show(diffx);
		ImageJFunctions.show(diffy);

	}
}
