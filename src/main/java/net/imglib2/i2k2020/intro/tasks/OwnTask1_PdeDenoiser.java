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
import java.lang.reflect.Array;

/**
 * Illustrates how to implement PDE based methods (e.g., a denoiser) in
 * imglib2.
 *
 * @author Michael Innerberger
 *
 */
public class OwnTask1_PdeDenoiser {

	/**
	 * Compute finite differences in d-th dimension.
	 *
	 * @param source	Source image.
	 * @param target	Target image.
	 * @param d			Dimension along which differences are to be computed.
	 */
	private static <T extends NumericType<T> & NativeType<T>> void computeFiniteDifferences(Img<T> source, Img<T> target, int d) {
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
	 * Compute length of discrete gradient.
	 *
	 * @param 	gradient	
	 *
	 * @return 	squared length of gradient
	 */
	private static <T extends NumericType<T> & NativeType<T>> Img<T> computeGradientLength(Img<T>[] gradient) {
		int n = gradient.length;
		final Img<T> duSquared = gradient[0].factory().create(gradient[0]);
		final Cursor<T> cursorDu = duSquared.cursor();
		final Cursor<T>[] cursorComponent = (Cursor<T>[]) Array.newInstance(cursorDu.getClass(), n);

		for (int i=0; i<n; ++i) {
			cursorComponent[i] = gradient[i].cursor();
		}

		T val = null;
		T component = null;
		while (cursorDu.hasNext()) {
			val = cursorDu.next();
			val.setZero();
			for (int i=0; i<n; ++i) {
				component = cursorComponent[i].next();
				component.mul(component);
				val.add(component);
			}
		}

		return duSquared;
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
		ImageJFunctions.show(img);

		final Img<T>[] gradient = (Img<T>[]) Array.newInstance(img.getClass(), 2);
		for (int i=0; i<2; ++i) {
			gradient[i] = img.factory().create(img);
			computeFiniteDifferences(img, gradient[i], i);
			ImageJFunctions.show(gradient[i]);
		}

		final Img<T> duSquared = computeGradientLength(gradient);
		ImageJFunctions.show(duSquared);
	}
}
