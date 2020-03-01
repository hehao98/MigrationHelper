package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.service.LibraryIdentityService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by xuyul on 2020/2/15.
 */
@Component
@ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "MavenArtifactExtractJob")
public class MavenArtifactExtractJob {

    Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    @Qualifier("ThreadPool")
    private ExecutorService executorService;

    @Autowired
    private LibraryIdentityService libraryIdentityService;

    @EventListener(ApplicationReadyEvent.class)
    public void run() throws Exception {
        String rootUrl = "https://repo1.maven.org/maven2/";
        executorService.submit(new SinglePageAnalysis(rootUrl, 0));
    }

    private class SinglePageAnalysis implements Runnable {

        private String url;

        private int retryCount;

        public SinglePageAnalysis(String url, int retryCount) {
            this.url = url;
            this.retryCount = retryCount;
        }

        @Override
        public void run() {
            try {
                LOG.info("start parse url: {}, retryCount = {}", url, retryCount);
                if(retryCount > 5) {
                    LOG.warn("retryCount > 5, stop retry url: {}", url);
                    return;
                }
                Document doc = Jsoup.connect(url).timeout(180 * 1000).get();
                Element element = doc.selectFirst("pre");
                Elements nodes = element.select("a[href]");
                boolean containsMeta = false;
                String metaUrl = null;
                List<SinglePageAnalysis> nextList = new ArrayList<>(nodes.size());
                for (Element node : nodes) {
                    String text = node.text();
                    if("maven-metadata.xml".equals(text)) {
                        containsMeta = true;
                        metaUrl = node.absUrl("href");
                        continue;
                    }
                    if(!text.endsWith("/")) {
                        continue;
                    }
                    if("../".equals(text)) {
                        continue;
                    }
                    nextList.add(new SinglePageAnalysis(node.absUrl("href"), 0));
                }
                if(containsMeta) {
                    LOG.info("extract group artifact url: {}", metaUrl);
                    containsMeta = libraryIdentityService.extractGroupArtifactFromMavenMeta(metaUrl);
                    if(!containsMeta) {
                        LOG.info("url is not meta info: {}", metaUrl);
                    }
                }
                if(!containsMeta){
                    for (SinglePageAnalysis next : nextList) {
                        executorService.submit(next);
                    }
                }
                if(retryCount > 0) {
                    LOG.warn("retry success, url: {}", url);
                }
            } catch (Exception e) {
                LOG.error("parse maven html fail url: " + url, e);
                executorService.submit(new SinglePageAnalysis(url, retryCount + 1));
            }
        }
    }
}
