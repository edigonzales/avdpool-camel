package ch.so.agi.avdpool.camel;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import ch.so.agi.av.Av2ch;

public class Av2chProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        File inFile = exchange.getIn().getBody(File.class);
        Path tempDir = Files.createTempDirectory("av2ch_");
        File outFile = Paths.get(tempDir.toFile().getAbsolutePath(), "ch_" + inFile.getName()).toFile();        

        Av2ch av2ch = new Av2ch();
        av2ch.convert(inFile.getAbsolutePath(), tempDir.toFile().getAbsolutePath(), outFile.getName(), "de");                
        exchange.getIn().setBody(outFile);
    }
}
