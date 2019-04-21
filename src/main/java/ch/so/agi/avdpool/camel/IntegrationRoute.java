package ch.so.agi.avdpool.camel;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.s3.S3Constants;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    @Value("${app.awsAccessKey}")
    private String awsAccessKey;

    @Value("${app.awsSecretKey}")
    private String awsSecretKey;
    
    @Value("${app.awsBucketNameSO}")
    private String awsBucketNameSO;

    @Value("${app.downloadDelay}")
    private String downloadDelay;

    @Value("${app.uploadDelay}")
    private String uploadDelay;

    @Value("${app.initialDownloadDelay}")
    private String initialDownloadDelay;

    @Value("${app.initialUploadDelay}")
    private String initialUploadDelay;
    
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
        /*
         * Download ITF (ZIP) files from Infogrips FTP server every n seconds or minutes.
         */
        from("ftp://"+ftpUserInfogrips+"@"+ftpUrlInfogrips+"/\\dm01avso24lv95\\itf\\?password="+ftpPwdInfogrips+"&antInclude=*.zip&autoCreate=false&noop=true&readLock=changed&stepwise=false&separator=Windows&passiveMode=true&binary=true&delay="+downloadDelay+"&initialDelay="+initialDownloadDelay+"&idempotentRepository=#fileConsumerRepo&idempotentKey=ftp-${file:name}-${file:size}-${file:modified}")
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
        .convertBodyTo(byte[].class)
        .setHeader(S3Constants.CONTENT_LENGTH, simple("${in.header.CamelFileLength}"))
        .setHeader(S3Constants.KEY,simple("${in.header.CamelFileNameOnly}"))
        .setHeader(S3Constants.CANNED_ACL,simple("PublicRead")) // https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/model/CannedAccessControlList.html
        .to("aws-s3://" + awsBucketNameSO
                + "?deleteAfterWrite=false&region=EU_CENTRAL_1" //https://docs.aws.amazon.com/de_de/general/latest/gr/rande.html https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/regions/Regions.html
                + "&accessKey={{awsAccessKey}}"
                + "&secretKey=RAW({{awsSecretKey}})")
        .log(LoggingLevel.INFO, "File uploaded: ${in.header.CamelFileNameOnly}");
        
        /*
         * Convert ITF files to "Bundesmodell" (DM01AVCH24DLV95)
         * Be careful: The library writes the error log messages to dev/null since it was really verbose.
         * It should restore the default behaviour but in there can be exotic corner cases... 
         */
        
        
        
        /*
         * Import ITF files into database three times a day (12:00 and 18:00 and 23:00).
         */
        //from("file://"+pathToUnzipFolder+"/?noop=true&charset=ISO-8859-1&include=.*\\.itf&scheduler=spring&scheduler.cron=*+*+*+*+*+*&initialDelay=5000&readLock=changed&idempotentRepository=#fileConsumerRepo&idempotentKey=ili2pg-${file:name}-${file:size}-${file:modified}")
        from("file://"+pathToUnzipFolder+"/?noop=true&charset=ISO-8859-1&include=.*\\.itf&scheduler=spring&scheduler.cron=0+0+12,18,23+*+*+*&readLock=changed&idempotentRepository=#fileConsumerRepo&idempotentKey=ili2pg-${file:name}-${file:size}-${file:modified}")
        .toD("ili2pg:replace?dbhost="+dbHostEdit+"&dbport=5432&dbdatabase="+dbDatabaseEdit+"&dbschema="+dbSchemaEdit+"&dbusr="+dbUserEdit+"&dbpwd="+dbPwdEdit+"&dataset=${in.header.CamelFileNameOnly.substring(0,4)}");

    }
}
