import pymongo
import pandas as pd

db = pymongo.MongoClient("mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper"
                           "?authSource=migration_helper").migration_helper

with open("libs_all.csv", "w") as f:
    f.write("_id,groupId,artifactId\n")
    for lib in db.libraryGroupArtifact.find():
        f.write("{},{},{}\n".format(lib["_id"], lib["groupId"], lib["artifactId"]))
