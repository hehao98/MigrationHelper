import edu.pku.migrationhelper.util.LZFUtils;
import edu.pku.migrationhelper.util.MathUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class UtilTest {

    final private Logger LOG = LoggerFactory.getLogger(getClass());

    @Test
    public void testLZF() throws Exception {
        String[] expectedPrefixes = {"0xzhaohx_jgit-cookbook", "AdrianBZG_gae", "BreVDD_rdpwrap", "INTFREE_UCCU_Sever"};
        for (int i = 1; i <= 4; ++i) {
            String fileName = String.format("lzf_test/%d.bin", i);
            URL url = getClass().getClassLoader().getResource(fileName);
            assertNotNull(url);

            File file = new File(url.getFile());
            int length = (int) file.length();
            byte[] content = new byte[length];
            FileInputStream is = new FileInputStream(file);
            assertNotEquals(is, null);

            int r = is.read(content);
            assertEquals(r, length);

            content = LZFUtils.lzfDecompressFromPerl(content);
            // System.out.println(new String(content));
            assertTrue(new String(content).startsWith(expectedPrefixes[i - 1]));
        }
    }

    @Test
    public void testBin2List() throws Exception {
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                testBin2List(i);
            }
        }
    }

    private void testBin2List(int size) throws Exception {
        Random random = new Random();
        List<Long> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(random.nextLong());
        }
        byte[] content = MathUtils.berNumberList(list);
        List<Long> result = MathUtils.unberNumberList(content);
        Iterator<Long> listIt = list.iterator();
        Iterator<Long> resultIt = result.iterator();
        while(listIt.hasNext() && resultIt.hasNext()) {
            assertEquals(listIt.next(), resultIt.next());
        }
        assertEquals(listIt.hasNext(), resultIt.hasNext());
    }


}
