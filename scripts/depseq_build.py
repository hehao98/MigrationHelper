import logging
import multiprocessing
import pymongo
import oscar.oscar as oscar
from xml.etree import ElementTree
from depseq_get_pom_blobs import get_dep_java


def build_depseq(woc_repo):
    logging.info("Start building dependency change sequence of {}".format(woc_repo["name"]))

    db = pymongo.MongoClient("mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper"
                           "?authSource=migration_helper").migration_helper

    # First step: load all commits and diffs related to this repo
    commits = dict()
    for commit in db.wocCommit.find({"_id": {"$in": woc_repo["commits"]}}):
        commits[commit["_id"]] = commit
    logging.info("{}: {} commits ({} commits in database)".format(woc_repo["name"], len(woc_repo["commits"]), len(commits)))

    # Second step: find number of different pom.xml files in the repo
    pom_paths = set()
    for sha, commit in commits.items():
        for diff in commit["diffs"]:
            if diff["filename"].endswith("pom.xml"):
                pom_paths.add(diff["filename"])
    logging.info("{}: {} different pom.xml files {}".format(woc_repo["name"], len(pom_paths), pom_paths))
    
    # Third step: for each pom.xml file,
    #   find all its versions,
    #   sort by time order (timestamp is the earliest time the blob occurs)
    blob_seq = { path: [] for path in pom_paths }
    for commit in sorted(list(commits.values()), key=lambda x: x["timestamp"]):
        for diff in commit["diffs"]:
            if diff["filename"] not in pom_paths:
                continue
            blob_seq[diff["filename"]].append((diff["newBlob"], commit["_id"]))
    for path in blob_seq:
        unique_blob_seq = []
        blob_set = set()
        for blob_sha, commit_sha in blob_seq[path]:
            if blob_sha not in blob_set and blob_sha != "":
                unique_blob_seq.append((blob_sha, commit_sha))
                blob_set.add(blob_sha)
        blob_seq[path] = unique_blob_seq
    logging.info("Different blobs for each path: {}".format({path: len(seq) for path, seq in blob_seq.items()}))

    # For each blob, try to find the blobs and save in database
    missing_count = 0
    missing_without_error = 0
    for path in blob_seq:
        for blob_sha, commit_sha in blob_seq[path]:
            if db.wocPomBlob.find_one({"_id": blob_sha}) is not None:
                continue
            missing_count += 1
            pom = {
                "_id": blob_sha,
                "_class": "edu.pku.migrationhelper.data.woc.WocPomBlob",
                "error": False,
                "dependencies": []
            }
            try:
                dependencies = get_dep_java(oscar.Blob(str(blob_sha)).data)
            except (TypeError, ElementTree.ParseError, LookupError) as e: # Missing blob, or malformed pom.xml
                pom["error"] = True
                dependencies = {}
            for ga_id, version in dependencies.items():
                # Skip erronous data
                if len(ga_id) >= 1000 or len(version) >= 1000:
                    logging.warn("Too long groupId/artifactId/version, which indicates corrupted data, skipping")
                    continue
                pom["dependencies"].append({
                    "groupId": ga_id.split(":")[0],
                    "artifactId": ga_id.split(":")[1],
                    "version": version
                })
            if not pom["error"]:
                missing_without_error += 1
            db.wocPomBlob.insert_one(pom)
    logging.info("{} missing blobs in which {} are added without error".format(missing_count, missing_without_error))

    # Final step: skip error blob versions,  
    #   and generate dependency change sequence using the remaining versions
    dep_seq = { path: [] for path in pom_paths }
    for path, seq in blob_seq.items():
        blob_seq = [b for b, c in seq]
        blobs = {pom_blob["_id"]: pom_blob for pom_blob in db.wocPomBlob.find({"_id": {"$in": blob_seq}})}
        logging.info(u"{}: {} blobs ({} in database)".format(path, len(seq), len(blobs)))
        seq = [(s, cs) for s, cs in seq if s in blobs and blobs[s]["error"] == False]
        for i in range(0, len(seq)):
            if i == 0:
                old_sha = ""
                old_libs = set()
            else:
                old_sha = seq[i - 1][0]
                old_libs = set(d["groupId"] + ":" + d["artifactId"] for d in blobs[old_sha]["dependencies"])
            new_sha = seq[i][0]
            commit_sha = seq[i][1]
            new_libs = set(d["groupId"] + ":" + d["artifactId"] for d in blobs[new_sha]["dependencies"])
            if new_libs == old_libs:
                continue
            change = {
                "commit": commit_sha,
                "oldBlob": old_sha,
                "newBlob": new_sha,
                "changes": []
            }
            change["changes"].extend("+" + lib for lib in new_libs - old_libs)
            change["changes"].extend("-" + lib for lib in old_libs - new_libs)
            dep_seq[path].append(change)

    
    # Save the sequence to database
    for path, seq in dep_seq.items():
        db.wocDepSeq.insert_one({
            "_class": "edu.pku.migrationhelper.data.woc.WocDepSeq",
            "repoName": woc_repo["name"],
            "fileName": path,
            "seq": seq
        })
    logging.info("{}: Finish".format(woc_repo["name"]))


if __name__ == "__main__":
    logging.basicConfig(
        format="%(asctime)s (Process %(process)d) [%(levelname)s] %(filename)s:%(lineno)d %(message)s",
        level=logging.INFO)

    logging.info("Start!")

    db = pymongo.MongoClient("mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper"
                           "?authSource=migration_helper").migration_helper
    db.wocDepSeq.create_index([("repoName", pymongo.ASCENDING), ("fileName", pymongo.ASCENDING)], unique=True)
    
    #for woc_repo in db.wocRepository.find():
        #build_depseq(woc_repo)
    
    pool = multiprocessing.Pool(16)
    results = []
    for woc_repo in db.wocRepository.find():
        results.append(pool.apply_async(build_depseq, (woc_repo,)))
    for result in results:
        result.get()
    pool.close()
    pool.join()
    
    logging.info("Finish!")
