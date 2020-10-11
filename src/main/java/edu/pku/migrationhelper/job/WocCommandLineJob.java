package edu.pku.migrationhelper.job;

import edu.pku.migrationhelper.service.WocRepositoryAnalysisService;
import edu.pku.migrationhelper.woc.WocHdbDriver;
import edu.pku.migrationhelper.woc.WocObjectDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Deprecated
// @Component
// @ConditionalOnProperty(name = "migration-helper.job.enabled", havingValue = "WocCommandLineJob")
public class WocCommandLineJob implements CommandLineRunner {

    Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    private WocRepositoryAnalysisService wocRepositoryAnalysisService;

    @Override
    public void run(String... args) throws Exception {

        wocRepositoryAnalysisService.closeAllWocDatabase();

        if(args.length < 7) {
            LOG.error("Usage: <Hdb|Object> <DatabaseFileBase> <PartCount> <KeyType> <ValueType> <Raw|Pretty> <Key> (<Offset> <Length>) (<OutputFile>)");
            return;
        }
        String databaseType = args[0];
        String fileBase = args[1];
        int partCount = Integer.parseInt(args[2]);
        WocHdbDriver.ContentType keyType = WocHdbDriver.ContentType.valueOf(args[3]);
        WocHdbDriver.ContentType valueType = WocHdbDriver.ContentType.valueOf(args[4]);
        boolean raw = "Raw".equals(args[5]);
        String key = args[6];

        if("Hdb".equals(databaseType)) {
            WocHdbDriver hdbDriver = new WocHdbDriver(fileBase, partCount, keyType, valueType);
            hdbDriver.openDatabaseFile();
            try {
                String outputFile = null;
                if(args.length > 7) {
                    outputFile = args[7];
                }
                byte[] content = null;
                if (raw) {
                    content = hdbDriver.getRaw(key);
                } else {
                    if(valueType != WocHdbDriver.ContentType.BerNumberList) {
                        String contentValue = hdbDriver.getValue(key);
                        content = contentValue == null ? null : contentValue.getBytes();
                    } else {
                        List<Long> contentValue = hdbDriver.getBerNumberListValue(key);
                        if(contentValue != null) {
                            StringBuilder sb = new StringBuilder();
                            for (Long number : contentValue) {
                                sb.append(number);
                                sb.append(",");
                            }
                            if (sb.length() > 0) {
                                sb.deleteCharAt(sb.length() - 1);
                            }
                            content = sb.toString().getBytes();
                        }
                    }
                }
                outputResult(outputFile, content);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                hdbDriver.closeDatabaseFile();
            }
        } else if ("Object".equals(databaseType)) {
            WocObjectDriver objectDriver = new WocObjectDriver(fileBase, partCount);
            objectDriver.openDatabaseFile();
            try {
                if(args.length < 9) {
                    LOG.error("Object Database Must Set Offset And Length");
                    return;
                }
                long offset = Long.parseLong(args[7]);
                int length = Integer.parseInt(args[8]);
                String outputFile = null;
                if(args.length > 9) {
                    outputFile = args[9];
                }
                byte[] content = null;
                if (raw) {
                    content = objectDriver.getRaw(key, offset, length);
                } else {
                    String contentValue = objectDriver.getLZFString(key, offset, length);
                    if(contentValue != null) {
                        content = contentValue.getBytes();
                    }
                }
                outputResult(outputFile, content);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                objectDriver.closeDatabaseFile();
            }
        } else {
            LOG.error("Unknown Database Type: {}", databaseType);
            return;
        }
    }

    private void outputResult(String outputFile, byte[] content) throws IOException {
        if(content == null) {
            LOG.error("content not found");
            return;
        }
        if(outputFile == null) {
            System.out.write(content);
            System.out.println();
        } else {
            FileOutputStream os = new FileOutputStream(outputFile);
            os.write(content);
            os.flush();
            os.close();
        }
    }
}
