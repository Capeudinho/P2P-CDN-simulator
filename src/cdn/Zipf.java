package cdn;

public final class Zipf
{
	private static int N;
	private static double a;
	private static double[] cdf;

	public static void init(int n, double alpha)
	{
		N = n;
		a = alpha;
		double sum = 0.0;
		double[] w = new double[N+1];
		for (int i = 1; i <= N; i++)
		{
			w[i] = 1.0/Math.pow(i,a);
			sum += w[i];
		}
		cdf = new double[N+1];
		double acc = 0;
		for (int i = 1; i <= N; i++)
		{
			acc += w[i]/sum;
			cdf[i]=acc;
		}
	}

	public static int sample()
	{
		double u = Math.random();
		int lo = 1;
		int hi = N;
		int ans = hi;
		while (lo <= hi)
		{
			int mid = (lo+hi)/2;
			if (cdf[mid] >= u)
			{
				ans = mid;
				hi = mid-1;
			}
			else
			{
				lo = mid+1;
			}
		}
		return ans;
	}
}