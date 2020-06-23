if [ "$1" != "CreateTableJob" ] && [ "$1" != "LibrariesIoImportJob" ] && [ "$1" != "LioJarParseJob" ] \
   && [ "$1" != "DataExportJob" ] && [ "$1" != "LibraryRecommendJob" ]; then
  echo "Usage: run-woc.sh <Job Name> <Arg1> <Arg2> <Arg3> <Arg4>"
  echo "Currently Supported Jobs: CreateTableJob, LibrariesIoImportJob, LioJarParseJob, "
  echo "                          DataExportJob, LibraryRecommendJob"
  echo "See README.md for usage examples"
  exit
fi

echo "Pulling Latest Version..."
cd MigrationHelperJAR || exit
git pull origin master
cd ..

job=$1
shift 1
echo "MigrationHelper: Running Job $job..."
/home/heh/jdk1.8.0_144/bin/java -Djava.library.path=/home/heh/lib \
     -Dlog4j.configuration=mylog4j.properties \
     -Dspoon.log.path=./spoon.log \
     -Dspring.profiles.active=woc \
     -Dmigration-helper.job.enabled=$job \
     -jar MigrationHelperJAR/migration-helper-1.0-SNAPSHOT.jar "$@"