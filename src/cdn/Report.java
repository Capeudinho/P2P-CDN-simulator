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

    @Override
    public boolean execute()
    {
        try (FileWriter fw = new FileWriter(FILE_NAME, true)) {
            if (!headerWritten) {
                fw.write("time,requests,hitrate,messages,lat_mean,lat_max\n");
                headerWritten = true;
            }
            fw.write(String.format("%d,%d,%.4f,%d,%.2f,%d\n",
                CommonState.getTime(),
                Metrics.requests(),
                Metrics.hitRate(),
                Metrics.messages(),
                Metrics.latMean(),
                Metrics.latMax()));
        } catch (IOException e) {
            System.err.println("Erro ao salvar métricas: " + e.getMessage());
        }
        return false;
    }
}