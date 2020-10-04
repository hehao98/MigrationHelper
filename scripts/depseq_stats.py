import pymongo
import logging

logging.basicConfig(
    format="%(asctime)s (Process %(process)d) [%(levelname)s] %(filename)s:%(lineno)d %(message)s",
    level=logging.INFO)

db = pymongo.MongoClient("mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper"
                        "?authSource=migration_helper").migration_helper

repos_with_depseq = set()
repo_files = set()
for depseq in db.wocDepSeq.find():
    repos_with_depseq.add(depseq["repoName"])
    repo_files.add((depseq["repoName"], depseq["fileName"]))
logging.info("{} repos, {} repo-file pairs".format(len(repos_with_depseq), len(repo_files)))

commits = set()
have_commits = db.wocCommit.count_documents({})
for woc_repo in db.wocRepository.find():
    commits.update(woc_repo["commits"])
logging.info("{} commits in total ({} in our database)".format(len(commits), have_commits))

pom_blobs = set()
pom_pairs = set()
java_pairs = set()
for count, commit in enumerate(db.wocCommit.find()):
    for diff in commit["diffs"]:
        if diff["filename"].endswith("pom.xml"):
            pom_pairs.add((diff["oldBlob"], diff["newBlob"]))
            pom_blobs.add(diff["oldBlob"])
            pom_blobs.add(diff["newBlob"])
        if diff["filename"].endswith(".java"):
            java_pairs.add((diff["oldBlob"], diff["newBlob"]))
    if count % 1000000 == 0:
        logging.info("{} commits inspected".format(count))
logging.info("{} different pom blobs, {} different pom pairs, {} different java pairs"
             .format(len(pom_blobs), len(pom_pairs), len(java_pairs)))
