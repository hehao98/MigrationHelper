# Manual

This folder contains important manual dataset that is crafted from significant manual efforts.
Do NOT delete or overwrite any files in this folder! 

`confirmed-migrations-initial-example.xlsx` is the initial set of ground truth migration we collected from the 5998 stratified sampled commit pairs in `possible-migrations-sampled.xlsx`.

`possible-migrations-filtered-annotated.xlsx` is 5200 highly suspicious migrations which is also annotated by whether the author think it is a real migration (`isTrue`) and whether the original rule is not real but can be fixed with a real rule (`fixed`).

`confirmed-migrations.xlsx` should be the final set of our migration dataset. It is currently a combination of `confirmed-migrations-initial-example.xlsx` and `possible-migrations-filtered-annotated.xlsx`. In the future, we plan to add many more data to it.

