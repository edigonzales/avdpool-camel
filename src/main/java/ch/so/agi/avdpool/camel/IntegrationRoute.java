package ch.so.agi.avdpool.camel;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.s3.S3Constants;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ch.so.agi.avdpool.camel.Av2chProcessor;
import ch.so.agi.avdpool.camel.Av2GeobauProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class IntegrationRoute extends RouteBuilder {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${app.ftpUserInfogrips}")
    private String ftpUserInfogrips;

    @Value("${app.ftpPwdInfogrips}")
    private String ftpPwdInfogrips;

    @Value("${app.ftpUrlInfogrips}")
    private String ftpUrlInfogrips;

    @Value("${app.pathToDownloadFolder}")
    private String pathToDownloadFolder;

    @Value("${app.pathToUnzipFolder}")
    private String pathToUnzipFolder;

    @Value("${app.pathToErrorFolder}")
    private String pathToErrorFolder;
    
    @Value("${app.pathToAv2ChFolder}")
    private String pathToAv2ChFolder;
    
    @Value("${app.pathToAv2GeobauFolder}")
    private String pathToAv2GeobauFolder;

    @Value("${app.awsAccessKey}")
    private String awsAccessKey;

    @Value("${app.awsSecretKey}")
    private String awsSecretKey;
    
    @Value("${app.awsBucketNameSO}")
    private String awsBucketNameSO;
    
    @Value("${app.awsBucketNameCH}")
    private String awsBucketNameCH;
    
    @Value("${app.awsBucketNameDXF}")
    private String awsBucketNameDXF;

    @Value("${app.downloadDelay}")
    private String downloadDelay;

    @Value("${app.uploadDelay}")
    private String uploadDelay;
    
    @Value("${app.convertDelay}")
    private String convertDelay;

    @Value("${app.initialDownloadDelay}")
    private String initialDownloadDelay;

    @Value("${app.initialUploadDelay}")
    private String initialUploadDelay;
  
    @Value("${app.initialConvertDelay}")
    private String initialConvertDelay;
    
    @Value("${app.importCronScheduleExpression}")
    private String importCronScheduleExpression;

    @Value("${app.dbHostEdit}")
    private String dbHostEdit;
    
    @Value("${app.dbPortEdit}")
    private String dbPortEdit;
    
    @Value("${app.dbDatabaseEdit}")
    private String dbDatabaseEdit;
    
    @Value("${app.dbSchemaEdit}")
    private String dbSchemaEdit;

    @Value("${app.dbUserEdit}")
    private String dbUserEdit;

    @Value("${app.dbPwdEdit}")
    private String dbPwdEdit;
    
    @Value("${app.emailSmtpSender}")
    private String emailSmtpSender;

    @Value("${app.emailUserSender}")
    private String emailUserSender;

    @Value("${app.emailPwdSender}")
    private String emailPwdSender;

    @Value("${app.emailUserRecipient}")
    private String emailUserRecipient;

    @Value("${app.smtpAuth}")
    private String smtpAuth = "false";

    @Override
    public void configure() throws Exception {
        /*
         * Send an email if an exception occures.
         */
        Predicate noSmtpAuthPredicate;
        if (smtpAuth.equalsIgnoreCase("false")) {
            noSmtpAuthPredicate = PredicateBuilder.constant(true);
        } else {
            noSmtpAuthPredicate = PredicateBuilder.constant(false);
        }

        onException(Exception.class)
        .handled(true)
        .setHeader("from", simple(emailUserSender))
        .setHeader("subject", simple("AV-Import/-Export: Fehler"))
        .setHeader("to", simple(emailUserRecipient))
        .setBody(simple("Route Id: ${routeId} \n Date: ${date:now:yyyy-MM-dd HH:mm:ss} \n File: ${in.header.CamelFileAbsolutePath} \n Message: ${exception.message} \n Stacktrace: ${exception.stacktrace}"))
        .choice()
            .when(noSmtpAuthPredicate).to(emailSmtpSender+"?mail.smtp.auth="+smtpAuth)
        .otherwise()
            .to(emailSmtpSender+"?username="+emailUserSender+"&password="+emailPwdSender)
        .end()
        .log(LoggingLevel.ERROR, simple("${exception.stacktrace}").getText());
         
        /*
         * Download ITF (ZIP) files from Infogrips FTP server every n seconds or minutes.
         */
        from("ftp://"+ftpUserInfogrips+"@"+ftpUrlInfogrips+"/\\dm01avso24lv95\\itf\\?password="+ftpPwdInfogrips+"&antInclude=*.zip&autoCreate=false&noop=true&readLock=changed&stepwise=false&separator=Windows&passiveMode=true&binary=true&delay="+downloadDelay+"&initialDelay="+initialDownloadDelay+"&idempotentRepository=#fileConsumerRepo&idempotentKey=ftp-${file:name}-${file:size}-${file:modified}")
        //from("ftp://"+ftpUserInfogrips+"@"+ftpUrlInfogrips+"/\\dm01avso24lv95\\itf\\?password="+ftpPwdInfogrips+"&antInclude=240100.zip&autoCreate=false&noop=true&readLock=changed&stepwise=false&separator=Windows&passiveMode=true&binary=true&delay="+downloadDelay+"&initialDelay="+initialDownloadDelay+"&idempotentRepository=#fileConsumerRepo&idempotentKey=ftp-${file:name}-${file:size}-${file:modified}")
        .routeId("_download_")
        .log(LoggingLevel.INFO, "Downloading and unzipping route: ${in.header.CamelFileNameOnly}")
        .to("file://"+pathToDownloadFolder)
        .split(new ZipSplitter())
        .streaming().convertBodyTo(String.class, "ISO-8859-1") 
            .choice()
                .when(body().isNotNull())
                    .setHeader(Exchange.FILE_NAME, simple("${file:name.noext}.itf"))
                    .to("file://"+pathToUnzipFolder+"?charset=ISO-8859-1")
            .end()
        .end();
        
        /*
         * Upload the (original) zipped ITF files to S3 every n seconds or minutes.
         */
        from("file://"+pathToDownloadFolder+"/?noop=true&include=.*\\.zip&delay="+uploadDelay+"&initialDelay="+initialUploadDelay+"&readLock=changed&idempotentRepository=#fileConsumerRepo&idempotentKey=s3-${file:name}-${file:size}-${file:modified}")
        .routeId("_upload_")
        .log(LoggingLevel.INFO, "Uploading DM01-SO: ${in.header.CamelFileNameOnly}") 
        .convertBodyTo(byte[].class)
        .setHeader(S3Constants.CONTENT_LENGTH, simple("${in.header.CamelFileLength}"))
        .setHeader(S3Constants.KEY,simple("${in.header.CamelFileNameOnly}"))
        .setHeader(S3Constants.CANNED_ACL,simple("PublicRead")) // https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/model/CannedAccessControlList.html
        .to("aws-s3://" + awsBucketNameSO
                + "?deleteAfterWrite=false&region=EU_CENTRAL_1" //https://docs.aws.amazon.com/de_de/general/latest/gr/rande.html https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/regions/Regions.html
                + "&accessKey={{awsAccessKey}}"
                + "&secretKey=RAW({{awsSecretKey}})")
        .log(LoggingLevel.INFO, "Upload DM01-SO done: ${in.header.CamelFileNameOnly}");
        
        /*
         * Convert ITF files to zipped "Bundesmodell" (DM01AVCH24DLV95) every n seconds or minutes.
         * Be careful: The library writes the error log messages to /dev/null since it was really verbose.
         * It should restore the default behaviour but there can be exotic corner cases... 
         */
        from("file://"+pathToUnzipFolder+"/?noop=true&charset=ISO-8859-1&include=.*\\.itf&delay="+convertDelay+"&initialDelay="+initialConvertDelay+"&readLock=changed&readLockMinAge=60s&idempotentRepository=#fileConsumerRepo&idempotentKey=av2ch-${file:name}-${file:size}-${file:modified}")
        .routeId("_av2ch_")
        .log(LoggingLevel.INFO, "Converting file to DM01-CH: ${in.header.CamelFileNameOnly}")        
        .process(new Av2chProcessor())
        .to("file://"+pathToAv2ChFolder+"/")
        .marshal().zipFile()
        .to("file://"+pathToAv2ChFolder+"/")
        .log(LoggingLevel.INFO, "Convertion to DM01-CH done: ${in.header.CamelFileNameOnly}");
        
        /*
         * Upload "Bundesmodell" to S3 every n seconds or minutes.
         */
        from("file://"+pathToAv2ChFolder+"/?noop=true&include=.*\\.itf.zip&delay="+uploadDelay+"&initialDelay="+initialUploadDelay+"&readLock=changed&readLockMinAge=60s&idempotentRepository=#fileConsumerRepo&idempotentKey=s3-ch-${file:name}-${file:size}-${file:modified}")
        .routeId("_av2ch upload_")
        .log(LoggingLevel.INFO, "Uploading DM01-CH-File: ${in.header.CamelFileNameOnly}")        
        .convertBodyTo(byte[].class)
        .setHeader(S3Constants.CONTENT_LENGTH, simple("${in.header.CamelFileLength}"))
        .setHeader(S3Constants.KEY,simple("${in.header.CamelFileNameOnly}"))
        .setHeader(S3Constants.CANNED_ACL,simple("PublicRead")) 
        .to("aws-s3://" + awsBucketNameCH
                + "?deleteAfterWrite=false&region=EU_CENTRAL_1" 
                + "&accessKey={{awsAccessKey}}"
                + "&secretKey=RAW({{awsSecretKey}})")
        .log(LoggingLevel.INFO, "Upload DM01-CH done: ${in.header.CamelFileNameOnly}");
        
        /*
         * Convert Bundesmodell to DXF-Geobau.
         */
        from("file://"+pathToAv2ChFolder+"/?noop=true&include=.*\\.itf&delay="+convertDelay+"&initialDelay="+initialConvertDelay+"&readLock=changed&readLockMinAge=60s&idempotentRepository=#fileConsumerRepo&idempotentKey=av2geobau-${file:name}-${file:size}-${file:modified}")        
        .routeId("_av2geobau_")
        .log(LoggingLevel.INFO, "Converting file to DXF-Geobau: ${in.header.CamelFileNameOnly}")        
        .process(new Av2GeobauProcessor())
        .to("file://"+pathToAv2GeobauFolder+"?fileName=${file:name.noext}.zip")
        .log(LoggingLevel.INFO, "Convertion to DXF-Geobau done: ${in.header.CamelFileNameOnly}");

        /*
         * Upload "DXF-Geobau" to S3 every n seconds or minutes.
         */
        from("file://"+pathToAv2GeobauFolder+"/?noop=true&include=.*\\.zip&delay="+uploadDelay+"&initialDelay="+initialUploadDelay+"&readLock=changed&readLockMinAge=60s&idempotentRepository=#fileConsumerRepo&idempotentKey=s3-dxf-${file:name}-${file:size}-${file:modified}")
        .routeId("_av2geobau upload_")
        .log(LoggingLevel.INFO, "Uploading DXF-Geobau-File: ${in.header.CamelFileNameOnly}")        
        .convertBodyTo(byte[].class)
        .setHeader(S3Constants.CONTENT_LENGTH, simple("${in.header.CamelFileLength}"))
        .setHeader(S3Constants.KEY,simple("${in.header.CamelFileNameOnly}"))
        .setHeader(S3Constants.CANNED_ACL,simple("PublicRead")) 
        .to("aws-s3://" + awsBucketNameDXF
                + "?deleteAfterWrite=false&region=EU_CENTRAL_1" 
                + "&accessKey={{awsAccessKey}}"
                + "&secretKey=RAW({{awsSecretKey}})")
        .log(LoggingLevel.INFO, "Upload DXF-Geobau done: ${in.header.CamelFileNameOnly}");

        /*
         * Import ITF files into database three times a day (12:00 and 18:00 and 23:00).
         */
        //from("file://"+pathToUnzipFolder+"/?noop=true&charset=ISO-8859-1&include=.*\\.itf&delay=30000&initialDelay=5000&readLock=changed&readLockMinAge=60s&idempotentRepository=#fileConsumerRepo&idempotentKey=ili2pg-${file:name}-${file:size}-${file:modified}")
        from("file://"+pathToUnzipFolder+"/?noop=true&charset=ISO-8859-1&include=.*\\.itf&scheduler=spring&scheduler.cron="+importCronScheduleExpression+"&readLock=changed&idempotentRepository=#fileConsumerRepo&idempotentKey=ili2pg-${file:name}-${file:size}-${file:modified}")
        .routeId("_ili2pg_")
        .log(LoggingLevel.INFO, "Importing File: ${in.header.CamelFileNameOnly}")        
        .setProperty("dbhost", constant(dbHostEdit))
        .setProperty("dbport", constant(dbPortEdit))
        .setProperty("dbdatabase", constant(dbDatabaseEdit))
        .setProperty("dbschema", constant(dbSchemaEdit))
        .setProperty("dbusr", constant(dbUserEdit))
        .setProperty("dbpwd", constant(dbPwdEdit))
        .setProperty("dataset", simple("${header.CamelFileName.substring(0,4)}"))
        .process(new Ili2pgReplaceProcessor());
        
        from("timer:foo?period=1h")
        .routeId("_cleaning_")
        .log(LoggingLevel.INFO, "Cleaning temporary directory.")
        .process(new CleanTemporaryDirectory());
    }
}
