# MigrationHelper

Welcome to the main repository for paper "A Multi-Metric Ranking Approach for Library Migration Recommendations."

This repository contains everything for paper replication, some command-line utility tools, and a RESTful backend for our website demo.

## For SANER 2021 Paper Readers

### Web Demo

A Web Demo is available at [this link](http://migration-helper.net). 

### Migration Dataset

We list where to find the migration dataset mentioned in the paper here.

1. [evaluation/manual/ground-truth-all.xlsx](evaluation/manual/ground-truth-all.xlsx). List of all ground truth migration rules.
2. [evaluation/manual/confirmed-migrations-all.xlsx](evaluation/manual/confirmed-migration-all.xlsx). List of all confirmed migration commits.

We also have some partial database dumps, including dependency change sequences, available [here](https://drive.google.com/drive/folders/1UBTp9betjAi6gSly4OmGvLwIl7AJvUfg). See the `doc/` folder for documentation about the MongoDB database.

### Recommendation Examples

Please download [evaluation/recommend-output.csv](evaluation/recommend-output.csv) (~50MB) for all recommendation output in the 190 source libraries in GT2014. For other recommendation example, you can either try the web demo above, or download dumped data in the Google Drive link mentioned above.

### Evaluation

The evaluation results are all put in the `evaluation/` folder. Not that the RQs are an earlier formulation different from that in paper. Therefore, we list where to find evaluation results in the paper here.

1. For labelling of ground truth used in RQ1 and RQ2, see `evaluation/rq1_*.ipynb`, `evaluation/manual/confirmed-migrations-*.xlsx`, and [evaluation/ground-truth.xlsx](evaluation/ground-truth.xlsx).
2. For RQ1, see [evaluation/rq2_metrics.ipynb](evaluation/rq2_metrics.ipynb).
3. For RQ2, see [evaluation/rq3_ranking.ipynb](evaluation/rq3_ranking.ipynb) and [evaluation/rq4_generalize.ipynb](evaluation/rq4_generalize.ipynb).

All the intermediate data during labelling of ground truth are also kept in this folder.

### Implementation Details for the Paper

For those interested in implementation details of the paper, here are some starting points to look at:

1. [src/.../DepSeqAnalysisService.java](src/main/java/edu/pku/migrationhelper/service/DepSeqAnalysisService.java)  implements the core migration target recommendation algorithm.
2. [src/.../LibraryIdentityService.java](src/main/java/edu/pku/migrationhelper/service/LibraryIdentityService.java) downloads JARs from Maven, analyzes them and store the classes.
3. [scripts/depseq_build3.py](scripts/depseq_build3.py) implements dependency sequence construction on World of Code.

## Development

Details about the development of this project is in [DEVELOPMENT.md](DEVELOPMENT.md). 