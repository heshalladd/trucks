

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

public class MPRAlgorithmTests {

	@Test
	public void test() {
		Integer[] intsPrime = { 6, 4, 5, 7, 8 };
		Integer[] ints1 = { 4, 2, 3, 5, 6 };
		Integer[] ints2 = { 5, 3, 4, 6, 7 };
		Integer[] ints3 = { 7, 5, 6, 8, 9 };
		Integer[] ints4 = { 8, 6, 7, 9, 10 };
		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints2)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints3)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints4)));
		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors, foriegnLists);
		
		Integer[] expected = { 4,8 };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		//assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		assertTrue("Incorrect Values",actualResults.equals(expectedResults));
	}

}
