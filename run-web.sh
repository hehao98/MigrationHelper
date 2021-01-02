echo "Pulling Latest Version..."
cd MigrationHelperJAR || exit
git pull origin master
cd ..

/home/ubuntu/jdk1.8.0_144/bin/java -Xms4g -Xmx5g -Dlog4j.configuration=mylog4j.properties \
       -Dspoon.log.path=./spoon.log \
       -Dspring.profiles.active=web \
       -jar MigrationHelperJAR/migration-helper-1.0-SNAPSHOT.jar --web "$@"
