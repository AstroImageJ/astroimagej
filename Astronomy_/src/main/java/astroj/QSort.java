package astroj;// QSort.java  : quick hack based on ij.util.StringSorter

/** A simple QuickSort for double arrays. */
public class QSort
	{

	/** Sorts the array. */
	public static void main(String[] a)
		{
		double[] num = new double[a.length];
		for (int i=0; i < a.length; i++)
			num[i] = Double.parseDouble(a[i]);
		if (!alreadySorted(num))
			sort(num, 0, num.length - 1);
		for (int i=0; i < num.length; i++)
			System.out.println(num[i]);
		}
	
	static void sort(double[] a, int from, int to)
		{
		int i = from, j = to;
		double center = a[ (from + to) / 2 ];
		do	{
			while ( i < to && center > a[i]) i++;
			while ( j > from && center < a[j]) j--;
			if (i < j)
				{
				double temp = a[i];
				a[i] = a[j];
				a[j] = temp;
				}
			if (i <= j) { i++; j--; }
			} while(i <= j);
		if (from < j) sort(a, from, j);
		if (i < to) sort(a,  i, to);
		}
		
	static boolean alreadySorted(double[] a)
		{
		for ( int i=1; i<a.length; i++ )
			{
			if (a[i] < a[i-1]) return false;
			}
		return true;
		}

	static double[] sortedCopy (double[] a)
		{
		if (a == null || a.length == 0) return a;
		double[] b = new double[a.length];
		for (int i=0; i < a.length; i++)
			b[i] = a[i];
		if (alreadySorted(b)) return b;
		sort (b,0,b.length-1);
		return b;
		}
	}
