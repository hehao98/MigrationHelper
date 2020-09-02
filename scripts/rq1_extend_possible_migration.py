# Run this on da4
import oscar.oscar as oscar
import pandas as pd

def get_commit_message(sha):
    return oscar.Commit(sha).full_message.decode("utf-8", "ignore")

def get_commit_time(sha):
    return oscar.Commit(sha).committed_at

def get_parent_count(sha):
    return len(oscar.Commit(sha).parent_shas)

df = pd.read_csv("../evaluation/possible-migrations-sampled.csv")

df["startCommitMessage"] = df["startCommit"].map(get_commit_message)
df["endCommitMessage"] = df["endCommit"].map(get_commit_message)
df["startCommitTime"] = df["startCommit"].map(get_commit_time)
df["endCommitTime"] = df["endCommit"].map(get_commit_time)

df.to_csv("../evaluation/possible-migrations-sampled.csv", index=False, encoding="utf-8")

