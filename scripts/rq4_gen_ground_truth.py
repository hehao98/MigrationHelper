import sys
import pymongo
import pandas as pd
from collections import defaultdict

db = pymongo.MongoClient("mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper"
                           "?authSource=migration_helper").migration_helper

migration_file = sys.argv[1]
output_file = sys.argv[2]

print("Read migrations from {} and output ground truth to {}".format(migration_file, output_file))

migrations = pd.read_excel(migration_file)
results = []
for from_lib, to_lib in sorted(set(zip(migrations["fromLib"], migrations["toLib"]))):
    print(from_lib, to_lib)
    
    from_lib_info = db.lioProject.find_one({ "name": from_lib })
    if from_lib_info is None:
        print("WARNING: {} does not exist in MongoDB Libraries.io data!".format(from_lib))
        from_lib_info = defaultdict(str)
        
    to_lib_info = db.lioProject.find_one({ "name": to_lib })
    if to_lib_info is None:
        print("WARNING: {} does not exist in MongoDB Libraries.io data!".format(to_lib))
        to_lib_info = defaultdict(str)
    results.append({
        "fromLib": from_lib,
        "toLib": to_lib,
        "isConfirmed": True,
        "fromLibHomePageURL": from_lib_info["homepageUrl"],
        "toLibHomePageURL": to_lib_info["homepageUrl"],
        "fromLibDescription": from_lib_info["description"],
        "toLibDescription": to_lib_info["description"],
        "fromLibRepositoryURL": from_lib_info["repositoryUrl"],
        "toLibRepositoryURL": to_lib_info["repositoryUrl"],
        "fromLibRepositoryDescription": from_lib_info["repositoryDescription"],
        "toLibRepositoryDescription": to_lib_info["repositoryDescription"],
    })
pd.DataFrame(results).sort_values(by=["fromLib", "toLib"]).to_excel(output_file, index=False)
