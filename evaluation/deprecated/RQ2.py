import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

plt.rcParams["figure.figsize"] = (5,4)
originData = pd.read_csv('RQ2-truth-position.csv')

cols = ['distance', 'total']
ranges = [np.arange(1, 500, 1), np.arange(1, 500, 1)]
conds = ['isTruth', 'not isTruth', '']
condNames = ['true', 'false', 'all']
for i in [0, 1]:
	suffix = '.png'
	data = originData
	for j in [0, 1, 2]:
		plt.clf()
		currData = data
		if conds[j] != '':
			currData = data.query(conds[j])
		print(currData.shape[0])
		x = currData[cols[i]].tolist()
		bins = ranges[i]
		plt.hist(x,bins,color='blue',alpha=0.5)
		plt.xlabel(cols[i])
		plt.ylabel("count")
		plt.subplots_adjust(left=0.2, right=0.9, top=0.9, bottom=0.1)
		plt.savefig('RQ2-'+cols[i]+'-'+condNames[j]+suffix, dpi=288)

suffix = '(1-pos).png'
data = originData
for j in [0, 1, 2]:
	plt.clf()
	currData = data
	if conds[j] != '':
		currData = data.query(conds[j])
	print(currData.shape[0])
	x = (6/(currData['distance']+5)).tolist()
	bins = np.arange(0, 2, 0.01)
	plt.hist(x,bins,color='blue',alpha=0.5)
	plt.xlabel('distanceRank')
	plt.ylabel("count")
	plt.savefig('RQ2-'+'pos'+'-'+condNames[j]+suffix, dpi=288)		
		
print('success')

