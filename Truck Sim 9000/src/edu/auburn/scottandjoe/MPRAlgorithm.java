package edu.auburn.scottandjoe;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

public class MPRAlgorithm {
	public static ArrayList<Integer> getMPR(ArrayList<Integer> localNeighbors,
			ArrayList<ArrayList<Integer>> foriegnLists) {
		HashSet<Integer> candidates = new HashSet<Integer>();
		for (ArrayList<Integer> list : foriegnLists) {
			int node = list.remove(0);
			list.removeAll(localNeighbors);
			candidates.addAll(list);
			list.add(node);
		}
		Collections.sort(foriegnLists, new CustomComparator());
		ArrayList<Integer> MPRSet = new ArrayList<Integer>();
		for (ArrayList<Integer> list : foriegnLists) {
			int node = list.remove(list.size() - 1);
			MPRSet.add(node);
			candidates.removeAll(list);
			if (candidates.size() == 0)
				break;
		}
		return MPRSet;
	}
	public static class CustomComparator implements
			Comparator<ArrayList<Integer>> {
		@Override
		public int compare(ArrayList<Integer> list1, ArrayList<Integer> list2) {
			return (list1.size() >= list2.size()) ? -1 : 1;
		}
	}
}
