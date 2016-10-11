package com.lagecompany.woc.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

class PerformanceItem {
	int min;
	int max;
	int avg;
	int count;
	int total;
}
/*
 * [PERF][MergeVisibleFaces] avg: 323, count: 1668, total: 540409, min: 133, max: 6196 
 * [PERF][MergeVisibleFaces] avg: 348, count: 1668, total: 580660, min: 129, max: 5618
 * [PERF][MergeVisibleFaces] avg: 344, count: 1668, total: 574517, min: 128, max: 8481
 */
public class PerformanceTrack {
	private static Map<String, PerformanceItem> itemMap = new HashMap<>();
	private static ThreadLocal<Long> currentTest = new ThreadLocal<>();

	public static void begin(String name) {
		currentTest.set(System.nanoTime());
	}

	public static void end(String name) {
		int elapsed = (int) ((System.nanoTime() - currentTest.get()) / 1000);

		PerformanceItem item = itemMap.get(name);
		if (item == null) {
			item = new PerformanceItem();
			item.min = elapsed;
			item.max = elapsed;
			item.avg = elapsed;
			item.count = 1;

			itemMap.put(name, item);
		} else {
			if (elapsed < item.min) {
				item.min = elapsed;
			}
			if (elapsed > item.max) {
				item.max = elapsed;
			}
			item.count++;
			item.total += elapsed;
			item.avg = (int) ((item.total) / item.count);
		}
	}

	public static void printResults() {
		for (Entry<String, PerformanceItem> entry : itemMap.entrySet()) {
			PerformanceItem item = entry.getValue();
			System.out.println("[PERF][" + entry.getKey() + "] avg: " + item.avg + ", count: " + item.count
					+ ", total: " + item.total + ", min: " + item.min + ", max: " + item.max);
		}
	}

}
