package data;

import edu.pku.migrationhelper.data.MethodSignature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MethodSignatureTest {

    @Test
    public void testTooLongSignature() {
        MethodSignature ms = new MethodSignature();
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < 1000; ++i) {
            str.append("aaaa,");
        }
        ms.setParamList(str.toString());
        System.out.println(ms.getParamList().length());
        assertTrue(ms.getParamList().length() <= 2047);
        ms.setPackageName(str.toString());
        assertTrue(ms.getPackageName().length() <= 255);
        ms.setClassName(str.toString());
        assertTrue(ms.getClassName().length() <= 255);
        ms.setMethodName(str.toString());
        assertTrue(ms.getMethodName().length() <= 255);
    }

    @Test
    public void testNonAsciiConversion() {
        MethodSignature ms = new MethodSignature();
        ms.setClassName("中文XXX");
        assertEquals(ms.getClassName(), "??XXX");
        ms.setPackageName("中XXX");
        assertEquals(ms.getPackageName(), "?XXX");
        ms.setMethodName("文XXX");
        assertEquals(ms.getMethodName(), "?XXX");
        ms.setParamList("XXX,中文类名");
        assertEquals(ms.getParamList(), "XXX,????");
    }
}
