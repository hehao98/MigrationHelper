package woc;

import org.junit.Before;
import org.junit.Test;
import tokyocabinet.HDB;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

public class TokyoCabinetTest {

    /**
     * If the TokyoCabinet binary does not exists, the test will be skipped
     * To invoke this, you should use a commandline like this
     *      mvn -DargLine="java.library.path=3rd" clean test -Dtest=TokyoCabinetTest
     * It will only work on linux because the binary in this repo is linux binary.
     */
    @Before
    public void checkDBBinary() {
        String libraryPath = System.getenv("java.library.path");
        boolean binaryExists = libraryPath != null && Paths.get(libraryPath, "libjtokyocabinet.so").toFile().exists();
        assumeTrue("java.library.path is not specified, skipping test", binaryExists);
    }

    @Test
    public void testTokyoCabinet() throws Exception {
        // create the object
        HDB hdb = new HDB();

        if (!new File("target").isDirectory()) {
            assertTrue(new File("target").mkdir());
        }

        // open the database
        if (!hdb.open("target/test_tc.tch", HDB.OWRITER | HDB.OCREAT)) {
            int ecode = hdb.ecode();
            throw new Exception("open error: " + HDB.errmsg(ecode));
        }

        // store records
        if (!hdb.put("foo", "hop") ||
                !hdb.put("bar", "step") ||
                !hdb.put("baz", "jump")) {
            int ecode = hdb.ecode();
            throw new Exception("put error: " + HDB.errmsg(ecode));
        }

        // retrieve records
        String value = hdb.get("foo");
        if (value != null) {
            System.out.println(value);
        } else {
            int ecode = hdb.ecode();
            throw new Exception("get error: " + HDB.errmsg(ecode));
        }

        // traverse records
        hdb.iterinit();
        String key;
        while ((key = hdb.iternext2()) != null) {
            value = hdb.get(key);
            if (value != null) {
                System.out.println(key + ":" + value);
            }
        }

        // close the database
        if (!hdb.close()) {
            int ecode = hdb.ecode();
            throw new Exception("close error: " + HDB.errmsg(ecode));
        }
    }
}
