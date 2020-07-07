package data;

import edu.pku.migrationhelper.data.api.ClassSignature;
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
        cs.setEnum(true);
        assertTrue(cs.isEnum());
        cs.setEnum(false);
        assertFalse(cs.isEnum());
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
        String test;
        JavaClass c = Repository.lookupClass("java.lang.String");
        ClassSignature cs = new ClassSignature(c, true);
        assertTrue(cs.isPublic() && cs.isFinal() && !cs.isInterface()
                && !cs.isNested() && !cs.isAnonymous() && !cs.isAbstract());
        assertEquals("java.lang.String", cs.getClassName());
        assertEquals("java.lang", cs.getPackageName());
        assertEquals("java.lang.Object", cs.getSuperClassName());
        System.out.println(cs.getInterfaceNames());
        assertTrue(cs.getInterfaceNames().contains("java.io.Serializable"));
        assertTrue(cs.getInterfaceNames().contains("java.lang.Comparable"));
        assertTrue(cs.getInterfaceNames().contains("java.lang.CharSequence"));
        System.out.println(cs.getMethods());
        System.out.println(cs.getFields());
    }

    @Test
    void testGenericClass() throws Exception {
        // This test is not very useful
        JavaClass c = Repository.lookupClass("java.util.ArrayList");
        ClassSignature cs = new ClassSignature(c, false);
        System.out.println(cs.getMethods());
        System.out.println(cs.getFields());
    }

}
