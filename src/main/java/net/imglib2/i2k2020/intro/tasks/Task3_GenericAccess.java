package net.imglib2.i2k2020.intro.tasks;

import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.util.ValuePair;

public class Task3_GenericAccess {

	/**
	 * Using an Iterable and generic types to compute the maximal value in an
	 * image
	 * 
	 * @param iterable
	 * @return
	 */
	public static <T extends Comparable<T> & Type<T>> T max(final Iterable<T> iterable) {
		final T currentMax = iterable.iterator().next().copy();

		for (final T pixel: iterable) {
			if (pixel.compareTo(currentMax) > 0) {
				currentMax.set(pixel);
			}
		}

		return currentMax;
	}

	/**
	 * Using an IterableInterval and generic types to compute the maximal value
	 * in an image
	 * 
	 * @param iterable
	 * @return
	 */
	public static <T extends Comparable<T> & Type<T>> Pair<T, long[]> maxWithLocation(final IterableInterval<T> iterable) {

		final Cursor<T> cursor = iterable.cursor();
		final T currentMax = cursor.next().copy();
		long[] position = new long[2];
		cursor.localize(position);

		while (cursor.hasNext()) {
			cursor.fwd();
			if (cursor.get().compareTo(currentMax) > 0) {
				currentMax.set(cursor.get());
				cursor.localize(position);
			}
		}

		return new ValuePair<>(currentMax, position);
	}

	/**
	 * Using a RandomAccessibleInterval and generic types to return the value in
	 * the center of an image
	 * 
	 * @param rai
	 * @return
	 */
	public static <T> T centerValue(final RandomAccessibleInterval<T> rai) {

		// create a RandomAccess
		// set it to the center pixel in all dimensions
		long[] centerPosition = new long[rai.numDimensions()];
		for (int d=0; d<rai.numDimensions(); ++d) {
			centerPosition[d] = rai.dimension(d) / 2;
		}

		return rai.getAt(centerPosition);
	}

	public static void main(String[] args) {

		// Img implements RandomAccessibleInterval and IterableInterval
		final Img<FloatType> imgF = Task1_CreateImg.createNewImg();

		// compute the maximal value
		System.out.println("\nMax value: " + max(imgF));

		// compute the maximal value and it's location
		final Pair<FloatType, long[]> max = maxWithLocation(imgF);
		System.out.println("\nMax value: " + max.getA() + " @ " + Util.printCoordinates(max.getB()));

		// output the value in the center of the image
		System.out.println("\nCenter value: " + centerValue(imgF));
	}
}
