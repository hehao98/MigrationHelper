package edu.pku.migrationhelper.service;

import ca.pfv.spmf.algorithms.sequentialpatterns.spam.AlgoTKS;
import ca.pfv.spmf.algorithms.sequentialpatterns.spam.PatternTKS;
import ca.pfv.spmf.patterns.itemset_list_integers_without_support.Itemset;
import edu.pku.migrationhelper.data.RepositoryDepSeq;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

@Service
public class DependencyChangePatternAnalysisService {

    public static class DependencySequenceResult {
        public List<List<Long>> pattern;
        public int support;
        public double globalSupport;
        public double patternSupport;
        public double multipleSupport;
    }

    public List<DependencySequenceResult> analyzeDependencySequence(List<RepositoryDepSeq> depSeqList, int topK) throws Exception {
        File inputFile = File.createTempFile("dependency_change_pattern_input", ".txt");
        File outputFile = File.createTempFile("dependency_change_pattern_output", ".txt");
        inputFile.deleteOnExit();
        outputFile.deleteOnExit();
        FileWriter writer = new FileWriter(inputFile);

        Map<Long, Integer> lib2Id = new HashMap<>(100000);
        Map<Integer, Long> id2Lib = new HashMap<>(100000);
        int idGenerator = 1;

        for (RepositoryDepSeq depSeq : depSeqList) {
            if(depSeq.getDepSeqList() != null && !depSeq.getDepSeqList().isEmpty()) {
                for (Long lib : depSeq.getDepSeqList()) {
                    if(lib == 0) {
                        writer.write("-1 ");
                    } else {
                        Integer id = lib2Id.get(lib);
                        if(id == null) {
                            id = idGenerator++;
                            lib2Id.put(lib, id);
                            id2Lib.put(id, lib);
                        }
                        writer.write(id + " ");
                    }
                }
                writer.write("-2\n");
            }
        }

        writer.close();

        AlgoTKS algo = new AlgoTKS();
        algo.setMinimumPatternLength(3);
        algo.setMaximumPatternLength(3);
        PriorityQueue<PatternTKS> patterns = algo.runAlgorithm(inputFile.getPath(), outputFile.getPath(), topK);

        List<DependencySequenceResult> result = new ArrayList<>(patterns.size());
        for (PatternTKS pattern : patterns) {
            DependencySequenceResult res = new DependencySequenceResult();
            List<List<Long>> libIds = new LinkedList<>();
            for (Itemset itemset : pattern.prefix.getItemsets()) {
                List<Long> itemList = new ArrayList<>(10);
                for (Integer item : itemset.getItems()) {
                    Long lib = id2Lib.get(item);
                    if(lib == null) throw new RuntimeException("Unknown item: " + item);
                    itemList.add(lib);
                }
                libIds.add(itemList);
            }
            res.pattern = new ArrayList<>(libIds);
            res.support = pattern.support;
            result.add(res);
        }
        return result;
    }

}
