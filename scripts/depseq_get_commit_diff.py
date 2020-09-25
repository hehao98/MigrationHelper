import logging
import pymongo
import subprocess
import multiprocessing
import oscar.oscar as oscar


EMPTY_BLOB = ""


def iter_c2fbb(chunk_id):
    if chunk_id < 0 or chunk_id >= 128:
        raise ValueError("Chunk ID should be [0, 128) for cf2bb")
    path = "/da2_data/basemaps/gz/c2fbbFullR{}.s".format(chunk_id)
    zcat = subprocess.Popen(["zcat", path], bufsize=10*1024*1024, stdout=subprocess.PIPE)
    for line in zcat.stdout:
        # code with utf-8 decoding is 3x slower, so we yield byte stream directly
        # line = line.decode("utf-8", "ignore") 
        info = line.strip().split(";")
        commit_sha = info[0]
        file_name = info[1]
        new_blob = info[2]
        if new_blob == "":
            new_blob = EMPTY_BLOB
        old_blob = EMPTY_BLOB
        if len(info) >= 4:
            old_blob = info[3]
        yield commit_sha, file_name, new_blob, old_blob


def iter_commits_with_diff(chunk_id, all_commits):
    current_sha = ""
    current_commit = {}
    for commit_sha, file_name, new_blob, old_blob in iter_c2fbb(chunk_id):
        if commit_sha not in all_commits:
            continue
        if commit_sha != current_sha:
            if current_sha != "":
                yield current_sha, current_commit
            current_sha = commit_sha
            current_commit = {
                "_id": commit_sha,
                "_class": "edu.pku.migrationhelper.data.woc.WocCommit",
                "error": False,
                "timestamp": "",
                "message": "",
                "parents": [],
                "diffs": []
            }
            try:
                woc_commit = oscar.Commit(current_sha)
                current_commit["timestamp"] = woc_commit.committed_at
                current_commit["parents"] = woc_commit.parent_shas
                current_commit["message"] = woc_commit.full_message.decode("utf-8", "ignore")
            except ValueError as e:
                logging.error("Error while loading commit info {}: {}".format(current_sha, e))
                current_commit["error"] = True
        elif file_name.endswith(".java") or file_name.endswith("pom.xml"):
            current_commit["diffs"].append({
                "oldBlob": old_blob,
                "newBlob": new_blob,
                "filename": file_name.decode("utf-8", "ignore")
            })
    yield current_sha, current_commit


def get_commit_with_diff(chunk_id, all_commits):
    logging.info("Chunk {}: Start".format(chunk_id))
    db = pymongo.MongoClient("mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper"
                           "?authSource=migration_helper").migration_helper
    bulk = []
    bulk_size = 10000
    total = 0
    for sha, commit in iter_commits_with_diff(chunk_id, all_commits):
        if len(commit["diffs"]) >= 3000:
            logging.warn("{} too large diff, truncate to 3000 pairs".format(sha))
            commit["diffs"] = commit["diffs"][0:3000]
        bulk.append(commit)
        if len(bulk) >= bulk_size:
            try:
                db.wocCommit.insert_many(bulk, ordered=False)
            except pymongo.errors.BulkWriteError:
                logging.warn("Duplicates were found.")
            del bulk[:]
            total += bulk_size
            logging.info("Chunk {}: {} commits added".format(chunk_id, total))
    if len(bulk) >= bulk_size:
        try:
            db.wocCommit.insert_many(bulk, ordered=False)
        except pymongo.errors.BulkWriteError:
            logging.warn("Duplicates were found.")
        del bulk[:]
        total += len(bulk)
        logging.info("Chunk {}: {} commits added in total".format(chunk_id, total))
    logging.info("Chunk {}: Finish".format(chunk_id))
        

if __name__ == "__main__":
    logging.basicConfig(
        format="%(asctime)s (Process %(process)d) [%(levelname)s] %(filename)s:%(lineno)d %(message)s",
        level=logging.INFO)

    logging.info("Start!")

    db = pymongo.MongoClient("mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper"
                           "?authSource=migration_helper").migration_helper
    commits = set()
    for woc_repo in db.wocRepository.find():
        commits.update(woc_repo["commits"])
    logging.info("{} commits in total".format(len(commits)))

    # get_commit_with_diff(0, commits)

    pool = multiprocessing.Pool(4)
    count = 0
    results = []
    for i in range(0, 128):
        results.append(pool.apply_async(get_commit_with_diff, (i, commits)))
    for result in results:
        result.get()
    pool.close()
    pool.join()

    logging.info("Finish!")


