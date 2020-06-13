if [ "$1" != "DataExportJob" ] && [ "$1" != "LibraryRecommendJob" ]; then
  echo "Usage: run-woc.sh <Job Name> <Arg1> <Arg2> <Arg3> <Arg4>"
  echo "Currently Supported Jobs: DataExportJob, LibraryRecommendJob"
  exit
fi
echo "Pulling Latest Version..."
cd MigrationHelperJAR || exit
git pull origin master
cd ..
echo "MigrationHelper: Running Job $1..."
/home/heh/jdk1.8.0_144/bin/java -Djava.library.path=/home/heh/lib \
     -Dlog4j.configuration=mylog4j.properties \
     -Dspoon.log.path=./spoon.log \
     -Dspring.profiles.active=woc \
     -Dmigration-helper.job.enabled=$1 \
     -jar MigrationHelperJAR/migration-helper-1.0-SNAPSHOT.jar $2 $3 $4 $5