import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

plt.rcParams["figure.figsize"] = (5,4)
originData = pd.read_csv('RQ1-pomOnly.csv')

cols = ['patternSupport', 'hot', 'hot', 'hotRank']
ranges = [np.arange(1, 50, 1), np.arange(0, 1, 0.01), np.arange(0, 1, 0.01), np.arange(0, 1, 0.01)]
conds = ['isTruth', 'not isTruth', '']
condNames = ['true', 'false', 'all']
for i in [0, 1, 2, 3]:
	suffix = '.png'
	data = originData
	if i == 1:
		data = originData.query('patternSupport >= 0')
		suffix = '-small.png'
	if i == 2:
		data = originData.query('patternSupport >= 8')
		suffix = '-big.png'
	if i == 3:
		data = originData.query('patternSupport >= 8')
	for j in [0, 1, 2]:
		plt.clf()
		currData = data
		if conds[j] != '':
			currData = data.query(conds[j])
		print(currData.shape[0])
		x = currData[cols[i]].tolist()
		bins = ranges[i]
		plt.hist(x,bins,color='blue',alpha=0.5)
		labelmapping = {"patternSupport": "RuleFreq", "hot": "Relative Rule Frequency (RRF)", "hotRank": "Popularity Regularization (PR)"}
		plt.xlabel(labelmapping[cols[i]])
		plt.ylabel("count")
		plt.subplots_adjust(left=0.2, right=0.9, top=0.9, bottom=0.1)
		plt.savefig('RQ1-'+cols[i]+'-'+condNames[j]+suffix, dpi=288)

truthCount = originData.query('isTruth').shape[0]
notTruthCount = originData.query('not isTruth').shape[0]
x = range(1,21)
falseRemain = []
trueRemain = []
precision = []
recall = []
fmeasure = []
for i in x:
	currData = originData.query('patternSupport >= ' + str(i))
	currTotal = currData.shape[0]
	currTruth = currData.query('isTruth').shape[0]
	currFalse = currData.query('not isTruth').shape[0]
	p = currTruth / float(currTotal)
	r = currTruth / float(truthCount)
	f = 2 * p * r / (p + r)
	precision.append(p*5)
	recall.append(r)
	fmeasure.append(f*5)
	falseRemain.append(currFalse / float(notTruthCount))
	trueRemain.append(currTruth / float(truthCount))
plt.clf();
plt.plot(x,precision,'s-',color = 'r',label="precision x 5")#s-:方形
plt.plot(x,recall,'o-',color = 'b',label="recall")#o-:圆形
plt.plot(x,fmeasure,'^-',color = 'g',label="F-measure x 5")
plt.xlabel("patternSupport")
plt.ylabel("percent")
plt.legend(loc = "best")#图例
plt.savefig('RQ1-patternSupport-greater-than-f1.png', dpi=288)

plt.clf();
plt.plot(x,falseRemain,'s-',color = 'r',label="falseRemain")#s-:方形
plt.plot(x,trueRemain,'o-',color = 'g',label="trueRemain")#o-:圆形
plt.xlabel("patternSupport")
plt.ylabel("percent")
plt.legend(loc = "best")#图例
plt.savefig('RQ1-patternSupport-greater-than-remain.png', dpi=288)

print('success')

