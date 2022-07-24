package net.imglib2.i2k2020.intro.tasks;

import ij.IJ;
import net.imglib2.img.Img;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Cursor;
import net.imglib2.view.Views;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
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
	 * Compute Perona-Malik flux 1/(1+|Du|^2/lambda^2) Du of discrete gradient.
	 *
	 * @param 	gradient	
	 *
	 * @return 	flux
	 */
	private static <T extends NumericType<T> & NativeType<T>> void computeFlux(Img<T>[] gradient) {
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
			val.setOne(); // TODO: introduce parameter lambda^2 here
			for (int i=0; i<n; ++i) {
				component = cursorComponent[i].next().copy();
				component.mul(component);
				val.add(component);
			}
			val.pow(-1);
			for (int i=0; i<n; ++i) {
				component.mul(val);
				cursorComponent[i].get().set(component);
			}
		}
	}

	/**
	 * Compute Perona-Malik denoising of image.
	 *
	 * @param 	img	
	 * @param 	nTimeSteps	
	 *
	 * @return 	denoised image
	 */
	private static <T extends NumericType<T> & NativeType<T>> Img<T> denoise(Img<T> img, int nTimeSteps) {
		int n = img.numDimensions();
		final Img<T> denoisedImg = copyImage(img);
		final Img<T>[] gradient = (Img<T>[]) Array.newInstance(img.getClass(), n);
		final Img<T>[] flux = null;

		for (int i=0; i<n; ++i) {
			gradient[i] = img.factory().create(img);
		}

		for (int t=0; t<nTimeSteps; ++t) {
			for (int i=0; i<n; ++i) {
				computeFiniteDifferences(img, gradient[i], i);
			}
			computeFlux(gradient);
			for (int i=0; i<n; ++i) {
				computeFiniteDifferences(gradient[i], gradient[i], i);
			}
			addTimeStep(denoisedImg, gradient);
		}

		return denoisedImg;
	}

	/**
	 * Generic, type-agnostic method to create an identical copy of an Img
	 *
	 * @param input - the Img to copy
	 * @return - the copy of the Img
	 */
	private static <T extends NumericType<T> & NativeType<T>> Img<T> copyImage(final Img<T> input) {
		Img<T> output = input.factory().create(input);
		Cursor<T> cursorInput = input.cursor();
		Cursor<T> cursorOutput = output.cursor();
 
		while (cursorInput.hasNext()) {
			cursorInput.fwd();
			cursorOutput.fwd();
			cursorOutput.get().set(cursorInput.get());
		}
 
		return output;
	}

	/**
	 * Add one timestep to the image.
	 *
	 * @param 	img	
	 * @param 	update	
	 */
	private static <T extends NumericType<T> & NativeType<T>> void addTimeStep(Img<T> img, Img<T>[] update) {
		int n = update.length;
		final Cursor<T> cursor = img.cursor();
		final Cursor<T>[] cursorComponent = (Cursor<T>[]) Array.newInstance(cursor.getClass(), n);

		for (int i=0; i<n; ++i) {
			cursorComponent[i] = update[i].cursor();
		}

		T val = null;
		T component = null;
		while (cursor.hasNext()) {
			val = cursor.next();
			for (int i=0; i<n; ++i) {
				component = cursorComponent[i].next();
				val.add(component); // TODO: add step size
			}
		}
	}

	public <T extends RealType<T> & NativeType<T>> OwnTask1_PdeDenoiser() {
		// open grayscale image
		final String imgLocation = "/home/michael/data/Programming/Java/imglib2-intro/pictures/noisy-image.pgm";
		// final String imgLocation = "/home/michael/data/Programming/Java/imglib2-intro/pictures/plane-with-noise.pgm";
		Img<T> img = ImagePlusImgs.from(IJ.openImage(imgLocation));
		ImageJFunctions.show(img);

		denoise(img, 10);

		ImageJFunctions.show(img);

		final PeronaMalikDenoiser pmd = new PeronaMalikDenoiser(1.0, 0.1);
		pmd.denoise(img, 10);
	}

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) {
		new OwnTask1_PdeDenoiser();
	}

	private class PeronaMalikDenoiser {
		private DoubleType alpha;
		private DoubleType beta;
		private RandomAccessibleInterval<DoubleType> currentState;
		private RandomAccessibleInterval<DoubleType>[] flux;

		public PeronaMalikDenoiser(double alpha, double beta) {
			setParameters(alpha, beta);
			currentState = null;
			flux = null;
		}

		public void setParameters(double alpha, double beta) {
			this.alpha = new DoubleType(alpha);
			this.beta = new DoubleType(beta);
		}

		public <T extends RealType<T> & NativeType<T>> Img<T> denoise(Img<T> img, int nSteps) {
			convertImageToState(img);
			for (int k=0; k<nSteps; ++k) {
				computeFlux();
				updateState();
			}
			cleanUp();

			return stateAsImageOfType(img.firstElement().getClass());
		}

		private <T extends RealType<T> & NativeType<T>> void convertImageToState(Img<T> img) {
			currentState = Converters.convert((RandomAccessibleInterval<T>) img, (i, o) -> o.set(i.getRealDouble()), new DoubleType());
		}

		private void computeFlux() {

		}
		
		private void updateState() {

		}
		
		private <T extends RealType<T> & NativeType<T>> Img<T> stateAsImageOfType(Class<?> cls) {

			return null;
		}

		private void cleanUp() {
			flux = null;
		}
	}
}
