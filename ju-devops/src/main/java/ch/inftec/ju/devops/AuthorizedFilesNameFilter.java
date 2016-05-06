package ch.inftec.ju.devops;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by rotscher on 9/11/14.
 */
public class AuthorizedFilesNameFilter implements FilenameFilter {

    private static final Logger logger = Logger.getLogger(AuthorizedFilesNameFilter.class);
    private String[] includeFilenamePatterns = new String[] {"-S-"};

    public AuthorizedFilesNameFilter() {
        logger.debug(String.format("using default filter: %s", this.includeFilenamePatterns));
    }

    public AuthorizedFilesNameFilter(String... includeFilenamePatterns) {
        this.includeFilenamePatterns = includeFilenamePatterns;
        logger.debug(String.format("custom filter from .cleanup-authorisation: %s", Arrays.asList(this.includeFilenamePatterns)));
    }


    @Override
    public boolean accept(File dir, String name) {

        for (String each : includeFilenamePatterns) {
            if (/*each != null && each.trim().length() > 0 && */ name.contains(each)) {
                return true;
            }
        }
        logger.debug(String.format("file filtered out: %s", name));
        return false;
    }
}
