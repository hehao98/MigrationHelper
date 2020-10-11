"""
This is a script for handling the legacy API Count values
We will load them from export/, and store them in the MongoDB
"""

import pymongo
import pandas as pd

db = pymongo.MongoClient("mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper"
                           "?authSource=migration_helper").migration_helper

libs = pd.read_csv("../export/GroupArtifact.csv").fillna("")
apis = pd.read_csv("../export/APISupport.csv")

id2lib = { i: g + ":" + a for i, g, a in zip(libs.id, libs.groupId, libs.artifactId) }

api_counts = []
for from_id, to_id, counter in zip(apis.fromId, apis.toId, apis.counter):
    api_counts.append({
        "fromLib": id2lib[from_id],
        "toLib": id2lib[to_id],
        "count": counter,
    })
    
db.wocAPICount.create_index([("fromLib", pymongo.ASCENDING), ("toLib", pymongo.ASCENDING)], unique=True)
db.wocAPICount.insert_many(api_counts)
