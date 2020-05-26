import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

originData = pd.read_csv('RQ3.csv')

cols = ['APISupport', 'APIRank0']
ranges = [np.arange(0, 1000, 1), np.arange(0, 1.1, 0.01)]
conds = ['isTruth', 'not isTruth', '']
condNames = ['true', 'false', 'all']
for i in [0, 1]:
	suffix = '.png'
	data = originData.query('patternSupport >= 8')
	for j in [0, 1, 2]:
		plt.clf()
		currData = data
		if conds[j] != '':
			currData = data.query(conds[j])
		print(currData.shape[0])
		print(currData.query(cols[i]+'==0').shape[0])
		currData = currData.query(cols[i]+'>0')
		x = currData[cols[i]].tolist()
		bins = ranges[i]
		plt.hist(x,bins,color='blue',alpha=0.5)
		plt.xlabel(cols[i])
		plt.ylabel("count")
		plt.savefig('RQ3-'+cols[i]+'-'+condNames[j]+suffix, dpi=288)	
		
print('success')

