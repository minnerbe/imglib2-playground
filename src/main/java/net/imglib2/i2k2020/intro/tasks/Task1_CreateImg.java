package net.imglib2.i2k2020.intro.tasks;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.array.ArrayImgFactory;

import java.util.Random;

/**
 * Illustrates how to use an array in ImgLib2, how to use iteration, random
 * access
 * 
 * @author Stephan Preibisch
 *
 */
public class Task1_CreateImg {

	/**
	 * Use ArrayImg to create a 5x5 float image and fill with random numbers,
	 * then print out its values and locations. Finally set the center pixel to
	 * 1000 and print out again.
	 *
	 * @return
	 */
	public static Img<FloatType> createNewImg() {

		// create a 5x5 pixel image of type float
		final ImgFactory<FloatType> imgFactory = new ArrayImgFactory<FloatType>(new FloatType());
		final Img<FloatType> img = imgFactory.create(5, 5);

		// generate a random number generator
		Random rnd = new Random(System.currentTimeMillis());

		// iterate all pixels of that image and set it to a random number
		for(FloatType pixel: img) {
			pixel.set(rnd.nextFloat());
		}

		printImageWithPositions(img);
		System.out.println("");

		// random access to a central pixel
		RandomAccess<FloatType> randomAccess = img.randomAccess();
		FloatType value = randomAccess.setPositionAndGet(2, 2);
		System.out.format("central pixel @ (2,2) = %f.\n", value.get());
		
		return null;
	}

	/**
	 * Create an image from an existing float[] array and print out their
	 * locations and values.
	 * 
	 * @return
	 */
	public static Img<FloatType> createImgFromArray() {

		final float[] array = new float[5 * 5];

		for (int i = 0; i < array.length; ++i) {
			array[i] = i;
		}

		final Img<FloatType> img = ArrayImgs.floats(array, 5, 5);

		printImageWithPositions(img);

		return img;
	}
	
	/**
	 * Print out all pixel values together with their position using a
	 * Cursor.
	 *
	 * @ param img Image to be printed.
	 * 		
	 */
	public static void printImageWithPositions(Img img) {
		Cursor<FloatType> cursor = img.localizingCursor();
		int[] position = new int[2];
		while(cursor.hasNext()) {
			cursor.fwd();
			cursor.localize(position);
			System.out.format("img(%d,%d) = %f.\n", position[0], position[1], cursor.get().get());
		}
	}

	public static void main(String[] args) {

		// create a new image, iterate and perform random access
		createNewImg();

		// create a new image from an existing float[] array
		createImgFromArray();
	}
}
