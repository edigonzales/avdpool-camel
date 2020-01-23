package ch.so.agi.avdpool.camel;

import java.io.File;
import java.io.FileFilter;
import java.util.Date;
import java.util.Iterator;
import java.util.function.Predicate;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CleanTemporaryDirectory implements Processor {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void process(Exchange exchange) throws Exception {
        // Av2ch and Av2Geobau use Files.createTempDirectory() which will create the
        // directory in the systems default temporary directory.
        // This should be safe.
        String tempDir = System.getProperty("java.io.tmpdir");
        long cutoff = System.currentTimeMillis() - (1 * 30 * 60 * 1000);

        log.info("Files and folder in the folder " + tempDir + " older than " + new Date(cutoff) + " will be deleted.");

        File dir = new File(tempDir);
        
        FileFilter wildcardFileFilter = new WildcardFileFilter("av2*_*");
        FileFilter ageFileFilter = new AgeFileFilter(cutoff);
        FileFilter combinedFilter = f -> wildcardFileFilter.accept(f) && ageFileFilter.accept(f);

        File[] files = dir.listFiles(combinedFilter);
        
        for (File file : files) {
            log.info("Deleting: " + file.getAbsolutePath());
            FileUtils.forceDelete(file);   
        }
    }
}
