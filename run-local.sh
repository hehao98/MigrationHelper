if [ "$1" != "CreateTableJob" ] && [ "$1" != "LibrariesIoImportJob" ] \
   && [ "$1" != "LioJarParseJob" ] && [ "$1" != "LibraryUpgradeRecommendJob" ] \
   && [ "$1" != "MongoDbInitializeJob" ] && [ "$1" != "TestJob" ]; then
  echo "Usage: run-local.sh <Job Name> <Arg1> <Arg2> <Arg3> <Arg4> ..."
  echo "Currently Supported Jobs: LibrariesIoImportJob, LioJarParseJob, LibraryUpgradeRecommendJob, "
  echo "                          MongoDbInitializeJob"
  echo "See README.md for usage examples"
  exit
fi

mvn clean package -DskipTests

job=$1
shift 1
echo "MigrationHelper: Running Job $job..."

if [ "$job" == "LioJarParseJob" ]; then
  java -Xms8g -Xmx12g -XX:+UseG1GC -XX:ParallelGCThreads=8 \
       -XX:ConcGCThreads=4 -XX:MaxGCPauseMillis=600000 -XX:+UnlockExperimentalVMOptions \
       -XX:G1NewSizePercent=20 -XX:G1MaxNewSizePercent=20 \
       -Djava.library.path=/home/heh/lib \
       -Dlog4j.configuration=mylog4j.properties \
       -Dspoon.log.path=./spoon.log \
       -Dspring.profiles.active=local \
       -Dmigration-helper.job.enabled="$job" \
       -jar target/migration-helper-1.0-SNAPSHOT.jar "$@"
else
  java -Djava.library.path=/home/heh/lib \
       -Dlog4j.configuration=mylog4j.properties \
       -Dspoon.log.path=./spoon.log \
       -Dspring.profiles.active=local \
       -Dmigration-helper.job.enabled="$job" \
       -jar target/migration-helper-1.0-SNAPSHOT.jar "$@"
fi