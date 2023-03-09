import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OnlineCoursesAnalyzer {
    private final Stream<String[]> dataset;

    public OnlineCoursesAnalyzer(String datasetPath) throws IOException {
        datasetPath = "D:\\IDEAproject\\OnlineCoursesAnalyzer\\local.csv";
        this.dataset = Files.lines(Paths.get(datasetPath)).skip(1)
                .map(l -> l.split(","));
    }
    public Map<String, Integer> getPtcpCountByInst() {
        Stream<String[]> dataset = this.dataset;
        Map<String, Integer> PtcpCountByInst;
        return  PtcpCountByInst = dataset.collect(Collectors.groupingBy(d -> d[0], Collectors.summingInt(p -> Integer.parseInt(p[8]))));

    }
//    public Map<String, Integer> getPtcpCountByInstAndSubject() {
//
//    }

}
