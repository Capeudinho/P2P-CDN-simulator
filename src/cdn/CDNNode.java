package cdn;

import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.core.Node;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.transport.Transport;
import peersim.config.Configuration;

import java.util.Random;

public class CDNNode implements EDProtocol
{
	private static final String PAR_LINKABLE = "linkable";
	private static final String PAR_TRANSPORT = "transport";
	private static final String PAR_CAPACITY = "capacity";
	private static final String PAR_CHUNKSIZE = "chunksize";
	private static final String PAR_ORIGIN_LAT = "originlatency";
	private final int lid;
	private final int tid;
	private final int capacity;
	private final int chunkBytes;
	private final int originLatency;
	private CachePolicy<String, byte[]> cache;

	public CDNNode(String prefix)
	{
		this.tid = Configuration.getPid(prefix+"."+PAR_TRANSPORT);
		this.capacity = Configuration.getInt(prefix+"."+PAR_CAPACITY, 500);
		this.chunkBytes = Configuration.getInt(prefix+"."+PAR_CHUNKSIZE, 65536);
		this.originLatency = Configuration.getInt(prefix+"."+PAR_ORIGIN_LAT, 50);
		this.lid = Configuration.getPid(prefix+"."+PAR_LINKABLE);
		this.cache = new LRUCache<>(capacity);
	}

	@Override
	public Object clone()
	{
		try
		{
			return (CDNNode)super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public void processEvent(Node node, int pid, Object event)
	{
		if (event instanceof Messages.ChunkRequest m)
		{
			onChunkRequest(node, pid, m);
		}
		else if (event instanceof Messages.ChunkReply m)
		{
			onChunkReply(node, pid, m);
		}
		else if (event instanceof Messages.NextChunk m)
		{
			onNextChunk(node, pid, m);
		}
		else
		{
			System.err.println("Unknown event "+event);
		}
	}

	private String key(long vid, int idx)
	{
		return vid+":"+idx;
	}

	private void onChunkRequest(Node me, int pid, Messages.ChunkRequest req)
	{
		String k = key(req.videoId, req.chunkIndex);
		int myId = (int)me.getID();
		if (myId == 0)
		{
			int service = originLatency+CommonState.r.nextInt((originLatency/4)+1);
			Node target = peersim.core.Network.get(req.requesterId);
			sendLater(me, target, new Messages.ChunkReply(req.videoId, req.chunkIndex, myId), service);
			return;
		}
		boolean hit = cache.contains(k);
		if (hit)
		{
			cache.get(k);
			Metrics.hit();
			int service = 2+CommonState.r.nextInt(3);
			Node target = peersim.core.Network.get(req.requesterId);
			sendLater(me, target, new Messages.ChunkReply(req.videoId, req.chunkIndex, myId), service);
			return;
		}
		else
		{
			Metrics.miss();
		}
		if (req.ttl > 0)
		{
			Node target = chooseNeighbor(me, req.prevHopId);
			sendLater(me, target, new Messages.ChunkRequest(req.videoId, req.chunkIndex, req.requesterId, myId, req.ttl-1), 1);
		}
		else
		{
			Node target = peersim.core.Network.get(0);
			sendLater(me, target, req, 1);
		}
	}

	private void onChunkReply(Node me, int pid, Messages.ChunkReply rep)
	{
		String k = key(rep.videoId, rep.chunkIndex);
		if (!cache.contains(k))
		{
			cache.put(k, new byte[chunkBytes]);
			Metrics.chunkStored(k);
		}
		Metrics.delivery();
		Metrics.requestCompleted((int)me.getID(), rep.videoId, rep.chunkIndex);
	}

	private void onNextChunk(Node me, int pid, Messages.NextChunk ev)
	{
		int meId = (int)me.getID();
		long vid = ev.videoId;
		int idx = ev.nextIndex;
		Node target = chooseNeighbor(me, 0);
		Metrics.requestIssued(meId, vid, idx);
		sendLater(me, target, new Messages.ChunkRequest(vid, idx, meId, meId, 5), 1);
		EDSimulator.add(5, new Messages.NextChunk(vid, idx+1), me, CommonState.getPid());
	}

	private Node chooseNeighbor(Node me, int prev)
	{
		Linkable linkable = (Linkable)me.getProtocol(lid);
		if (linkable.degree() == 0)
		{
			return linkable.getNeighbor(0);
		}
		Random r = CommonState.r;
		int i = 0;
		int t = 0;
		while (t < linkable.degree()*linkable.degree())
		{
			i = r.nextInt(linkable.degree());
			Node neighbor = linkable.getNeighbor(i);
			if (neighbor.isUp() && (int)neighbor.getID() != prev && (int)neighbor.getID() != 0)
			{
				return neighbor;
			}
			t++;
		}
		return linkable.getNeighbor(0);
	}

	private void sendLater(Node from, Node to, Object ev, int delay)
	{
		Transport transport = (Transport)from.getProtocol(tid);
		transport.send(from, to, ev, CommonState.getPid());
		Metrics.msg();
		if (ev instanceof Messages.ChunkReply && (int)from.getID() != 0)
		{
			Metrics.addBytesTransferred(chunkBytes);
		}
	}
}