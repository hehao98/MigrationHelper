package anonymous.migrationhelper.service;

import anonymous.migrationhelper.mapper.LibraryVersionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by xuyul on 2020/1/2.
 */
@Service
public class TestService {

    @Autowired
    private LibraryVersionMapper libraryVersionMapper;

}
