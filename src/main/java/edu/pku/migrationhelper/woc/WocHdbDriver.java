package edu.pku.migrationhelper.woc;

import com.ning.compress.lzf.LZFDecoder;
import com.ning.compress.lzf.LZFException;
import com.twitter.hashing.KeyHasher;
import edu.pku.migrationhelper.util.LZFUtils;
import edu.pku.migrationhelper.util.MathUtils;
import org.apache.tomcat.util.buf.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tokyocabinet.HDB;

import java.util.*;

/**
 * Created by xuyul on 2020/2/21.
 */
public class WocHdbDriver {

    public enum ContentType {
        Text,
        LZFText,
        SHA1,
        SHA1List,
        BerNumberList,
    }

    public static final Set<ContentType> SupportedKeyType = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            ContentType.Text, ContentType.SHA1
    )));

    public static final Set<ContentType> SupportedValueType = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            ContentType.Text, ContentType.LZFText, ContentType.SHA1, ContentType.SHA1List, ContentType.BerNumberList
    )));

    Logger LOG = LoggerFactory.getLogger(getClass());

    private String baseName;

    private int partCount;

    private ContentType keyType;

    private ContentType valueType;

    private HDB[] databaseArray;

    public WocHdbDriver(String baseName, int partCount, ContentType keyType, ContentType valueType) {
        if(partCount % 2 != 0) {
            throw new RuntimeException("partCount % 2 != 0");
        }
        if (!SupportedKeyType.contains(keyType)) {
            throw new RuntimeException("keyType not supported: " + keyType);
        }
        if (!SupportedValueType.contains(valueType)) {
            throw new RuntimeException("valueType not supported: " + valueType);
        }
        this.baseName = baseName;
        this.partCount = partCount;
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public void openDatabaseFile() {
        databaseArray = new HDB[partCount];
        for (int i = 0; i < partCount; i++) {
            HDB hdb = new HDB();
            String fileName = baseName + i + ".tch";
            if(!hdb.open(fileName, HDB.OREADER)) {
                LOG.error("open tokyo cabinet database fail, name = {}, errCode = {}, errMsg = {}",
                        fileName, hdb.ecode(), hdb.errmsg());
                throw new RuntimeException("open tokyo cabinet database fail");
            }
            databaseArray[i] = hdb;
        }
    }

    public void closeDatabaseFile() {
        for (HDB hdb : databaseArray) {
            hdb.close();
        }
    }

    public byte[] getRaw(String key) {
        int slice = getSliceByKey(key);
        HDB hdb = databaseArray[slice];
        return hdb.get(getKeyBytes(key));
    }

    public String getValue(String key) {
        byte[] value = getRaw(key);
        if(value == null) return null;
        switch (valueType) {
            case Text:
                return new String(value);
            case LZFText:
                try {
                    return new String(LZFUtils.decompressFromPerl(value));
                } catch (LZFException e) {
                    throw new RuntimeException(e);
                }
            case SHA1:
            case SHA1List:
                return HexUtils.toHexString(value);
            default:
                throw new RuntimeException("valueType not supported: " + valueType);
        }
    }

    public List<String> getSHA1ListValue(String key) {
        if (valueType != ContentType.SHA1List) {
            throw new RuntimeException("valueType not supported: " + valueType);
        }
        byte[] value = getRaw(key);
        if(value == null) return null;
        if (value.length % 20 != 0) {
            throw new RuntimeException("SHA1 list length % 20 != 0");
        }
        int count = value.length / 20;
        String hex = HexUtils.toHexString(value);
        List<String> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(hex.substring(i * 40, i * 40 + 40));
        }
        return result;
    }

    public List<Long> getBerNumberListValue(String key) {
        if (valueType != ContentType.BerNumberList) {
            throw new RuntimeException("valueType not supported: " + valueType);
        }
        byte[] value = getRaw(key);
        if(value == null) return null;
        return MathUtils.unber(value);
    }

    public int getSliceByKey(String key) {
        switch (keyType) {
            case Text:
                return (int)(KeyHasher.FNV1A_32().hashKey(key.getBytes()) & (partCount - 1));
            case SHA1:
                if(key.length() != 40) {
                    throw new RuntimeException("SHA1 keyType must use key of 40 length");
                }
                return HexUtils.fromHexString(key.substring(0, 2))[0]  & (partCount - 1);
        }
        throw new RuntimeException("keyType not supported: " + keyType);
    }

    public byte[] getKeyBytes(String key) {
        switch (keyType) {
            case Text:
                return key.getBytes();
            case SHA1:
                if(key.length() != 40) {
                    throw new RuntimeException("SHA1 keyType must use key of 40 length");
                }
                return HexUtils.fromHexString(key);
        }
        throw new RuntimeException("keyType not supported: " + keyType);
    }
}
