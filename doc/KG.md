# Modeling Java Open Source Libraries - A Knowledge Graph Approach

## Introduction

Modern software systems rely heavily on third-party libraries, and especially open source libraries in recent years. However, understanding and managing open source libraries is often challenging for software project leaders, developers and maintainers. One reason might be the exponential growth of open source libraries in recent years. (e.g. ~80k different Maven artifact versions in 2010, ~4M in 2020) Another reason might lie in the fragile and vulnerable nature of libraries, with problems including but not limited to security vulnerabilities, maintenance failures and backward incompatibilities. Traditionally, human experts play an important role in dependency management. However, the vast amount of available information might be overwhelming, and human decisions based on a limited set of information are prone to biases. Therefore, a data-driven understanding of software libraries, as an enhancement service for domain experts, is needed to facilitate library selection, curation and upgrade during the whole software lifecycle.

A knowledge graph, in the broadest sense, refers to a graph that encodes entities and their relationships. It is often built on top of existing databases and used for flexible query and reasoning over data. Domain specific knowledge graphs have been proven useful in a wide range of applications. In this document, we detail on how we mine knowledge of open source libraries and construct a domain specific knowledge graph for downstream applications. We have to note that, in the current implementation, all the data are still stored in MongoDB or MySQL, and we only provide an entity-relationship "view" of existing data. Future work might include storing the knowledge graph explicitly on a graph database.

The remainder of this document is organized as follows. We first introduce existing data sources that we use or mine from. Next, we introduce the data schema on MySQL and MongoDB, and an entity-relationship view of the available data. Finally, we discuss how we use these data for downstream applications. To ease our discussion and align tightly with the current implementation, we limit our discussion to Java libraries hosted on Maven Central Repository.

## Existing Data Sources

### Libraries.io Dataset

Libraries.io是由Tidelift公司

### Source Code, JAR and Documentation on Maven Central Repository

### Open Source Version Control Data from World of Code

## Intermediate Data

### Library and API Database on MongoDB



###  Repository Data on MySQL



### A Knowledge Graph (Entity-Relationship) View of Existing Data



## Downstream Applications

### Similar Library Recommendation

### API Change Computation and Library Upgrade Assistance

