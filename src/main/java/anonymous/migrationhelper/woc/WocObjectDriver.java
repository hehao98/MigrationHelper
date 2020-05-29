package anonymous.migrationhelper.woc;

import anonymous.migrationhelper.util.LZFUtils;
import org.apache.tomcat.util.buf.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by xxx on 2020/2/21.
 */
public class WocObjectDriver {

    Logger LOG = LoggerFactory.getLogger(getClass());

    private String baseName;

    private int partCount;

    private FileInputStream[] databaseArray;

    public WocObjectDriver(String baseName, int partCount) {
        if(partCount % 2 != 0) {
            throw new RuntimeException("partCount % 2 != 0");
        }
        this.baseName = baseName;
        this.partCount = partCount;
    }

    public void openDatabaseFile() {
        databaseArray = new FileInputStream[partCount];
        for (int i = 0; i < partCount; i++) {
            String fileName = baseName + i + ".bin";
            try {
                FileInputStream fis = new FileInputStream(fileName);
                databaseArray[i] = fis;
            } catch (Exception e) {
                LOG.error("open database fail, name = " + fileName);
                throw new RuntimeException(e);
            }
        }
    }

    public void closeDatabaseFile() {
        for (FileInputStream fis : databaseArray) {
            try {
                fis.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public synchronized byte[] getRaw(String key, long offset, int length) throws IOException {
        int slice = getSliceByKey(key);
        FileInputStream fis = databaseArray[slice];
        fis.getChannel().position(0);
        long actualSkip = fis.skip(offset);
        if(offset != actualSkip) {
            throw new IOException("skip " + actualSkip + " bytes, expect " + offset + " bytes");
        }
        byte[] result = new byte[length];
        int actualRead = fis.read(result);
        if(length != actualRead) {
            throw new IOException("read " + actualRead + " bytes, expect " + length + " bytes");
        }
        return result;
    }

    public String getLZFString(String key, long offset, int length) throws IOException {
        byte[] raw = getRaw(key, offset, length);
        if(raw == null) return null;
        try {
            return new String(LZFUtils.lzfDecompressFromPerl(raw));
        } catch (LZFUtils.LZFException e) {
            throw new IOException(e);
        }
    }

    public int getSliceByKey(String key) {
        if(key.length() != 40) {
            throw new RuntimeException("SHA1 keyType must use key of 40 length");
        }
        return HexUtils.fromHexString(key.substring(0, 2))[0]  & (partCount - 1);
    }
}
