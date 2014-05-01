package edu.auburn.scottandjoe;

import java.util.Arrays;

public class RollingAverage {
	private int size;
	private int numEntries;
	private double total = 0d;
	private int index = 0;
	private double samples[];

	public RollingAverage(int size) {
		this.size = size;
		samples = new double[size];
		Arrays.fill(samples, 0.0);
	}

	public void add(double x) {
		total -= samples[index];
		samples[index] = x;
		total += x;
		if (numEntries < size) {
			numEntries++;
		}
		if (++index == size) {
			index = 0;
		}
	}

	public double getAverage() {
		return total / numEntries;
	}
}
