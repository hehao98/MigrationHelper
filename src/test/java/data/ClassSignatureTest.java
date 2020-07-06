package data;

import edu.pku.migrationhelper.data.ClassSignature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    }
}
