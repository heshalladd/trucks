
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

public class MPRAlgorithmTests {

	@Test
	public void testNode1() {
		Integer[] intsPrime = { 1, 2, 3 };
		Integer[] ints1 = { 2, 1, 3, 4 };
		Integer[] ints2 = { 3, 1, 2, 4, 5 };

		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints2)));

		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = { 3 };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		System.out.println(actualResults);
		assertTrue("Incorrect Values", actualResults.equals(expectedResults));
	}

	@Test
	public void testNode2() {
		Integer[] intsPrime = { 2, 1, 3, 4 };
		Integer[] ints1 = { 1, 2, 3 };
		Integer[] ints2 = { 3, 1, 2, 4, 5 };
		Integer[] ints3 = { 4, 2, 3, 5, 6 };
		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints2)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints3)));

		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = { 4 };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		System.out.println(actualResults);
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		assertTrue("Incorrect Values", actualResults.equals(expectedResults));
	}

	@Test
	public void testNode3() {
		Integer[] intsPrime = { 3, 1, 2, 4, 5 };
		Integer[] ints1 = { 1, 2, 3 };
		Integer[] ints2 = { 2, 1, 3, 4 };
		Integer[] ints3 = { 4, 2, 3, 5, 6 };
		Integer[] ints4 = { 5, 3, 4, 6, 7 };
		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints2)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints3)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints4)));
		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = { 5 };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		System.out.println(actualResults);
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		assertTrue("Incorrect Values", actualResults.equals(expectedResults));
	}

	@Test
	public void testNode4() {
		Integer[] intsPrime = { 4, 2, 3, 5, 6 };
		Integer[] ints1 = { 2, 1, 3, 4 };
		Integer[] ints2 = { 3, 1, 2, 4, 5 };
		Integer[] ints3 = { 5, 3, 4, 6, 7 };
		Integer[] ints4 = { 6, 4, 5, 7, 8 };
		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints2)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints3)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints4)));
		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = { 6, 3 };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		System.out.println(actualResults);
		assertTrue("Incorrect Values: " + actualResults,
				actualResults.equals(expectedResults));
	}

	@Test
	public void testNode5() {
		Integer[] intsPrime = { 5, 3, 4, 6, 7 };
		Integer[] ints1 = { 3, 1, 2, 4, 5 };
		Integer[] ints2 = { 4, 2, 3, 5, 6 };
		Integer[] ints3 = { 6, 4, 5, 7, 8 };
		Integer[] ints4 = { 7, 5, 6, 8, 9 };
		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints2)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints3)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints4)));
		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = { 7, 3 };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		System.out.println(actualResults);
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		assertTrue("Incorrect Values", actualResults.equals(expectedResults));
	}
	
	@Test
	public void testNode6() {
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
		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = { 8, 4 };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		System.out.println(actualResults);
		assertTrue("Incorrect Values", actualResults.equals(expectedResults));
	}

	@Test
	public void testNode7() {
		Integer[] intsPrime = { 7, 5, 6, 8, 9 };
		Integer[] ints1 = { 5, 3, 4, 6, 7 };
		Integer[] ints2 = { 6, 4, 5, 7, 8 };
		Integer[] ints3 = { 8, 6, 7, 9, 10 };
		Integer[] ints4 = { 9, 7, 8, 10 };
		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints2)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints3)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints4)));
		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = { 5, 9 };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		System.out.println(actualResults);
		assertTrue("Incorrect Values: " + actualResults,
				actualResults.equals(expectedResults));
	}
	
	@Test
	public void testNode8() {
		Integer[] intsPrime = { 8, 6, 7, 9, 10 };
		Integer[] ints1 = { 6, 4, 5, 7, 8 };
		Integer[] ints2 = { 7, 5, 6, 8, 9 };
		Integer[] ints3 = { 9, 7, 8, 10};
		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints2)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints3)));

		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = { 6 };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		System.out.println(actualResults);
		assertTrue("Incorrect Values: " + actualResults,
				actualResults.equals(expectedResults));
	}
	
	@Test
	public void testNode9() {
		Integer[] intsPrime = { 9, 7, 8, 10};
		Integer[] ints1 = { 7, 5, 6, 8, 9 };
		Integer[] ints2 = { 8, 6, 7, 9, 10 };
		Integer[] ints3 = { 10, 8, 9};
		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints2)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints3)));

		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = { 7 };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		System.out.println(actualResults);
		assertTrue("Incorrect Values: " + actualResults,
				actualResults.equals(expectedResults));
	}

	@Test
	public void testNode10() {
		Integer[] intsPrime = { 10, 8, 9};
		Integer[] ints1 = { 8, 6, 7, 9, 10 };
		Integer[] ints2 = { 9, 7, 8, 10 };

		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints2)));


		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = { 8 };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		System.out.println(actualResults);
		assertTrue("Incorrect Values: " + actualResults,
				actualResults.equals(expectedResults));
	}
	
	@Test
	public void testNode10OrderVaration() {
		Integer[] intsPrime = { 10, 8, 9};
		Integer[] ints2 = { 9, 7, 8, 10 };
		Integer[] ints1 = { 8, 6, 7, 9, 10 };


		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints2)));


		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = { 8 };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		System.out.println(actualResults);
		assertTrue("Incorrect Values: " + actualResults,
				actualResults.equals(expectedResults));
	}
	
	@Test
	public void testNode6OrderVariationReverse() {
		Integer[] intsPrime = { 6, 4, 5, 7, 8 };
		Integer[] ints4 = { 4, 2, 3, 5, 6 };
		Integer[] ints3 = { 5, 3, 4, 6, 7 };
		Integer[] ints2 = { 7, 5, 6, 8, 9 };
		Integer[] ints1 = { 8, 6, 7, 9, 10 };
		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints2)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints3)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints4)));
		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = { 4,8 };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		System.out.println(actualResults);
		assertTrue("Incorrect Values: "+actualResults, actualResults.equals(expectedResults));
	}
	
	@Test
	public void testNode6OrderVariationMixed() {
		Integer[] intsPrime = { 6, 4, 5, 7, 8 };
		Integer[] ints3 = { 4, 2, 3, 5, 6 };
		Integer[] ints4 = { 5, 3, 4, 6, 7 };
		Integer[] ints1 = { 7, 5, 6, 8, 9 };
		Integer[] ints2 = { 8, 6, 7, 9, 10 };
		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints2)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints3)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints4)));
		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = { 4,8 };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		System.out.println(actualResults);
		assertTrue("Incorrect Values: "+actualResults, actualResults.equals(expectedResults));
	}
	
	@Test
	public void testSoloNode() {
		Integer[] intsPrime = { 6 };

		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();

		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = { };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		System.out.println(actualResults);
		assertTrue("Incorrect Values: "+actualResults, actualResults.equals(expectedResults));
	}
	
	@Test
	public void testTwoNodesOnly() {
		Integer[] intsPrime = { 6, 4};
		Integer[] ints1 = { 4, 6 };

		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));

		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = {  };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		System.out.println(actualResults);
		assertTrue("Incorrect Values: "+actualResults, actualResults.equals(expectedResults));
	}
	
	@Test
	public void testSomeEmptySets() {
		Integer[] intsPrime = { 6, 4, 5, 7, 8 };
		Integer[] ints0 = {};
		Integer[] ints1 = {};
		Integer[] ints2 = {};
		Integer[] ints3 = {};
		Integer[] ints4 = { 4, 2, 3, 5, 6 };
		Integer[] ints5 = { 5, 3, 4, 6, 7 };
		Integer[] ints6 = {};
		Integer[] ints7 = { 7, 5, 6, 8, 9 };
		Integer[] ints8 = { 8, 6, 7, 9, 10 };
		Integer[] ints9 = {};
		Integer[] ints10 = {};
		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints0)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints2)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints3)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints4)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints5)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints6)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints7)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints8)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints9)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints10)));

		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = { 8,4 };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		System.out.println(actualResults);
		assertTrue("Incorrect Values: "+actualResults, actualResults.equals(expectedResults));
	}
	
	@Test
	public void testSingleWithEmptySets() {
		Integer[] intsPrime = { 6};
		Integer[] ints0 = {};
		Integer[] ints1 = {};
		Integer[] ints2 = {};
		Integer[] ints3 = {};
		Integer[] ints4 = {};
		Integer[] ints5 = {};
		Integer[] ints6 = {};
		Integer[] ints7 = {};
		Integer[] ints8 = {};
		Integer[] ints9 = {};
		Integer[] ints10 = {};

		ArrayList<Integer> localNeighbors = new ArrayList<Integer>(
				Arrays.asList(intsPrime));
		
		ArrayList<ArrayList<Integer>> foriegnLists = new ArrayList<ArrayList<Integer>>();
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints0)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints1)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints2)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints3)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints4)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints5)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints6)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints7)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints8)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints9)));
		foriegnLists.add(new ArrayList<Integer>(Arrays.asList(ints10)));
		ArrayList<Integer> actualResults = MPRAlgorithm.getMPR(localNeighbors,
				foriegnLists);

		Integer[] expected = {  };
		ArrayList<Integer> expectedResults = new ArrayList<Integer>(
				Arrays.asList(expected));
		// assertEquals("Incorrect Values",actualResults.size(),expectedResults.size());
		System.out.println(actualResults);
		assertTrue("Incorrect Values: "+actualResults, actualResults.equals(expectedResults));
	}
}
