import pandas as pd
import itertools

rules = pd.read_csv("../evaluation/test-ground-truth-2014.csv").fillna("")

new_rules = []

for index, row in rules.iterrows():
    fgas = set(row.fromGroupArtifacts.split(";"))
    tgas = set(row.toGroupArtifacts.split(";"))
    for f in fgas:
        for t in tgas - fgas:
            if f == t:
                continue
            if f.split(":")[0] == t.split(":")[0]:
                continue
            x = {
                "fromLibrary": row.fromLibrary,
                "toLibrary": row.toLibrary,
                "fromGroupArtifact": f,
                "toGroupArtifact": t,
            }
            new_rules.append(x)

print("{} rules before extension".format(len(new_rules)))

new_rules = pd.DataFrame(new_rules)
new_rules.to_csv("../evaluation/possible-ground-truth-2014.csv", index=False)
