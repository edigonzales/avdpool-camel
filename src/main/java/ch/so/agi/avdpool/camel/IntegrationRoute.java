package ch.so.agi.avdpool.camel;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.s3.S3Constants;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ch.so.agi.camel.processors.Av2chProcessor;

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

    @Value("${app.awsAccessKey}")
    private String awsAccessKey;

    @Value("${app.awsSecretKey}")
    private String awsSecretKey;
    
    @Value("${app.awsBucketNameSO}")
    private String awsBucketNameSO;
    
    @Value("${app.awsBucketNameCH}")
    private String awsBucketNameCH;

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

    @Override
    public void configure() throws Exception {
        /*
         * Send an email if an exception occures.
         */
        onException(Exception.class)
        .setHeader("subject", simple("AV-Import/-Export: Fehler"))
        .setHeader("to", simple(emailUserRecipient))
        .setBody(simple("Route Id: ${routeId} \n Date: ${date:now:yyyy-MM-dd HH:mm:ss} \n File: ${in.header.CamelFileAbsolutePath} \n Message: ${exception.message} \n Stacktrace: ${exception.stacktrace}"))
        .to(emailSmtpSender+"?username="+emailUserSender+"&password="+emailPwdSender);

        /*
         * Download ITF (ZIP) files from Infogrips FTP server every n seconds or minutes.
         */
        from("ftp://"+ftpUserInfogrips+"@"+ftpUrlInfogrips+"/\\dm01avso24lv95\\itf\\?password="+ftpPwdInfogrips+"&antInclude=*.zip&autoCreate=false&noop=true&readLock=changed&stepwise=false&separator=Windows&passiveMode=true&binary=true&delay="+downloadDelay+"&initialDelay="+initialDownloadDelay+"&idempotentRepository=#fileConsumerRepo&idempotentKey=ftp-${file:name}-${file:size}-${file:modified}")
        .routeId("_download_")
        .to("file://"+pathToDownloadFolder)
        .split(new ZipSplitter())
        .streaming().convertBodyTo(String.class, "ISO-8859-1") 
            .choice()
                .when(body().isNotNull())
                    .to("file://"+pathToUnzipFolder+"?charset=ISO-8859-1")
            .end()
        .end()
        .log(LoggingLevel.INFO, "File unzipped: ${in.header.CamelFileNameOnly}"); 
        
        /*
         * Upload the unzipped ITF files to S3 every n seconds or minutes.
         */
        from("file://"+pathToUnzipFolder+"/?noop=true&include=.*\\.itf&delay="+uploadDelay+"&initialDelay="+initialUploadDelay+"&readLock=changed&idempotentRepository=#fileConsumerRepo&idempotentKey=s3-${file:name}-${file:size}-${file:modified}")
        .routeId("_upload_")
        .convertBodyTo(byte[].class)
        .setHeader(S3Constants.CONTENT_LENGTH, simple("${in.header.CamelFileLength}"))
        .setHeader(S3Constants.KEY,simple("${in.header.CamelFileNameOnly}"))
        .setHeader(S3Constants.CANNED_ACL,simple("PublicRead")) // https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/model/CannedAccessControlList.html
        .to("aws-s3://" + awsBucketNameSO
                + "?deleteAfterWrite=false&region=EU_CENTRAL_1" //https://docs.aws.amazon.com/de_de/general/latest/gr/rande.html https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/regions/Regions.html
                + "&accessKey={{awsAccessKey}}"
                + "&secretKey=RAW({{awsSecretKey}})")
        .log(LoggingLevel.INFO, "DM01-SO-File uploaded: ${in.header.CamelFileNameOnly}");
        
        /*
         * Convert ITF files to "Bundesmodell" (DM01AVCH24DLV95) every n seconds or minutes.
         * Be careful: The library writes the error log messages to dev/null since it was really verbose.
         * It should restore the default behaviour but in there can be exotic corner cases... 
         */
        from("file://"+pathToUnzipFolder+"/?noop=true&charset=ISO-8859-1&include=.*\\.itf&delay="+convertDelay+"&initialDelay="+initialConvertDelay+"&readLock=changed&idempotentRepository=#fileConsumerRepo&idempotentKey=av2ch-${file:name}-${file:size}-${file:modified}")
        .routeId("_av2ch_")
        .process(new Av2chProcessor())
        .to("file://"+pathToAv2ChFolder+"/")
        .log(LoggingLevel.INFO, "File converted to DM01-CH: ${in.header.CamelFileNameOnly}");

        /*
         * Upload "Bundesmodell" to S3 every n seconds or minutes.
         */
        from("file://"+pathToAv2ChFolder+"/?noop=true&include=.*\\.itf&delay="+uploadDelay+"&initialDelay="+initialUploadDelay+"&readLock=changed&idempotentRepository=#fileConsumerRepo&idempotentKey=s3-ch-${file:name}-${file:size}-${file:modified}")
        .routeId("_av2ch upload_")
        .convertBodyTo(byte[].class)
        .setHeader(S3Constants.CONTENT_LENGTH, simple("${in.header.CamelFileLength}"))
        .setHeader(S3Constants.KEY,simple("${in.header.CamelFileNameOnly}"))
        .setHeader(S3Constants.CANNED_ACL,simple("PublicRead")) 
        .to("aws-s3://" + awsBucketNameCH
                + "?deleteAfterWrite=false&region=EU_CENTRAL_1" 
                + "&accessKey={{awsAccessKey}}"
                + "&secretKey=RAW({{awsSecretKey}})")
        .log(LoggingLevel.INFO, "DM01-CH-File uploaded: ${in.header.CamelFileNameOnly}");
        
        /*
         * Import ITF files into database three times a day (12:00 and 18:00 and 23:00).
         */
        //from("file://"+pathToUnzipFolder+"/?noop=true&charset=ISO-8859-1&include=.*\\.itf&scheduler=spring&scheduler.cron=*+*+*+*+*+*&initialDelay=5000&readLock=changed&idempotentRepository=#fileConsumerRepo&idempotentKey=ili2pg-${file:name}-${file:size}-${file:modified}")
        from("file://"+pathToUnzipFolder+"/?noop=true&charset=ISO-8859-1&include=.*\\.itf&scheduler=spring&scheduler.cron="+importCronScheduleExpression+"&readLock=changed&idempotentRepository=#fileConsumerRepo&idempotentKey=ili2pg-${file:name}-${file:size}-${file:modified}")
        .routeId("_ili2pg_")
        .toD("ili2pg:replace?dbhost="+dbHostEdit+"&dbport="+dbPortEdit+"&dbdatabase="+dbDatabaseEdit+"&dbschema="+dbSchemaEdit+"&dbusr="+dbUserEdit+"&dbpwd="+dbPwdEdit+"&dataset=${in.header.CamelFileNameOnly.substring(0,4)}")
        .log(LoggingLevel.INFO, "File imported: ${in.header.CamelFileNameOnly}");
    }
}
