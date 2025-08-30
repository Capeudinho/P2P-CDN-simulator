package cdn;

import peersim.core.Control;
import peersim.core.CommonState;
import java.io.FileWriter;
import java.io.IOException;

public class Report implements Control
{
	private static final String FILE_NAME = "metrics.txt";
	private boolean headerWritten = false;

	public Report(String prefix)
	{
	}

	private double meanChunkRedundancy()
	{
		var map = Metrics.getChunkRedundancy();
		if (map.isEmpty())
		{
			return 0.0;
		}
		int total = 0;
		for (int v : map.values())
		{
			total += v;
		}
		return total/(double)map.size();
	}

	@Override
	public boolean execute()
	{
		try (FileWriter fw = new FileWriter(FILE_NAME, true))
		{
			if (!headerWritten)
			{
				fw.write("time   requests   hitrate   messages   lat mean   lat max   peers traffic   chunk_redundancy\n");
				headerWritten = true;
			}
			fw.write(String.format("%d   %d   %.4f   %d   %.4f   %d   %d   %.4f\n", CommonState.getTime(), Metrics.requests(), Metrics.hitRate(), Metrics.messages(), Metrics.latMean(), Metrics.latMax(), Metrics.getTotalBytesTransferred(), meanChunkRedundancy()));
		}
		catch (IOException e)
		{
			System.err.println("Erro ao salvar métricas: "+e.getMessage());
		}
		return false;
	}
}