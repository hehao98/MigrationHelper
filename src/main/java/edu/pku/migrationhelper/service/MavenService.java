package edu.pku.migrationhelper.service;

import edu.pku.migrationhelper.data.lib.LibraryInfo;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
public class MavenService {

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    /**
     * Do a shallow dependency extraction (i.e. no transitive dependency resolution)
     *   Local variable resolution is implemented
     * @param pom String content of pom.xml
     * @return list of dependencies
     * @throws IOException if error happens when reading pom.cml
     * @throws XmlPullParserException if pom.xml is malformed
     */
    public List<LibraryInfo> analyzePom(String pom) throws IOException, XmlPullParserException {
        return analyzePom(new ByteArrayInputStream(pom.getBytes()));
    }

    /**
     * @see MavenService#analyzePom(String) 
     */
    public List<LibraryInfo> analyzePom(InputStream stream) throws IOException, XmlPullParserException {
        Model model = new MavenXpp3Reader().read(stream);
        List<Dependency> dependencies = model.getDependencies();
        Properties properties = model.getProperties();
        return dependencies.stream().map(d -> {
                LibraryInfo l = new LibraryInfo();
                l.groupId = resolveProperties(d.getGroupId(), properties);
                l.artifactId = resolveProperties(d.getArtifactId(), properties);
                l.version = resolveProperties(d.getVersion(), properties);
                return l;
        }).collect(Collectors.toList());
    }

    public String resolveProperties(String original, Properties properties) {
        for (String key : properties.stringPropertyNames()) {
            original = original.replaceAll("\\$\\{" + key + "}", properties.getProperty(key));
        }
        return original;
    }
}
