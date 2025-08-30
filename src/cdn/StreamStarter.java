package cdn;

import peersim.core.Control;
import peersim.core.Network;
import peersim.edsim.EDSimulator;
import peersim.core.CommonState;
import peersim.config.Configuration;
import peersim.core.Node;

public class StreamStarter implements Control
{
    private static final String PAR_PROTOCOL = "protocol";
    private static final String PAR_SESSIONS = "sessions";
    private static final String PAR_CATALOG = "catalog";
    private static final String PAR_ALPHA = "alpha";
    private final int pid;
    private final int sessions;
    private final int catalog;
    private final double alpha;

    public StreamStarter(String prefix)
	{
        this.pid = Configuration.getPid(prefix+"."+PAR_PROTOCOL);
        this.sessions = Configuration.getInt(prefix+"."+PAR_SESSIONS, 100);
        this.catalog = Configuration.getInt(prefix+"."+PAR_CATALOG, 2000);
        this.alpha = Configuration.getDouble(prefix+"."+PAR_ALPHA, 0.9);
        Zipf.init(catalog, alpha);
    }

    @Override
    public boolean execute()
	{
        int started = 0;
        while (started < sessions)
		{
            int nodeId = 1+CommonState.r.nextInt(Math.max(1, Network.size()-1));
            Node node = Network.get(nodeId);
            long videoId = Zipf.sample();
            EDSimulator.add(0, new Messages.NextChunk(videoId, 0), node, pid);
            started++;
        }
        System.out.println("Started "+started+" streaming sessions.");
        return false;
    }
}