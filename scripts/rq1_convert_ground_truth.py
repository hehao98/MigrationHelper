import pandas as pd
import itertools
from collections import defaultdict

rules = pd.read_csv("../evaluation/ground-truth-2014.csv").fillna("")

new_rules = set()
ga2lib = dict()

for index, row in rules.iterrows():
    fgas = set(row.fromGroupArtifacts.split(";"))
    tgas = set(row.toGroupArtifacts.split(";"))
    for f in fgas:
        for t in tgas - fgas:
            if f == t:
                continue
            if f.split(":")[0] == t.split(":")[0]:
                continue
            if f == "" or t == "":
                continue
            new_rules.add((f, t))
            ga2lib[f] = row.fromLibrary
            ga2lib[t] = row.toLibrary

print("{} rules before extension".format(len(new_rules)))

# (A, B) is rule -> (B, A) is rule
new_rules.update([(b, a) for a, b in new_rules])

# (A, B) is rule and (B, C) is rule -> (A, C) is rule
fromlib2tolibs = defaultdict(set)
lib2name = dict()
for f, t in new_rules:
    fromlib2tolibs[f].add(t)
while True:
    rules_to_add = set()
    for a, b in new_rules:
        if b in fromlib2tolibs:
            for c in fromlib2tolibs[b]:
                rules_to_add.add((a, c))
    if all(x in new_rules for x in rules_to_add):
        break
    new_rules.update(rules_to_add)

print("{} rules after extension".format(len(new_rules)))

df = []
for f, t in new_rules:
    if f.split(":")[0] == t.split(":")[0]:
        continue
    if ga2lib[f] == ga2lib[t]:
        continue
    df.append({
        "fromLibrary": ga2lib[f],
        "toLibrary": ga2lib[t],
        "fromGroupArtifact": f,
        "toGroupArtifact": t,
    })
print("{} rules after removing same group id rules and same library rules".format(len(df)))
df = pd.DataFrame(df).drop_duplicates().sort_values(by="fromLibrary")
df.to_csv("../evaluation/possible-ground-truth-2014.csv", index=False)
