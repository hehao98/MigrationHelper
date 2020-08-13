import logging
import pymongo
import oscar.oscar as oscar


if __name__ == "__main__":
    logging.basicConfig(
        format="%(asctime)s (Process %(process)d) [%(levelname)s] %(filename)s:%(lineno)d %(message)s",
        level=logging.INFO)

    logging.info("Start!")

    db = pymongo.MongoClient("mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper"
                             "?authSource=migration_helper").migration_helper
    
    query = { "hostType": "GitHub", "fork": False, "language": "Java", "starsCount": {"$gt": 10} }
    query_count = db.lioRepository.count_documents(query)
    logging.info("{} repository selected".format(query_count))
    
    db.wocRepository.create_index([("name", pymongo.ASCENDING)], unique=True)
    for lio_repo in db.lioRepository.find(query).sort("starsCount", pymongo.DESCENDING):
        name = lio_repo["nameWithOwner"].replace("/", "_")
        if db.wocRepository.find_one({"name": name}) is not None:
            logging.info("{} already exists in the database".format(name))
            continue
        woc_repo = { 
            "_id": lio_repo["_id"],
            "_class": "edu.pku.migrationhelper.data.woc.WocRepository",
            "name": name,
            "commits": []
        }
        woc_repo["commits"] = list(oscar.Project(str(name)).commit_shas)
        logging.info("{}: {} commits".format(name, len(woc_repo["commits"])))

        try:
            db.wocRepository.insert_one(woc_repo)
        except pymongo.errors.DocumentTooLarge:
            logging.error("{} has way too many commits to be included in our database".format(name))
            logging.error("it will be stored as a dummy entry with 0 commits")
            woc_repo["commits"] = []
            db.wocRepository.insert_one(woc_repo)
    logging.info("Finish!")
