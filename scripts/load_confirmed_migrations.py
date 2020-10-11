"""
Load the manually annotated confirmed migrations to MongoDB
"""

import pymongo
import pandas as pd

db = pymongo.MongoClient("mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper"
                           "?authSource=migration_helper").migration_helper

migrations = pd.read_excel("../evaluation/manual/confirmed-migrations-all.xlsx")
db.wocConfirmedMigration.insert_many(migrations.to_dict("records"))