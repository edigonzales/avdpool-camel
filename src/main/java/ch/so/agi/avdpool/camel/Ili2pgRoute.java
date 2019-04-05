package ch.so.agi.avdpool.camel;

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Ili2pgRoute extends RouteBuilder {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${app.pathToUnzipFolder}")
    private String pathToUnzipFolder;

    @Value("${app.dbHostEdit}")
    private String dbHostEdit;
    
    @Value("${app.dbDatabaseEdit}")
    private String dbDatabaseEdit;
    
    @Value("${app.dbSchemaEdit}")
    private String dbSchemaEdit;

    @Value("${app.dbUserEdit}")
    private String dbUserEdit;

    @Value("${app.dbPwdEdit}")
    private String dbPwdEdit;

    @Override
    public void configure() throws Exception {
      from("file://"+pathToUnzipFolder+"/?noop=true&charset=ISO-8859-1&include=.*\\.itf&delay=30000&initialDelay=5000&readLock=changed&idempotentRepository=#fileConsumerRepo&idempotentKey=ili2pg-${file:name}-${file:size}-${file:modified}")
      .toD("ili2pg:import?dbhost="+dbHostEdit+"&dbport=5432&dbdatabase="+dbDatabaseEdit+"&dbschema="+dbSchemaEdit+"&dbusr="+dbUserEdit+"&dbpwd="+dbPwdEdit+"&dataset=${file:onlyname.noext}");
    }
}
