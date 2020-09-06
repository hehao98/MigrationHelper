from depseq_build3 import construct_blob_seq, construct_dep_seq

def test_simplest_case():
    commits = {
        0: {
            "_id": 0,
            "timestamp": "2020",
            "diffs": [
                {
                    "filename": "pom.xml",
                    "oldBlob": "",
                    "newBlob": "a",
                }
            ]
        }
    }
    pom_paths = ["pom.xml"]
    all_blobs = { 
        "a":    {
            "_id": "a",
            "error": "false",
            "dependencies": [
                {
                    "groupId": "A",
                    "artifactId": "B",
                    "version": "1.0"
                }
            ]

        } 
    }
    blob_seq = construct_blob_seq(commits, pom_paths)
    dep_seq = construct_dep_seq(pom_paths, blob_seq, all_blobs)
    print(blob_seq)
    print(dep_seq)