import re
import logging
import pymongo
import multiprocessing
import subprocess
import oscar.oscar as oscar
from xml.etree import ElementTree
from typing import Dict


def replace_variables_in_pom(text: str, properties: Dict[str, str]) -> str:
    pattern = re.compile(r"\${([^}]+)\}")
    for s in pattern.findall(text):
        if s in properties:
            text = text.replace("${" + s + "}", properties[s])
    return text


def get_dep_java(pom_xml: bytes) -> Dict[str, str]:
    if pom_xml is None:
        return {}
    result = {}

    namespaces = {'xmlns': 'http://maven.apache.org/POM/4.0.0'}
    root = ElementTree.fromstring(pom_xml)
    properties = {}
    properties_node = root.find(".//xmlns:properties", namespaces=namespaces)
    if properties_node is not None:
        for prop in properties_node:
            tag = prop.tag
            i = tag.find('}')
            if i >= 0:
                tag = tag[i + 1:]
            properties[tag] = prop.text

    deps = root.findall(".//xmlns:dependency", namespaces=namespaces)
    for d in deps:
        group_id = d.find("xmlns:groupId", namespaces=namespaces)
        artifact_id = d.find("xmlns:artifactId", namespaces=namespaces)
        version = d.find("xmlns:version", namespaces=namespaces)
        group_id_text = ""
        artifact_id_text = ""
        version_text = ""
        if group_id is not None and group_id.text is not None:
            group_id_text = replace_variables_in_pom(group_id.text, properties)
        if artifact_id is not None and artifact_id.text is not None:
            artifact_id_text = replace_variables_in_pom(artifact_id.text, properties)
        if version is not None and version.text is not None:
            version_text = replace_variables_in_pom(version.text, properties)
        result[group_id_text + ":" + artifact_id_text] = version_text
    return result


def iter_bbcf(chunk_id: int):
    if chunk_id < 0 or chunk_id >= 128:
        raise ValueError("Chunk ID should be [0, 128) for cf2bb")
    path = "/da3_data/basemaps/gz/obbcfFullR{}.s".format(chunk_id)
    zcat = subprocess.Popen(["zcat", path], bufsize=10*1024*1024, stdout=subprocess.PIPE)
    for line in zcat.stdout:
        # code with utf-8 decoding is 3x slower, so we yield byte stream directly
        # line = line.decode("utf-8", "ignore")
        info = line.strip().split(b";")
        commit_sha = info[2]
        file_name = info[3]
        new_blob = info[0]
        old_blob = info[1]
        yield commit_sha, file_name, new_blob, old_blob


def extract_pom_blobs(chunk_id: int):
    logging.info("Chunk {}: Start".format(chunk_id))
    db = pymongo.MongoClient("mongodb://migration_helper:HeHMgt2020@da1.eecs.utk.edu:27020/migration_helper"
                             "?authSource=migration_helper")
    results = []
    insert_limit = 10000
    total = 0
    for commit_sha, file_name, new_blob, old_blob in iter_bbcf(chunk_id):
        if not file_name.endswith(b"pom.xml"):
            continue
        pom = {
            "_id": new_blob,
            "_class": "edu.pku.migrationhelper.data.woc.PomBlob",
            "dependencies": []
        }
        for ga_id, version in get_dep_java(oscar.Blob(new_blob.decode("utf-8", "ignore")).data).items():
            pom["dependencies"].append({
                "groupId": ga_id.split(":")[0],
                "artifactId": ga_id.split(":")[1],
                "version": version
            })
        if len(results) >= insert_limit:
            db.pomBlob.insert_many(results)
            results.clear()
            total += insert_limit
            logging.info("Chunk {}: {} entries added".format(chunk_id, total))
    if len(results) > 0:
        db.pomBlob.insert_many(results)
        total += len(results)
    logging.info("Chunk {}: Success".format(chunk_id))
    return


if __name__ == "__main__":
    logging.basicConfig(
        format="%(asctime)s (Process %(process)d) [%(levelname)s] %(filename)s:%(lineno)d %(message)s",
        level=logging.INFO)

    logging.info("Start!")
    pool = multiprocessing.Pool(4)
    count = 0
    results = []
    extract_pom_blobs(0)
    # for i in range(0, 128):
    #     results.append(pool.apply_async(extract_pom_blobs, (i,)))
    for result in results:
        result.get()
    pool.close()
    pool.join()
    logging.info("Finish!")





