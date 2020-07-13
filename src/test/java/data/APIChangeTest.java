package data;

import edu.pku.migrationhelper.data.api.*;
import edu.pku.migrationhelper.service.JarAnalysisService;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class APIChangeTest {

    List<ClassSignature> getFromJar(String path) throws IOException {
        JarAnalysisService jas = new JarAnalysisService();
        String jarFilePath = Objects.requireNonNull(
                getClass().getClassLoader().getResource(path)).getPath();
        return jas.analyzeJar(jarFilePath, true, null);
    }

    void printAPIChanges(APIChange change) {
        for (ClassChange chg : change.getChangedClasses()) {
            System.out.println(chg.getOldClass() + " " + chg.getNewClass());
            for (MethodChange mc : chg.getChangedMethods()) {
                System.out.println("-- " + mc.getOldMethod() + " -> " + mc.getNewMethod());
            }
            for (FieldChange fc : chg.getChangedFields()) {
                System.out.println("-- " + fc.getOldField() + " -> " + fc.getNewField());
            }
        }
    }

    @Test
    public void testAPIChanges() throws Exception {
        System.out.println("========== testAPIChanges ==========");
        List<ClassSignature> from = getFromJar("jars/gson-2.8.5.jar");
        List<ClassSignature> to = getFromJar("jars/gson-2.8.6.jar");
        APIChange change = new APIChange("gson", "gson", "2.8.5", "2.8.6", from, to);
        printAPIChanges(change);
    }

    @Test
    public void testAPIChangesMany() throws Exception {
        System.out.println("========== testAPIChangesMany ==========");
        List<ClassSignature> from = getFromJar("jars/gson-2.8.5.jar");
        List<ClassSignature> to = getFromJar("jars/gson-2.7.jar");
        APIChange change = new APIChange("gson", "gson", "2.7", "2.8.6", from, to);
        printAPIChanges(change);
    }
}
