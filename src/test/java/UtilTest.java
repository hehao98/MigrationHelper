import edu.pku.migrationhelper.util.LZFUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class UtilTest {

    final private Logger LOG = LoggerFactory.getLogger(getClass());

    @Test
    public void testLZF() throws Exception {
        String[] expectedPerfix = {"0xzhaohx_jgit-cookbook", "AdrianBZG_gae", "BreVDD_rdpwrap", "INTFREE_UCCU_Sever"};
        for (int i = 1; i <= 4; ++i) {
            String fileName = String.format("lzf_test/%d.bin", i);
            URL url = getClass().getClassLoader().getResource(fileName);

            assert url != null;

            File file = new File(url.getFile());
            int length = (int) file.length();
            byte[] content = new byte[length];
            FileInputStream is = new FileInputStream(file);

            assertNotEquals(is, null);

            is.read(content);
            content = LZFUtils.lzfDecompressFromPerl(content);
            // System.out.println(new String(content));
            assertTrue(new String(content).startsWith(expectedPerfix[i - 1]));
        }
    }
}
