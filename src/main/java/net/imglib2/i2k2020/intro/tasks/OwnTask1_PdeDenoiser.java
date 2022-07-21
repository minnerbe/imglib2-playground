package net.imglib2.i2k2020.intro.tasks;

import ij.IJ;
import net.imglib2.img.Img;
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

	}
}
