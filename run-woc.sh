cd MigrationHelperJAR || exit
git pull origin master
cd ..
/home/kylinxyl/jdk1.8.0_144/bin/java -Djava.library.path=/home/kylinxyl/lib \
     -Dlog4j.configuration=mylog4j.properties \
     -Dspoon.log.path=./spoon.log \
     -Dspring.profiles.active=woc \
     -Dmigration-helper.job.enabled=LibraryRecommendJob \
     -jar MigrationHelperJAR/migration-helper-1.0-SNAPSHOT.jar