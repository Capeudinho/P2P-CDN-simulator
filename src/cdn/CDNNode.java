package cdn;

import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.core.Node;
import peersim.core.CommonState;
import peersim.transport.Transport;
import peersim.config.Configuration;
import java.util.Arrays;
import java.util.Random;

public class CDNNode implements EDProtocol
{
	private static final String PAR_TRANSPORT = "transport";
	private static final String PAR_CAPACITY = "capacity";
	private static final String PAR_CHUNKSIZE = "chunksize";
	private static final String PAR_ORIGIN_LAT = "originlatency";
	private final int tid;
	private final int capacity;
	private final int chunkBytes;
	private final int originLatency;
	private CachePolicy<String, byte[]> cache;
	private int[] neighbors;

	public CDNNode(String prefix)
	{
		this.tid = Configuration.getPid(prefix+"."+PAR_TRANSPORT);
		this.capacity = Configuration.getInt(prefix+"."+PAR_CAPACITY, 200);
		this.chunkBytes = Configuration.getInt(prefix+"."+PAR_CHUNKSIZE, 128*1024);
		this.originLatency = Configuration.getInt(prefix+"."+PAR_ORIGIN_LAT, 50);
		this.cache = new LFUCache<>(capacity);
		this.neighbors = new int[0];
	}

	@Override public Object clone()
	{
		try
		{
			CDNNode copy = (CDNNode)super.clone();
			copy.cache = new LFUCache<>(capacity);
			copy.neighbors = new int[0];
			return copy;
		}
		catch (CloneNotSupportedException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void setNeighbors(int[] nb)
	{
		this.neighbors = nb;
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
		boolean hit = cache.contains(k);
		if (hit)
		{
			cache.get(k);
			Metrics.hit();
			int service = 2+CommonState.r.nextInt(3);
			sendLater(me, req.requesterId, new Messages.ChunkReply(req.videoId, req.chunkIndex, (int)me.getID()), service);
			return;
		}
		Metrics.miss();
		int myId = (int)me.getID();
		if (myId == 0)
		{
			int base = originLatency+CommonState.r.nextInt((originLatency/4)+1);
			byte[] data = new byte[chunkBytes];
			Arrays.fill(data, (byte)1);
			cache.put(k, data);
			sendLater(me, req.requesterId, new Messages.ChunkReply(req.videoId, req.chunkIndex, myId), base);
		}
		else
		{
			int toOrigin = 0;
			sendLater(me, toOrigin, req, 1);
		}
	}

	private void onChunkReply(Node me, int pid, Messages.ChunkReply rep)
	{
		String k = key(rep.videoId, rep.chunkIndex);
		if (!cache.contains(k))
		{
			cache.put(k, new byte[chunkBytes]);
		}
		Metrics.delivery();
		Metrics.requestCompleted((int)me.getID(), rep.videoId, rep.chunkIndex);
		Integer maybeRequester = Metrics.getWaitingRequester(rep.videoId, rep.chunkIndex);
		if (maybeRequester != null && maybeRequester != (int)me.getID())
		{
			sendLater(me, maybeRequester, rep, 1);
		}
	}

	private void onNextChunk(Node me, int pid, Messages.NextChunk ev)
	{
		int meId = (int)me.getID();
		long vid = ev.videoId;
		int idx = ev.nextIndex;
		int target = chooseNeighbor(meId);
		if (target < 0)
		{
			target = 0;
		}
		Metrics.requestIssued(meId, vid, idx);
		sendLater(me, target, new Messages.ChunkRequest(vid, idx, meId, meId), 1);
		int playbackGap = 5;
		EDSimulator.add(playbackGap, new Messages.NextChunk(vid, idx+1), me, CommonState.getPid());
	}

	private int chooseNeighbor(int meId)
	{
		if (neighbors == null || neighbors.length == 0)
		{
			return -1;
		}
		Random r = CommonState.r;
		return neighbors[r.nextInt(neighbors.length)];
	}

	private void sendLater(Node from, int toNodeId, Object ev, int delay)
	{
		Node to = peersim.core.Network.get(toNodeId);
		((Transport)from.getProtocol(tid)).send(from, to, ev, CommonState.getPid());
		Metrics.msg();
	}
}