import logging
import multiprocessing
import pymongo
import oscar.oscar as oscar
from datetime import datetime
from xml.etree import ElementTree
from depseq_get_pom_blobs import get_dep_java


def add_pom_blob(db, blob_sha):
    if blob_sha == "":
        return False
    if db.wocPomBlob.find_one({"_id": blob_sha}) is not None:
        return True
    pom = {
        "_id": blob_sha,
        "_class": "edu.pku.migrationhelper.data.woc.WocPomBlob",
        "error": False,
        "dependencies": []
    }
    try:
        dependencies = get_dep_java(oscar.Blob(str(blob_sha)).data)
    except (TypeError, ElementTree.ParseError, LookupError, ValueError) as e: # Missing blob, or malformed pom.xml
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
    db.wocPomBlob.insert_one(pom)
    if pom["error"]:
        return False
    return True


def build_depseq(woc_repo):
    logging.info("Start building dependency change sequence of {}".format(woc_repo["name"]))

    db = pymongo.MongoClient("mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper"
                           "?authSource=migration_helper").migration_helper

    # First step: load all commits and diffs related to this repo
    commits = dict()
    error = 0
    for commit in db.wocCommit.find({"_id": {"$in": woc_repo["commits"]}}):
        if type(commit["timestamp"]) == unicode or commit["timestamp"] is None or commit["error"] == True:
            error += 1
            continue
        commits[commit["_id"]] = commit
    logging.info("{}: {} commits ({} commits in database, {} errors)"
        .format(woc_repo["name"], len(woc_repo["commits"]), len(commits), error))

    # find number of different pom.xml files in the repo
    pom_paths = set()
    for sha, commit in commits.items():
        for diff in commit["diffs"]:
            if diff["filename"].endswith("pom.xml"):
                pom_paths.add(diff["filename"])
    logging.info("{}: {} different pom.xml files {}".format(woc_repo["name"], len(pom_paths), pom_paths))

    # for each pom.xml file, find all blob pairs and the commits in them
    path2commits = { path: [] for path in pom_paths }
    for commit in sorted(list(commits.values()), key=lambda x: x["timestamp"]):
        for diff in commit["diffs"]:
            if diff["filename"] not in pom_paths:
                continue
            path2commits[diff["filename"]].append((diff["newBlob"], diff["oldBlob"], commit["_id"]))

    # For each blob, try to find the blobs and save in database
    for path in path2commits:
        for new_blob, old_blob, commit_sha in path2commits[path]:
            add_pom_blob(db, new_blob)
            add_pom_blob(db, old_blob)

    # Fourth step: for each commit sequence sorted by time, we build dependency change sequence
    #   by iterating non-merge commits
    path2depseq = { path: [] for path in pom_paths }
    for path in path2commits:
        blob_seq = set([b for b, ob, c in path2commits[path]] + [ob for b, ob, c in path2commits[path]])
        blobs = {pom_blob["_id"]: pom_blob for pom_blob in db.wocPomBlob.find({"_id": {"$in": list(blob_seq)}})}
        for new_blob, old_blob, commit_sha in path2commits[path]:
            if len(commits[commit_sha]["parents"]) > 1:
                continue
            if new_blob == "":
                new_libs = set()
            else:
                new_libs = set(d["groupId"] + ":" + d["artifactId"] for d in blobs[new_blob]["dependencies"])
            if old_blob == "":
                old_libs = set()
            else:
                old_libs = set(d["groupId"] + ":" + d["artifactId"] for d in blobs[old_blob]["dependencies"])
            if new_libs == old_libs:
                continue
            change = {
                "commit": commit_sha,
                "oldBlob": old_blob,
                "newBlob": new_blob,
                "changes": []
            }
            change["changes"].extend("+" + lib for lib in new_libs - old_libs)
            change["changes"].extend("-" + lib for lib in old_libs - new_libs)
            path2depseq[path].append(change)

    # Save the sequence to database
    for path, seq in path2depseq.items():
        try:
            db.wocDepSeq2.insert_one({
                "_class": "edu.pku.migrationhelper.data.woc.WocDepSeq",
                "repoName": woc_repo["name"],
                "fileName": path,
                "seq": seq
            })
        except pymongo.errors.DocumentTooLarge:
            logging.error("{} {} dep seq too large, will not be inserted to db".format(woc_repo["name"], path))
    logging.info("{}: Finish".format(woc_repo["name"]))


if __name__ == "__main__":
    logging.basicConfig(
        format="%(asctime)s (Process %(process)d) [%(levelname)s] %(filename)s:%(lineno)d %(message)s",
        level=logging.INFO)

    logging.info("Start!")

    db = pymongo.MongoClient("mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper"
                           "?authSource=migration_helper").migration_helper
    db.wocDepSeq2.create_index([("repoName", pymongo.ASCENDING), ("fileName", pymongo.ASCENDING)], unique=True)
    
    #for woc_repo in db.wocRepository.find():
    #    build_depseq(woc_repo)
    
    pool = multiprocessing.Pool(8)
    results = []
    for woc_repo in db.wocRepository.find():
        results.append(pool.apply_async(build_depseq, (woc_repo,)))
    for result in results:
        result.get()
    pool.close()
    pool.join()
    
    logging.info("Finish!")
