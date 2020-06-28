package edu.pku.migrationhelper.service;

import com.twitter.hashing.KeyHasher;
import edu.pku.migrationhelper.data.MethodSignature;
import edu.pku.migrationhelper.mapper.MethodSignatureMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MapperUtilService {

    @Autowired
    private MethodSignatureMapper methodSignatureMapper;

    public static int getMethodSignatureSliceKey(String packageName, String className) {
        String key = packageName + ":" + className;
        return (int)(KeyHasher.FNV1A_32().hashKey(key.getBytes()) & (MethodSignatureMapper.MAX_TABLE_COUNT - 1));
    }

    public static int getMethodSignatureSliceKey(long signatureId) {
        return (int)(signatureId >> MethodSignatureMapper.MAX_ID_BIT) & (MethodSignatureMapper.MAX_TABLE_COUNT - 1);
    }

    public static String getMethodSignatureCacheKey(MethodSignature ms) {
        return ms.getPackageName() + ":" + ms.getClassName() + ":" + ms.getMethodName() + ":" + ms.getParamList();
    }

    public List<MethodSignature> getMethodSignaturesByIds(List<Long> signatureIds) {
        int tableCount = methodSignatureMapper.MAX_TABLE_COUNT;
        List<List<Long>> idsByTable = new ArrayList<>();
        for (int i = 0; i < tableCount; ++i) {
            idsByTable.add(new ArrayList<>());
        }
        for (long id : signatureIds) {
            int slice = getMethodSignatureSliceKey(id);
            idsByTable.get(slice).add(id);
        }
        List<MethodSignature> result = new ArrayList<>();
        for (int i = 0; i < tableCount; ++i) {
            if (idsByTable.get(i).size() == 0) continue;
            result.addAll(methodSignatureMapper.findByIds(i, idsByTable.get(i)));
        }
        assert result.size() == signatureIds.size();
        return result;
    }
}
