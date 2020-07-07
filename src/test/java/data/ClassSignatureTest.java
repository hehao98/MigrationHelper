package data;

import edu.pku.migrationhelper.data.ClassSignature;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ClassSignatureTest {

    @Test
    void testSetModifier() {
        ClassSignature cs = new ClassSignature();
        cs.setPackage(true);
        assertTrue(cs.isPackage() && !cs.isPublic());
        cs.setPublic(true);
        assertTrue(cs.isPublic() && !cs.isPackage());
        cs.setFinal(true);
        assertTrue(cs.isFinal());
        cs.setFinal(false);
        assertFalse(cs.isFinal());
        cs.setAbstract(true);
        assertTrue(cs.isAbstract());
        cs.setAbstract(false);
        assertFalse(cs.isAbstract());
        cs.setInterface(true);
        assertTrue(cs.isInterface());
        cs.setInterface(false);
        assertFalse(cs.isInterface());
        cs.setNested(true);
        assertTrue(cs.isNested());
        cs.setNested(false);
        assertFalse(cs.isNested());
        cs.setAnonymous(true);
        assertTrue(cs.isAnonymous());
        cs.setAnonymous(false);
        assertFalse(cs.isAnonymous());
        System.out.println(cs.getId());
    }

    @Test
    void testLoadJavaClass() throws Exception {
        JavaClass c = Repository.lookupClass("java.lang.String");
        ClassSignature cs = new ClassSignature(c);
        assertTrue(cs.isPublic() && cs.isFinal() && !cs.isInterface()
                && !cs.isNested() && !cs.isAnonymous() && !cs.isAbstract());
        assertEquals("java.lang.String", cs.getClassName());
        assertEquals("java.lang", cs.getPackageName());
        assertEquals("java.lang.Object", cs.getSuperClassName());
        System.out.println(Arrays.toString(cs.getInterfaceNames()));
        assertTrue(Arrays.asList(cs.getInterfaceNames()).contains("java.io.Serializable"));
        assertTrue(Arrays.asList(cs.getInterfaceNames()).contains("java.lang.Comparable"));
        assertTrue(Arrays.asList(cs.getInterfaceNames()).contains("java.lang.CharSequence"));
    }
}
