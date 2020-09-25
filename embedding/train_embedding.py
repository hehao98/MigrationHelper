# this should be run with Python3, with gensim and Cython installed
# the best way of preparing such an environment is by using anaconda destribution

import os
import random
import logging
import csv
import json
import pandas as pd
import gensim
import subprocess
from gensim.models.doc2vec import Doc2Vec, TaggedDocument


class MavenArtifactCorpus(object):
    def __init__(self, data_chunks):
        self.data_chunks = data_chunks
    def __iter__(self):
        path = "/da1_data/play/heh/dbdump/java/blob2pom.{}.gz"
        doc_index = 0
        for i in range(0, self.data_chunks):
            zcat = subprocess.Popen(["zcat", path.format(i)], bufsize=10*1024*1024, stdout=subprocess.PIPE)
            for line in zcat.stdout:
                info = line.strip().split(b";")
                blob_sha = info[0]
                pom_info = json.loads(line[len(blob_sha) + 1:], encoding="ascii")
                if pom_info["has_error"]:
                    continue
                document = pom_info["dependencies"].keys()
                yield TaggedDocument(document, [doc_index])
                doc_index += 1


class LibrariesIOCorpus(object):
    def __init__(self, min_len):
        self.min_len = min_len
    def __iter__(self):
        path = "../export/LibrariesIOProjectDependencies.csv"
        with open(path, "r") as f:
            reader = csv.DictReader(f)
            prev = ""
            curr_doc = []
            doc_index = 0
            for row in reader:
                curr = row["Repository Name with Owner"] + row["Manifest Filepath"] + row["Git branch"]
                if curr != prev:
                    if len(curr_doc) >= self.min_len:
                        yield TaggedDocument(curr_doc, [doc_index])
                    doc_index += 1
                    prev = curr
                    curr_doc = []
                curr_doc.append(row["Dependency Project Name"])

                
if __name__ == "__main__":
    logging.basicConfig(
        format="%(asctime)s (Process %(process)d) [%(levelname)s] %(filename)s:%(lineno)d %(message)s",
        level=logging.INFO,
        handlers=[logging.StreamHandler()])

    logging.info("Start!")
    data_chunks = 128
    min_len = 3
    vector_size = 200
    window = 100
    min_count = 100
    negative = 5
    epochs = 20
    assert gensim.models.doc2vec.FAST_VERSION > -1
    #model = Doc2Vec(MavenArtifactCorpus(data_chunks), dm=1, dm_mean=1, dbow_words=1, workers=8,
    #    vector_size=vector_size, window=window, min_count=min_count, negative=negative, epochs=epochs)
    #model.save("doc2vec.maven_artifact.{}.{}.{}.{}.{}".format(vector_size, window, min_count, negative, epochs))
    #for doc in LibrariesIOCorpus():
    #    print(doc)
    model = Doc2Vec(LibrariesIOCorpus(min_len), dm=1, dm_mean=1, dbow_words=1, workers=8,
        vector_size=vector_size, window=window, min_count=min_count, negative=negative, epochs=epochs)
    model.save("doc2vec.libraries.io.{}.{}.{}.{}.{}.{}".format(vector_size, window, min_count, negative, epochs, min_len))
    


