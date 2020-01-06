package ch.so.agi.avdpool.camel;

import java.io.File;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ehi.ili2db.base.Ili2db;
import ch.ehi.ili2db.gui.Config;
import ch.ehi.ili2pg.PgMain;

public class Ili2pgReplaceProcessor implements Processor {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void process(Exchange exchange) throws Exception {
        File xtfFile = exchange.getIn().getBody(File.class);
        
        String dbhost = (String) exchange.getProperty("dbhost");
        String dbport = (String) exchange.getProperty("dbport");
        String dbdatabase = (String) exchange.getProperty("dbdatabase");
        String dbschema = (String) exchange.getProperty("dbschema");
        String dbusr = (String) exchange.getProperty("dbusr");
        String dbpwd = (String) exchange.getProperty("dbpwd");
        String dataset = (String) exchange.getProperty("dataset");

        Config settings = createConfig();

        settings.setFunction(Config.FC_REPLACE);
        settings.setDbhost(dbhost);
        settings.setDbport(dbport);
        settings.setDbdatabase(dbdatabase);
        settings.setDbschema(dbschema);
        settings.setDbusr(dbusr);
        settings.setDbpwd(dbpwd);
        settings.setDatasetName(dataset);

        String dburl = "jdbc:postgresql://" + settings.getDbhost() + ":" + settings.getDbport() + "/" + settings.getDbdatabase();
        settings.setDburl(dburl);

        settings.setValidation(false);
        
        if (Ili2db.isItfFilename(xtfFile.getAbsolutePath())) {
            settings.setItfTransferfile(true);
        }
        settings.setXtffile(xtfFile.getAbsolutePath());
                  
        try {
            Ili2db.readSettingsFromDb(settings);
            Ili2db.run(settings, null);
        } catch (Exception e) {
            log.error("failed to run ili2pg", e);
            log.error(e.getMessage());
            throw new Exception(e);
        }
    }

    private Config createConfig() {
        Config settings = new Config();
        new PgMain().initConfig(settings);
        return settings;
    }
}
