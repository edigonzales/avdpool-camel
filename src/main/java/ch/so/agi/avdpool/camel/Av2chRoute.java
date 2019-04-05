package ch.so.agi.avdpool.camel;

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ch.so.agi.camel.processors.Av2chProcessor;

@Component
public class Av2chRoute extends RouteBuilder {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${app.pathToUnzipFolder}")
    private String pathToUnzipFolder;

    @Value("${app.pathToAv2ChFolder}")
    private String pathToAv2ChFolder;

    @Override
    public void configure() throws Exception {
        from("file://"+pathToUnzipFolder+"/?noop=true&charset=ISO-8859-1&include=.*\\.itf&delay=30000&initialDelay=5000&readLock=changed&idempotentRepository=#fileConsumerRepo&idempotentKey=av2ch-${file:name}-${file:size}-${file:modified}")
        .process(new Av2chProcessor())
        .to("file://"+pathToAv2ChFolder+"/");

    }

}
