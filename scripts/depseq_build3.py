# In this version, we try specifically to deal with the problem of parallel branches

import logging
import multiprocessing
import pymongo
import oscar.oscar as oscar
from datetime import datetime
from xml.etree import ElementTree
from depseq_get_pom_blobs import get_dep_java


def construct_blob_seq(commits, pom_paths):
    # For each pom.xml file,
    #   find all its versions,
    #   sort by time order (timestamp is the earliest time the blob occurs)
    blob_seq = { path: [] for path in pom_paths }
    for commit in sorted(list(commits.values()), key=lambda x: x["timestamp"]):
        for diff in commit["diffs"]:
            if diff["filename"] not in pom_paths:
                continue
            blob_seq[diff["filename"]].append((diff["newBlob"], commit["_id"], diff["oldBlob"]))
    # Since all the blob_seq is already sorted by time
    #  we only keep its first time of occurence, but keep the old blob sha for diffing
    for path in blob_seq:
        unique_blob_seq = []
        blob_set = set()
        for blob_sha, commit_sha, old_blob_sha in blob_seq[path]:
            if blob_sha not in blob_set and blob_sha != "":
                unique_blob_seq.append((blob_sha, commit_sha, old_blob_sha))
                blob_set.add(blob_sha)
        blob_seq[path] = unique_blob_seq
    logging.info("Different blobs for each path: {}".format({path: len(seq) for path, seq in blob_seq.items()}))
    return blob_seq


def try_add_blob(blob_sha, db):
    if blob_sha == "":
        return
    if db.wocPomBlob.find_one({"_id": blob_sha}) is not None:
        return
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
    try:
        db.wocPomBlob.insert_one(pom)
    except pymongo.errors.DocumentTooLarge:
        pom["dependencies"] = []
        pom["error"] = True
        db.wocPomBlob.insert_one(pom)


def get_all_blobs(blob_shas, db):
    blob_shas = list(blob_shas)
    n = 1000
    all_blobs = dict()
    for i in range(0, len(blob_shas), n):
        this_shas = blob_shas[i:min(i + n, len(blob_shas))]
        all_blobs.update({pom_blob["_id"]: pom_blob for pom_blob in db.wocPomBlob.find({"_id": {"$in": this_shas}})})
    return all_blobs


def construct_dep_seq(pom_paths, blob_seq, all_blobs):
    # Fill error blobs with content from previous verions,  
    #   and generate dependency change sequence by comparing pair by pair
    dep_seq = { path: [] for path in pom_paths }
    for path, seq in blob_seq.items():
        blobs = dict()
        for blob_sha, commit_sha, old_blob_sha in seq:
            blobs[blob_sha] = all_blobs[blob_sha]
            if old_blob_sha != "":
                blobs[old_blob_sha] = all_blobs[old_blob_sha]
        for blob_sha, commit_sha, old_blob_sha in seq:
            if blobs[blob_sha]["error"] and old_blob_sha != "":
                blobs[blob_sha] = blobs[old_blob_sha]
        for new_sha, commit_sha, old_sha in seq:
            if old_sha == "":
                old_libs = set()
                old_libs2ver = dict()
            else:
                old_libs = set(d["groupId"] + ":" + d["artifactId"] for d in blobs[old_sha]["dependencies"])
                old_libs2ver = {d["groupId"] + ":" + d["artifactId"]: d["version"] for d in blobs[old_sha]["dependencies"]}
            new_libs = set(d["groupId"] + ":" + d["artifactId"] for d in blobs[new_sha]["dependencies"])
            new_libs2ver = {d["groupId"] + ":" + d["artifactId"]: d["version"] for d in blobs[new_sha]["dependencies"]}
            change = {
                "commit": commit_sha,
                "oldBlob": old_sha,
                "newBlob": new_sha,
                "changes": [],
                "versionChanges": []
            }
            change["changes"].extend("+" + lib for lib in new_libs - old_libs)
            change["changes"].extend("-" + lib for lib in old_libs - new_libs)
            change["versionChanges"].extend("+" + lib + " " + new_libs2ver[lib] for lib in new_libs - old_libs)
            change["versionChanges"].extend("-" + lib + " " + old_libs2ver[lib] for lib in old_libs - new_libs)
            for lib in new_libs & old_libs:
                if old_libs2ver[lib] != new_libs2ver[lib]:
                    change["versionChanges"].append(" {} {}->{}".format(lib, old_libs2ver[lib], new_libs2ver[lib]))
            if len(change["changes"]) == 0 and len(change["versionChanges"]) == 0:
                continue
            dep_seq[path].append(change)
    return dep_seq


def build_depseq(woc_repo):
    logging.info("Start building dependency change sequence of {}".format(woc_repo["name"]))

    db = pymongo.MongoClient("mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper"
                           "?authSource=migration_helper").migration_helper

    # First step: load all commits and diffs related to this repo
    commits = dict()
    error = 0
    for commit in db.wocCommit.find({"_id": {"$in": woc_repo["commits"]}}):
        if type(commit["timestamp"]) == unicode or commit["timestamp"] is None or commit["error"] is True:
            error += 1
            continue
        commits[commit["_id"]] = commit
    logging.info("{}: {} commits ({} commits in database, {} errors)"
        .format(woc_repo["name"], len(woc_repo["commits"]), len(commits), error))

    # Second step: find number of different pom.xml files in the repo
    pom_paths = set()
    for sha, commit in commits.items():
        for diff in commit["diffs"]:
            if diff["filename"].endswith("pom.xml"):
                pom_paths.add(diff["filename"])
    logging.info("{}: {} different pom.xml files {}".format(woc_repo["name"], len(pom_paths), pom_paths))
    
    if db.wocDepSeq3.count_documents({"repoName": woc_repo["name"]}) == len(pom_paths):
        logging.info("Seems that {} is already constructed, skipping".format(woc_repo["name"]))
        return

    blob_seq = construct_blob_seq(commits, pom_paths)

    # For each blob, try to find the blobs and save in database
    blob_shas = set()
    for path in blob_seq:
        for blob_sha, commit_sha, old_blob_sha in blob_seq[path]:
            blob_shas.update([blob_sha, old_blob_sha])
            try_add_blob(blob_sha, db)
            try_add_blob(old_blob_sha, db)
    all_blobs = get_all_blobs(blob_shas, db)
    logging.info("{} blob shas in total, {} errors".format(len(blob_shas), 
        len([b for sha, b in all_blobs.items() if b["error"]])))

    dep_seq = construct_dep_seq(pom_paths, blob_seq, all_blobs)

    # Save the sequence to database
    for path, seq in dep_seq.items():
        if db.wocDepSeq3.find_one({"repoName": woc_repo["name"], "fileName": path}) is not None:
            continue
        try:
            db.wocDepSeq3.insert_one({
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

    db2 = pymongo.MongoClient("mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper"
                           "?authSource=migration_helper").migration_helper
    db2.wocDepSeq3.create_index([("repoName", pymongo.ASCENDING), ("fileName", pymongo.ASCENDING)], unique=True)
    
    #for woc_repo in db.wocRepository.find():
    #    build_depseq(woc_repo)
    
    pool = multiprocessing.Pool(4)
    results = []
    for woc_repo in db2.wocRepository.find():
        results.append(pool.apply_async(build_depseq, (woc_repo,)))
    for result in results:
        result.get()
    pool.close()
    pool.join()
    
    logging.info("Finish!")
