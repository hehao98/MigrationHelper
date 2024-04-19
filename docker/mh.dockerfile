FROM ubuntu:20.04 AS build

# Install curl and maven
RUN apt-get update && apt-get install -y curl maven

# Setup Oracle JDK 8
RUN cd /tmp/ && curl https://cfdownload.adobe.com/pub/adobe/coldfusion/java/java8/java8u361/jdk/jdk-8u361-linux-x64.tar.gz --output jdk-8u361-linux-x64.tar.gz
RUN mkdir -p /usr/lib/jvm && tar -xvf /tmp/jdk-8u361-linux-x64.tar.gz -C /usr/lib/jvm/
RUN update-alternatives --install /usr/bin/java java /usr/lib/jvm/jdk1.8.0_361/bin/java 1
RUN update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/jdk1.8.0_361/bin/javac 1
RUN update-alternatives --set java /usr/lib/jvm/jdk1.8.0_361/bin/java
RUN update-alternatives --set javac /usr/lib/jvm/jdk1.8.0_361/bin/javac
RUN rm /tmp/jdk-8u361-linux-x64.tar.gz && rm -rf /usr/lib/jvm/jdk1.8.0_361/*src.zip

# Copy the Maven project files into the image
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY 3rd ./3rd

# Build the project
RUN mvn clean package -DskipTests

# Stage 2: Run the JAR file
FROM ubuntu:20.04 AS run

# Install curl
RUN apt-get update && apt-get install -y curl
# clean up
RUN apt-get clean && rm -rf /var/lib/apt/lists/*

# Setup Oracle JRE 8
RUN cd /tmp/ && curl https://cfdownload.adobe.com/pub/adobe/coldfusion/java/java8/java8u361/jre/jre-8u361-linux-x64.tar.gz --output jre-8u361-linux-x64.tar.gz
RUN mkdir -p /usr/lib/jvm && tar -xvf /tmp/jre-8u361-linux-x64.tar.gz -C /usr/lib/jvm/
RUN update-alternatives --install /usr/bin/java java /usr/lib/jvm/jre1.8.0_361/bin/java 1
RUN update-alternatives --set java /usr/lib/jvm/jre1.8.0_361/bin/java
RUN rm /tmp/jre-8u361-linux-x64.tar.gz && rm -rf /usr/lib/jvm/jre1.8.0_361/*src.zip

# Copy the JAR file from the build stage
WORKDIR /app
COPY --from=build /app/target/migration-helper-1.0-SNAPSHOT.jar /app/migration-helper.jar

# Copy properties files
COPY ./src/main/resources /app/src/main/resources
# Replace localhost with mongo
RUN sed -i 's/localhost/mongo/g' /app/src/main/resources/application-web.yaml

# Run the JAR file
CMD ["java", "-Xms4g", "-Xmx5g", "-Dlog4j.configuration=mylog4j.properties", "-Dspoon.log.path=./spoon.log", "-Dspring.profiles.active=web", "-jar", "migration-helper.jar", "--web"]