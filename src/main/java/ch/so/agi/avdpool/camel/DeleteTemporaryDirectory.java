package ch.so.agi.avdpool.camel;

import java.io.File;
import java.util.Date;
import java.util.Iterator;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteTemporaryDirectory implements Processor {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void process(Exchange exchange) throws Exception {
        // Av2ch and Av2Geobau use Files.createTempDirectory() which will create the directory in the systems default temporary directory.
        // This should be safe.
        String tempDir = System.getProperty("java.io.tmpdir");
        long cutoff = System.currentTimeMillis() - (1 * 60 * 60 * 1000);
        
        log.info("Files and folder in the folder " + tempDir + " older than " + new Date(cutoff) + " will be deleted.");
        
        // TODO:
        // - does it delete folder to?
        // - combine with name filter (there is such a thing).
        
        String[] files = new File(tempDir).list( new AgeFileFilter(cutoff) );

        for ( int i = 0; i < files.length; i++ ) {
            log.info("Delete file: " + files[i]);
            //System.out.println(files[i]);
        }        

    }

}
