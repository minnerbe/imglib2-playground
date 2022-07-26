package net.imglib2.i2k2020.intro.tasks;

import ij.IJ;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Cursor;
import net.imglib2.Point;
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

	public <T extends RealType<T> & NativeType<T>> OwnTask1_PdeDenoiser() {
		// open grayscale image
		final String imgLocation = "/home/michael/data/Programming/Java/imglib2-intro/pictures/noisy-image.pgm";
		// final String imgLocation = "/home/michael/data/Programming/Java/imglib2-intro/pictures/plane-with-noise.pgm";
		Img<T> img = ImagePlusImgs.from(IJ.openImage(imgLocation));
		ImageJFunctions.show(img);

		final PeronaMalikDenoiser pmd = new PeronaMalikDenoiser(1.0, 0.001);
		Img<DoubleType> denoisedImg = pmd.denoise(img, 1000);
		ImageJFunctions.show(denoisedImg);
	}

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) {
		new OwnTask1_PdeDenoiser();
	}

	/**
	 * Compute a denoised image using a PDE-based method suggested by Perona
	 * and Malik: compute the solution to the model
	 * 	d_t u = div(g(|grad u|^2) * grad u),
	 * 	where the flux is g(s) = 1/(1+s/lambda).
	 * The parameter alpha steers the step sizes (alpha = delta t / h) of the
	 * time discretization.  The parameter beta controls the strength of the
	 * diffusion (beta = lambda / h^2).  The spatial step size of the finite
	 * difference scheme is implicitly given by h>0.
	 */
	private class PeronaMalikDenoiser {
		private int numDimensions;
		private DoubleType alpha;
		private DoubleType beta;
		private Img<DoubleType> currentState;
		private Img<DoubleType>[] flux;
		final private DoubleType one = new DoubleType(1.);

		public PeronaMalikDenoiser(double alpha, double beta) {
			setParameters(alpha, beta);
			currentState = null;
			flux = null;
			this.numDimensions = -1;
		}

		public void setParameters(double alpha, double beta) {
			this.alpha = new DoubleType(alpha);
			this.beta = new DoubleType(beta);
		}

		public <T extends RealType<T> & NativeType<T>> Img<DoubleType> denoise(Img<T> img, int nSteps) {
			initializeState(img);
			for (int k=0; k<nSteps; ++k) {
				computeFlux();
				updateState();
			}
			clearTemporaryVariables();

			return currentState;
		}

		private <T extends RealType<T> & NativeType<T>> void initializeState(Img<T> img) {
			numDimensions = img.numDimensions();
			currentState = convertToDouble(img);
			flux = (Img<DoubleType>[]) Array.newInstance(currentState.getClass(), numDimensions);
			for (int k=0; k<numDimensions; ++k) {
				flux[k] = createNewDoubleImageFrom(img);
			}
		}
		
		private <T extends RealType<T> & NativeType<T>> Img<DoubleType> convertToDouble(Img<T> img) {
			currentState = createNewDoubleImageFrom(img);
			final RandomAccessibleInterval<DoubleType> conversionWrapper = Converters.convert(
					(RandomAccessibleInterval<T>) img,
					(i, o) -> o.set(i.getRealDouble()),
					new DoubleType());
			copyValues(conversionWrapper, currentState);
			
			return currentState;
		}

		private void copyValues(RandomAccessibleInterval<DoubleType> source, Img<DoubleType> target) {
			final RandomAccess<DoubleType> sourceAccess = source.randomAccess();
			final Cursor<DoubleType> targetCursor = target.localizingCursor();
			final Point position = new Point(numDimensions);
			while (targetCursor.hasNext()) {
				targetCursor.fwd();
				targetCursor.localize(position);
				targetCursor.get().set(sourceAccess.setPositionAndGet(position));
			}
		}

		private <T extends RealType<T> & NativeType<T>> Img<DoubleType> createNewDoubleImageFrom(Img<T> img) {
			final ImgFactory imgFactory = new ArrayImgFactory(new DoubleType());
			final long[] dimensions = img.maxAsLongArray();
			return imgFactory.create(dimensions);
		}

		/**
		 * Compute discrete flux via finite differences.
		 */
		private void computeFlux() {
			final Cursor<DoubleType> stateCursor = currentState.localizingCursor();
			final Cursor<DoubleType>[] fluxCursor = (Cursor<DoubleType>[]) Array.newInstance(currentState.cursor().getClass(), numDimensions);
			final RandomAccessible<DoubleType> infiniteState = Views.extendMirrorSingle(currentState);
			final RandomAccess<DoubleType> stateAccess = infiniteState.randomAccess();

			for (int k=0; k<numDimensions; ++k) {
				fluxCursor[k] = flux[k].cursor();
			}

			final Point position = new Point(numDimensions);
			DoubleType diff = new DoubleType();
			DoubleType coeff = new DoubleType();

			while (stateCursor.hasNext()) {
				stateCursor.fwd();
				stateCursor.localize(position);
				coeff.setZero();

				for (int d=0; d<numDimensions; ++d) {
					fluxCursor[d].fwd();
					computeDirectionalDerivative(stateAccess, position, d, diff);
					fluxCursor[d].get().setReal(diff.getRealDouble());
					diff.mul(diff);
					coeff.add(diff);
				}
				
				transformDiffusionCoefficient(coeff);

				for (int d=0; d<numDimensions; ++d) {
					fluxCursor[d].get().mul(coeff);
				}
			}
		}

		/**
		 * Perform nonlinear trasformation of gradient norm: s -> 1/(1+s/beta).
		 */
		private void transformDiffusionCoefficient(DoubleType s) {
			s.div(beta);
			s.add(one);
			s.pow(-1);
		}

		/**
		 * Compute finite differences in specified dimension (difference of
		 * pixels with position +1/-1 in specified dimension).
		 */
		private void computeDirectionalDerivative(RandomAccess<DoubleType> access, Point position, int dimension, DoubleType result) {
				position.move(1, dimension);
				result.setReal(access.setPositionAndGet(position).getRealDouble());
				position.move(-2, dimension);
				result.sub(access.setPositionAndGet(position));
				position.move(1, dimension);
		}
		
		/**
		 * Add one timestep to the image.
		 */
		private void updateState() {
			final Cursor<DoubleType> stateCursor = currentState.localizingCursor();
			final RandomAccess<DoubleType>[] fluxAccess =
			(RandomAccess<DoubleType>[]) Array.newInstance(Views.extendMirrorSingle(flux[0]).randomAccess().getClass(), numDimensions);

			for (int k=0; k<numDimensions; ++k) {
				fluxAccess[k] = Views.extendMirrorSingle(flux[k]).randomAccess();
			}

			final Point position = new Point(numDimensions);
			DoubleType diff = new DoubleType();

			while (stateCursor.hasNext()) {
				stateCursor.fwd();
				stateCursor.localize(position);
				diff.setZero();

				for (int d=0; d<numDimensions; ++d) {
					computeDirectionalDerivative(fluxAccess[d], position, d, diff);
					diff.mul(alpha);
					stateCursor.get().add(diff);
				}
			}
		}
		
		private void clearTemporaryVariables() {
			flux = null;
		}
	}
}
