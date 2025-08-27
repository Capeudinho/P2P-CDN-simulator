package cdn;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class Metrics
{
	private static long hits = 0;
	private static long misses = 0;
	private static long deliveries = 0;
	private static long msgs = 0;
	private static long issued = 0;
	private static long totalBytesTransferred = 0;
	private static final Map<String, Long> startTimes = new ConcurrentHashMap<>();

	private static long now()
	{
		return peersim.core.CommonState.getTime();
	}

	public static synchronized void hit()
	{
		hits++;
	}

	public static synchronized void miss()
	{
		misses++;
	}

	public static synchronized void delivery()
	{
		deliveries++;
	}

	public static synchronized void msg()
	{
		msgs++;
	}

	public static synchronized void requestIssued(int requester, long vid, int idx)
	{
		issued++;
		String k = vid+":"+idx;
		startTimes.put(k, now());
	}

	public static synchronized void requestCompleted(int requester, long vid, int idx)
	{
		String k = vid+":"+idx;
		Long t0 = startTimes.remove(k);
		if (t0 != null)
		{
			long lat = now() - t0;
			LatStats.add(lat);
		}
	}

	public static double hitRate()
	{
		long denom = hits+misses;
		if (denom == 0)
		{
			return 0.0;
		}
		else
		{
			return (hits*1.0)/denom;
		}
	}

	public static long requests()
	{
		return issued;
	}

	public static long deliveries()
	{
		return deliveries;
	}

	public static long messages()
	{
		return msgs;
	}

	private static final class LatStats
	{
		private static long count = 0;
		private static long sum = 0;
		private static long max = 0;

		static synchronized void add(long x)
		{
			count++;
			sum += x;
			if (x > max)
			{
				max = x;
			}
		}

		static synchronized long count()
		{
			return count;
		}

		static synchronized double mean()
		{
			if (count == 0)
			{
				return 0.0;
			} else
			{
				return ((double)sum)/count;
			}
		}

		static synchronized long max()
		{
			return max;
		}
	}

	public static long latCount()
	{
		return LatStats.count();
	}

	public static double latMean()
	{
		return LatStats.mean();
	}

	public static long latMax()
	{
		return LatStats.max();
	}

	public static void addBytesTransferred(long bytes)
	{
		totalBytesTransferred += bytes;
	}

	public static long getTotalBytesTransferred()
	{
		return totalBytesTransferred;
	}
}