package cdn;

import peersim.core.Control;
import peersim.core.CommonState;

public class Report implements Control
{
	public Report(String prefix)
	{

	}

	@Override
	public boolean execute()
	{
		System.out.printf("t=%d req=%d hitrate=%.4f msgs=%d lat.mean=%.2f lat.max=%d%n", CommonState.getTime(), Metrics.requests(), Metrics.hitRate(), Metrics.messages(), Metrics.latMean(), Metrics.latMax());
		return false;
	}
}