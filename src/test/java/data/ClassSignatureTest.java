package data;

import edu.pku.migrationhelper.data.api.Annotation;
import edu.pku.migrationhelper.data.api.ClassSignature;
import edu.pku.migrationhelper.data.api.MethodSignature;
import javafx.util.Pair;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ClassSignatureTest {

    @Test
    void testSetModifier() {
        ClassSignature cs = new ClassSignature();
        cs = cs.setPackage(true);
        assertTrue(cs.isPackage() && !cs.isPublic());
        cs = cs.setPublic(true);
        assertTrue(cs.isPublic() && !cs.isPackage());
        cs = cs.setFinal(true);
        assertTrue(cs.isFinal());
        cs = cs.setFinal(false);
        assertFalse(cs.isFinal());
        cs = cs.setAbstract(true);
        assertTrue(cs.isAbstract());
        cs = cs.setAbstract(false);
        assertFalse(cs.isAbstract());
        cs = cs.setInterface(true);
        assertTrue(cs.isInterface());
        cs = cs.setInterface(false);
        assertFalse(cs.isInterface());
        cs = cs.setEnum(true);
        assertTrue(cs.isEnum());
        cs = cs.setEnum(false);
        assertFalse(cs.isEnum());
        cs = cs.setNested(true);
        assertTrue(cs.isNested());
        cs = cs.setNested(false);
        assertFalse(cs.isNested());
        cs = cs.setAnonymous(true);
        assertTrue(cs.isAnonymous());
        cs = cs.setAnonymous(false);
        assertFalse(cs.isAnonymous());
        System.out.println(cs.getId());
    }

    @Test
    void testInterfaceManipulation() {
        ClassSignature cs = new ClassSignature();
        cs.setInterfaceId("abc", "def");
        assertEquals("def", cs.getInterfaceId("abc"));
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
    void testClassSignatureEqual() {
        Set<ClassSignature> set = new HashSet<>();
        assertEquals(new ClassSignature(), new ClassSignature());
        assertEquals(new ClassSignature().setClassName("java.lang.Object"),
                new ClassSignature().setClassName("java.lang.Object"));
        set.add(new ClassSignature());
        System.out.println(set);
        set.add(new ClassSignature());
        System.out.println(set);
        assertEquals(1, set.size());
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
