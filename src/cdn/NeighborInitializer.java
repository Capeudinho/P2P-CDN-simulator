package cdn;

import peersim.core.Control;
import peersim.core.Network;
import peersim.core.CommonState;
import peersim.config.Configuration;
import peersim.core.Node;
import java.util.HashSet;

public class NeighborInitializer implements Control
{
	private static final String PAR_PROTOCOL = "protocol";
	private static final String PAR_DEGREE = "degree";
	private final int pid;
	private final int degree;

	public NeighborInitializer(String prefix)
	{
		this.pid = Configuration.getPid(prefix+"."+PAR_PROTOCOL);
		this.degree = Configuration.getInt(prefix+"."+PAR_DEGREE, 6);
	}

	@Override
	public boolean execute()
	{
		int n = Network.size();
		for(int i = 0; i < n; i++)
		{
			Node node = Network.get(i);
			CDNNode proto = (CDNNode)node.getProtocol(pid);
			if (i == 0)
			{
				proto.setNeighbors(new int[0]);
				continue;
			}
			HashSet<Integer> set = new HashSet<>();
			while (set.size() < Math.min(degree, n-2))
			{
				int cand = CommonState.r.nextInt(n);
				if (cand != i && cand != 0)
				{
					set.add(cand);
				}
			}
			int[] arr = set.stream().mapToInt(x -> x).toArray();
			proto.setNeighbors(arr);
		}
		System.out.println("Neighbors initialized.");
		return false;
	}
}