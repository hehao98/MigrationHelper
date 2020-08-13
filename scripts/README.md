# Scripts

This folder contains Python scripts to achieve some tasks that are difficult to do in Java / more convinent to do in Python. 
Some of the scripts require a properly configured [oscar.py](https://github.com/ssc-oscar/oscar.py) module to work.

## Dependency Change Sequence Construction

`depseq_*.py` are a set of scripts to extract relevant data from World of Code and construct dependency change sequences.
Most of the code will use `oscar.py`, so it must be run on the raw Python 2 installation on World of Code. 

I use Python to rewrite this module for two reasons:

1. Re-use code from my Bachelor's Thesis.
2. It's difficult to maintain a World of Code API for Java.

For performance reasons, we avoid diff computation because it is extremely costly.
Instead, we use the precomputed and compressed mappings like `cf2bb` and `bbcf`.
This module first select repositories, extract related commits and diffs, then extract `pom.xml` blobs.
Finally, it build dependency change sequences from the extracted data.
All the data are also mapped to Java classes in the `data.woc` package.

```shell script
python depseq_get_repo_commits.py
python depseq_get_commit_diff.py
python depseq_get_pom_blobs.py
python depseq_build.py
```

`depseq_stats.py` is for printing some interesting statistics about the collected data.

We do not collect Java blobs because there are too many (probabaly around 1 billion), which is far beyond the capacity of 
a single machine 100GB RAM MongoDB server. 
To compute the API support, we plan to utilize the Blob random access service provided by World of Code.
