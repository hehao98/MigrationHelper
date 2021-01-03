# MigrationHelper

Welcome to the main repository for paper "A Multi-Metric Ranking Approach for Library Migration Recommendations."

This repository contains everything for paper replication, some command-line utility tools, and a RESTful backend for our website demo.

## For SANER 2021 Paper Readers

### Web Demo

A Web Demo is available at [this link](http://migration-helper.net). 

### Migration Dataset

We list where to find the migration dataset mentioned in the paper here.

1. [evaluation/manual/ground-truth-all.xlsx](evaluation/manual/ground-truth-all.xlsx). List of all ground truth migration rules.
2. [evaluation/manual/confirmed-migrations-all.xlsx](evaluation/manual/confirmed-migrations-all.xlsx). List of all confirmed migration commits.

We also have some partial database dumps, including dependency change sequences, available [here](https://drive.google.com/drive/folders/1UBTp9betjAi6gSly4OmGvLwIl7AJvUfg). See the `doc/` folder for documentation about the MongoDB database.

### Recommendation Examples

Please download [evaluation/recommend-output.csv](evaluation/recommend-output.csv) (~50MB) for all recommendation output in the 190 source libraries in GT2014. For other recommendation example, you can either try the web demo above, or download dumped data in the Google Drive link mentioned above.

### Evaluation

The evaluation results are all put in the `evaluation/` folder. Not that the RQs are an earlier formulation different from that in paper. Therefore, we list where to find evaluation results in the paper here.

1. For labelling of ground truth used in RQ1 and RQ2, see `evaluation/rq1_*.ipynb`, `evaluation/manual/confirmed-migrations-*.xlsx`, and [evaluation/ground-truth.xlsx](evaluation/ground-truth.xlsx).
2. For RQ1, see [evaluation/rq2_metrics.ipynb](evaluation/rq2_metrics.ipynb).
3. For RQ2, see [evaluation/rq3_ranking.ipynb](evaluation/rq3_ranking.ipynb) and [evaluation/rq4_generalize.ipynb](evaluation/rq4_generalize.ipynb).

All the intermediate data during labelling of ground truth are also kept in this folder.

### Implementation Details

For those interested in implementation details of the paper, here are some starting points to look at:

1. [src/.../DepSeqAnalysisService.java](src/main/java/edu/pku/migrationhelper/service/DepSeqAnalysisService.java)  implements the core migration target recommendation algorithm.
2. [src/.../LibraryIdentityService.java](src/main/java/edu/pku/migrationhelper/service/LibraryIdentityService.java) downloads JARs from Maven, analyzes them and store the classes.
3. [scripts/depseq_build3.py](scripts/depseq_build3.py) implements dependency sequence construction on World of Code.

## Supplementary Materials

### Manual API Change Analysis of 100 Random Sampled Migration Commits

Detailed annotations can be found in [evaluation/manual/sampled-commits.csv](evaluation/manual/sampled-commits.csv).

### Complete Table of Precision, Recall, NDCG, MRR and F-Measure

#### Summary

```
----------- GT2014 -----------
RuleFreq                      : Precision@1 = 0.4421, MRR = 0.5275, Recall@5 = 0.2354, Recall@10 = 0.3066, Recall@20 = 0.4204, NDCG@10 = 0.3669
RuleFreqSameCommit            : Precision@1 = 0.6632, MRR = 0.7348, Recall@5 = 0.4127, Recall@10 = 0.5796, Recall@20 = 0.7193, NDCG@10 = 0.6073
Teyton et al. 2013            : Precision@1 = 0.6632, MRR = 0.7311, Recall@5 = 0.4101, Recall@10 = 0.5821, Recall@20 = 0.7193, NDCG@10 = 0.6056
Alrubaye et al. 2019          : Precision@1 = 0.9412, MRR = 0.9412, Recall@5 = 0.0492, Recall@10 = 0.0492, Recall@20 = 0.0492, NDCG@10 = 0.9412
Message                       : Precision@1 = 0.6737, MRR = 0.7587, Recall@5 = 0.4476, Recall@10 = 0.6248, Recall@20 = 0.7620, NDCG@10 = 0.6619
RFSC * Message                : Precision@1 = 0.7579, MRR = 0.8277, Recall@5 = 0.5084, Recall@10 = 0.6960, Recall@20 = 0.8564, NDCG@10 = 0.7436
RFSC * Message * Distance     : Precision@1 = 0.7737, MRR = 0.8409, Recall@5 = 0.5265, Recall@10 = 0.7089, Recall@20 = 0.8629, NDCG@10 = 0.7589
RFSC * Message * APISupport   : Precision@1 = 0.7737, MRR = 0.8361, Recall@5 = 0.5097, Recall@10 = 0.6986, Recall@20 = 0.8706, NDCG@10 = 0.7479
Our Method                    : Precision@1 = 0.7947, MRR = 0.8566, Recall@5 = 0.5330, Recall@10 = 0.7089, Recall@20 = 0.8939, NDCG@10 = 0.7702
----------- GT2020 -----------
Teyton et al. 2013            : Precision@1 = 0.6174, MRR = 0.7066, Recall@5 = 0.5270, Recall@10 = 0.6710, Recall@20 = 0.8380, NDCG@10 = 0.6468
Teyton et al. 2013'           : Precision@1 = 0.6035, MRR = 0.6985, Recall@5 = 0.5172, Recall@10 = 0.6628, Recall@20 = 0.8020, NDCG@10 = 0.6653
Teyton et al. 2013''          : Precision@1 = 0.8148, MRR = 0.8410, Recall@5 = 0.2209, Recall@10 = 0.2226, Recall@20 = 0.2226, NDCG@10 = 0.8475
Alrubaye et al. 2019          : Precision@1 = 0.9143, MRR = 0.9143, Recall@5 = 0.0540, Recall@10 = 0.0540, Recall@20 = 0.0540, NDCG@10 = 0.9143
Our Approach                  : Precision@1 = 0.6870, MRR = 0.7918, Recall@5 = 0.6514, Recall@10 = 0.8314, Recall@20 = 0.9918, NDCG@10 = 0.7770
```

#### Result of Our Approach on GT2014

```
MRR-C/P = 0.8565813287461487/0.8565813287461487
Top   1: Precision = 0.7947, Recall = 0.1953, NDCG = 0.7947, F-Measure = 0.3136
Top   2: Precision = 0.6614, Recall = 0.3234, NDCG = 0.7836, F-Measure = 0.4344
Top   3: Precision = 0.5583, Recall = 0.4088, NDCG = 0.7669, F-Measure = 0.4720
Top   4: Precision = 0.4907, Recall = 0.4787, NDCG = 0.7640, F-Measure = 0.4846
Top   5: Precision = 0.4374, Recall = 0.5330, NDCG = 0.7611, F-Measure = 0.4805
Top   6: Precision = 0.3912, Recall = 0.5718, NDCG = 0.7580, F-Measure = 0.4645
Top   7: Precision = 0.3584, Recall = 0.6106, NDCG = 0.7594, F-Measure = 0.4517
Top   8: Precision = 0.3351, Recall = 0.6520, NDCG = 0.7639, F-Measure = 0.4427
Top   9: Precision = 0.3146, Recall = 0.6882, NDCG = 0.7690, F-Measure = 0.4318
Top  10: Precision = 0.2918, Recall = 0.7089, NDCG = 0.7702, F-Measure = 0.4134
Top  20: Precision = 0.1849, Recall = 0.8939, NDCG = 0.8123, F-Measure = 0.3064
```

#### Result of Teyton et al. 2013 (t = 0) on GT2014

```
MRR-C/P = 0.7172956537340605/0.7227737800081314
Top   1: Precision = 0.6526, Recall = 0.1604, NDCG = 0.6526, F-Measure = 0.2575
Top   2: Precision = 0.4868, Recall = 0.2380, NDCG = 0.5893, F-Measure = 0.3197
Top   3: Precision = 0.4134, Recall = 0.3027, NDCG = 0.5761, F-Measure = 0.3495
Top   4: Precision = 0.3714, Recall = 0.3622, NDCG = 0.5764, F-Measure = 0.3667
Top   5: Precision = 0.3344, Recall = 0.4075, NDCG = 0.5713, F-Measure = 0.3673
Top   6: Precision = 0.3142, Recall = 0.4592, NDCG = 0.5793, F-Measure = 0.3731
Top   7: Precision = 0.2916, Recall = 0.4968, NDCG = 0.5829, F-Measure = 0.3675
Top   8: Precision = 0.2753, Recall = 0.5356, NDCG = 0.5894, F-Measure = 0.3636
Top   9: Precision = 0.2584, Recall = 0.5653, NDCG = 0.5957, F-Measure = 0.3547
Top  10: Precision = 0.2417, Recall = 0.5873, NDCG = 0.5987, F-Measure = 0.3425
Top  20: Precision = 0.1506, Recall = 0.7283, NDCG = 0.6344, F-Measure = 0.2496
```

#### Result of Teyton et al. 2013 (t = 0.002) on GT2014

```
MRR-C/P = 0.7221139810907252/0.7301255641023083
Top   1: Precision = 0.6595, Recall = 0.1578, NDCG = 0.6595, F-Measure = 0.2547
Top   2: Precision = 0.5125, Recall = 0.2393, NDCG = 0.6297, F-Measure = 0.3263
Top   3: Precision = 0.4459, Recall = 0.3040, NDCG = 0.6309, F-Measure = 0.3615
Top   4: Precision = 0.4044, Recall = 0.3583, NDCG = 0.6387, F-Measure = 0.3800
Top   5: Precision = 0.3655, Recall = 0.3972, NDCG = 0.6412, F-Measure = 0.3807
Top   6: Precision = 0.3489, Recall = 0.4450, NDCG = 0.6551, F-Measure = 0.3911
Top   7: Precision = 0.3310, Recall = 0.4812, NDCG = 0.6661, F-Measure = 0.3922
Top   8: Precision = 0.3108, Recall = 0.5045, NDCG = 0.6688, F-Measure = 0.3846
Top   9: Precision = 0.2960, Recall = 0.5265, NDCG = 0.6745, F-Measure = 0.3790
Top  10: Precision = 0.2816, Recall = 0.5420, NDCG = 0.6777, F-Measure = 0.3706
Top  20: Precision = 0.2110, Recall = 0.6365, NDCG = 0.7063, F-Measure = 0.3169
```

#### Result of Teyton et al. 2013 (t = 0.015) on GT2014

```
MRR-C/P = 0.8862573099415206/0.8862573099415206
Top   1: Precision = 0.8737, Recall = 0.1074, NDCG = 0.8737, F-Measure = 0.1912
Top   2: Precision = 0.7820, Recall = 0.1345, NDCG = 0.8696, F-Measure = 0.2296
Top   3: Precision = 0.7468, Recall = 0.1527, NDCG = 0.8796, F-Measure = 0.2535
Top   4: Precision = 0.7209, Recall = 0.1604, NDCG = 0.8832, F-Measure = 0.2624
Top   5: Precision = 0.7111, Recall = 0.1656, NDCG = 0.8840, F-Measure = 0.2686
Top   6: Precision = 0.7043, Recall = 0.1695, NDCG = 0.8853, F-Measure = 0.2732
Top   7: Precision = 0.7000, Recall = 0.1721, NDCG = 0.8864, F-Measure = 0.2762
Top   8: Precision = 0.6943, Recall = 0.1734, NDCG = 0.8864, F-Measure = 0.2774
Top   9: Precision = 0.6974, Recall = 0.1759, NDCG = 0.8916, F-Measure = 0.2810
Top  10: Precision = 0.6904, Recall = 0.1759, NDCG = 0.8916, F-Measure = 0.2804
Top  20: Precision = 0.6766, Recall = 0.1759, NDCG = 0.8916, F-Measure = 0.2793
```

#### Result of Alrubaye et al. 2019 (RS >= 0.6 and AS > 0) on GT2014

```
MRR-C/P = 0.9411764705882353/0.9411764705882353
Top   1: Precision = 0.9412, Recall = 0.0414, NDCG = 0.9412, F-Measure = 0.0793
Top   2: Precision = 0.9250, Recall = 0.0479, NDCG = 0.9412, F-Measure = 0.0910
Top   3: Precision = 0.9268, Recall = 0.0492, NDCG = 0.9412, F-Measure = 0.0934
Top   4: Precision = 0.9268, Recall = 0.0492, NDCG = 0.9412, F-Measure = 0.0934
Top   5: Precision = 0.9268, Recall = 0.0492, NDCG = 0.9412, F-Measure = 0.0934
Top   6: Precision = 0.9268, Recall = 0.0492, NDCG = 0.9412, F-Measure = 0.0934
Top   7: Precision = 0.9268, Recall = 0.0492, NDCG = 0.9412, F-Measure = 0.0934
Top   8: Precision = 0.9268, Recall = 0.0492, NDCG = 0.9412, F-Measure = 0.0934
Top   9: Precision = 0.9268, Recall = 0.0492, NDCG = 0.9412, F-Measure = 0.0934
Top  10: Precision = 0.9268, Recall = 0.0492, NDCG = 0.9412, F-Measure = 0.0934
Top  20: Precision = 0.9268, Recall = 0.0492, NDCG = 0.9412, F-Measure = 0.0934
```

#### Result of Our Approach on GT2020

```
Result of Our Method on 230 Library Queries:
MRR-C/P = 0.7880833013249893/0.7880833013249893
Top   1: Precision = 0.6783, Recall = 0.2553, NDCG = 0.6783
Top   2: Precision = 0.5413, Recall = 0.4075, NDCG = 0.6856
Top   3: Precision = 0.4507, Recall = 0.5090, NDCG = 0.6962
Top   4: Precision = 0.3891, Recall = 0.5859, NDCG = 0.7133
Top   5: Precision = 0.3391, Recall = 0.6383, NDCG = 0.7235
Top   6: Precision = 0.3080, Recall = 0.6956, NDCG = 0.7372
Top   7: Precision = 0.2789, Recall = 0.7349, NDCG = 0.7467
Top   8: Precision = 0.2565, Recall = 0.7725, NDCG = 0.7583
Top   9: Precision = 0.2382, Recall = 0.8069, NDCG = 0.7675
Top  10: Precision = 0.2191, Recall = 0.8249, NDCG = 0.7702
Top  20: Precision = 0.1326, Recall = 0.9984, NDCG = 0.8048
```

#### Result of Teyton et al. 2013 (t = 0.0) on GT2020

```
MRR-C/P = 0.6936973683500808/0.6936973683500808
Top   1: Precision = 0.5913, Recall = 0.2226, NDCG = 0.5913
Top   2: Precision = 0.4565, Recall = 0.3437, NDCG = 0.5750
Top   3: Precision = 0.3681, Recall = 0.4157, NDCG = 0.5722
Top   4: Precision = 0.3239, Recall = 0.4877, NDCG = 0.5927
Top   5: Precision = 0.2809, Recall = 0.5286, NDCG = 0.6023
Top   6: Precision = 0.2493, Recall = 0.5630, NDCG = 0.6116
Top   7: Precision = 0.2242, Recall = 0.5908, NDCG = 0.6179
Top   8: Precision = 0.2065, Recall = 0.6219, NDCG = 0.6263
Top   9: Precision = 0.1903, Recall = 0.6448, NDCG = 0.6326
Top  10: Precision = 0.1787, Recall = 0.6727, NDCG = 0.6391
Top  20: Precision = 0.1133, Recall = 0.8527, NDCG = 0.6845
```

#### Result of Teyton et al. 2013 (t = 0.002) on GT2020

```
MRR-C/P = 0.698466763865831/0.698466763865831
Top   1: Precision = 0.6035, Recall = 0.2242, NDCG = 0.6035
Top   2: Precision = 0.4578, Recall = 0.3372, NDCG = 0.5819
Top   3: Precision = 0.3868, Recall = 0.4223, NDCG = 0.5966
Top   4: Precision = 0.3349, Recall = 0.4812, NDCG = 0.6119
Top   5: Precision = 0.2921, Recall = 0.5172, NDCG = 0.6197
Top   6: Precision = 0.2670, Recall = 0.5581, NDCG = 0.6348
Top   7: Precision = 0.2444, Recall = 0.5859, NDCG = 0.6442
Top   8: Precision = 0.2281, Recall = 0.6137, NDCG = 0.6518
Top   9: Precision = 0.2162, Recall = 0.6416, NDCG = 0.6597
Top  10: Precision = 0.2052, Recall = 0.6628, NDCG = 0.6653
Top  20: Precision = 0.1487, Recall = 0.8020, NDCG = 0.7003
```

#### Result of Teyton et al. 2013 (t = 0.015) on GT2020

```
MRR-C/P = 0.8425925925925926/0.8425925925925926
Top   1: Precision = 0.8148, Recall = 0.1440, NDCG = 0.8148
Top   2: Precision = 0.6894, Recall = 0.1817, NDCG = 0.8323
Top   3: Precision = 0.6392, Recall = 0.2029, NDCG = 0.8409
Top   4: Precision = 0.6209, Recall = 0.2144, NDCG = 0.8463
Top   5: Precision = 0.6027, Recall = 0.2209, NDCG = 0.8479
Top   6: Precision = 0.5939, Recall = 0.2226, NDCG = 0.8490
Top   7: Precision = 0.5862, Recall = 0.2226, NDCG = 0.8490
Top   8: Precision = 0.5837, Recall = 0.2226, NDCG = 0.8490
Top   9: Precision = 0.5837, Recall = 0.2226, NDCG = 0.8490
Top  10: Precision = 0.5837, Recall = 0.2226, NDCG = 0.8490
Top  20: Precision = 0.5837, Recall = 0.2226, NDCG = 0.8490
```

#### Result of Alrubaye et al. 2019 (RS >= 0.6 and AS > 0) on GT2020

```
Result of Alrubaye et al. 2019 on 35 Library Queries:
MRR-C/P = 0.9142857142857143/0.9142857142857143
Top   1: Precision = 0.9143, Recall = 0.0524, NDCG = 0.9143
Top   2: Precision = 0.8919, Recall = 0.0540, NDCG = 0.9143
Top   3: Precision = 0.8919, Recall = 0.0540, NDCG = 0.9143
Top   4: Precision = 0.8919, Recall = 0.0540, NDCG = 0.9143
Top   5: Precision = 0.8919, Recall = 0.0540, NDCG = 0.9143
Top   6: Precision = 0.8919, Recall = 0.0540, NDCG = 0.9143
Top   7: Precision = 0.8919, Recall = 0.0540, NDCG = 0.9143
Top   8: Precision = 0.8919, Recall = 0.0540, NDCG = 0.9143
Top   9: Precision = 0.8919, Recall = 0.0540, NDCG = 0.9143
Top  10: Precision = 0.8919, Recall = 0.0540, NDCG = 0.9143
Top  20: Precision = 0.8919, Recall = 0.0540, NDCG = 0.9143
```

## Development

Details about the development of this project is in [DEVELOPMENT.md](DEVELOPMENT.md). 
