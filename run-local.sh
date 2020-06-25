if [ "$1" != "CreateTableJob" ] && [ "$1" != "LibrariesIoImportJob" ] \
   && [ "$1" != "LioJarParseJob" ] && [ "$1" != "TestJob" ]; then
  echo "Usage: run-local.sh <Job Name> <Arg1> <Arg2> <Arg3> <Arg4> ..."
  echo "Currently Supported Jobs: CreateTableJob, LibrariesIoImportJob, LioJarParseJob, TestJob"
  echo "See README.md for usage examples"
  exit
fi

mvn clean package -DskipTests

job=$1
shift 1
echo "MigrationHelper: Running Job $job..."
java -Djava.library.path=/home/heh/lib \
     -Dlog4j.configuration=mylog4j.properties \
     -Dspoon.log.path=./spoon.log \
     -Dspring.profiles.active=local \
     -Dmigration-helper.job.enabled="$job" \
     -jar target/migration-helper-1.0-SNAPSHOT.jar "$@"