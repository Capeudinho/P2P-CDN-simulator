package cdn;

public final class Messages
{
	public static final class ChunkRequest
	{
		public final long videoId;
		public final int chunkIndex;
		public final int requesterId;
		public final int prevHopId;

		public ChunkRequest(long v, int idx, int req, int prev)
		{
			this.videoId = v;
			this.chunkIndex = idx;
			this.requesterId = req;
			this.prevHopId = prev;
		}

		@Override public String toString()
		{
			return "REQ(v="+videoId+",c="+chunkIndex+",from="+requesterId+")";
		}
	}

	public static final class ChunkReply
	{
		public final long videoId;
		public final int chunkIndex;
		public final int srcNodeId;

		public ChunkReply(long v, int idx, int src)
		{
			this.videoId = v;
			this.chunkIndex = idx;
			this.srcNodeId = src;
		}

		@Override public String toString()
		{
			return "REP(v="+videoId+",c="+chunkIndex+",src="+srcNodeId+")";
		}
	}

	public static final class NextChunk
	{
		public final long videoId;
		public final int nextIndex;

		public NextChunk(long v, int i)
		{
			this.videoId = v;
			this.nextIndex = i;
		}

		@Override public String toString()
		{
			return "NEXT(v="+videoId+",i="+nextIndex+")";
		}
	}
}