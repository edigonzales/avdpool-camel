package ch.so.agi.avdpool.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.s3.S3Constants;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DownloadRoute extends RouteBuilder {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${app.ftpUserInfogrips}")
    private String ftpUserInfogrips;

    @Value("${app.ftpPwdInfogrips}")
    private String ftpPwdInfogrips;

    @Value("${app.ftpUrlInfogrips}")
    private String ftpUrlInfogrips;

//    @Value("${app.idempotentFileUrl}")
//    private String idempotentFileUrl;

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

    @Override
    public void configure() throws Exception {
        
//        from("ftp://"+ftpUserInfogrips+"@"+ftpUrlInfogrips+"/\\gb2av\\?password="+ftpPwdInfogrips+"&antInclude=VOLLZUG*.zip&autoCreate=false&noop=true&readLock=changed&stepwise=false&separator=Windows&passiveMode=true&binary=true&delay=30000&initialDelay=5000&idempotentRepository=#fileConsumerRepo&idempotentKey=${file:name}-${file:size}-${file:modified}")
//        .to("file://"+pathToDownloadFolder)
//        .split(new ZipSplitter())
//        .streaming().convertBodyTo(String.class) // What happens when it gets huge? Is 'String.class' a problem? 
//            .choice()
//                .when(body().isNotNull())
//                    .to("file://"+pathToUnzipFolder)
//            .end()
//        .end();
        
        
        from("file://"+pathToUnzipFolder+"/?noop=true&delay=30000&initialDelay=5000")
        .convertBodyTo(byte[].class)
        .setHeader(S3Constants.CONTENT_LENGTH, simple("${in.header.CamelFileLength}"))
        .setHeader(S3Constants.KEY,simple("${in.header.CamelFileNameOnly}"))
        .to("aws-s3://ch.so.agi.geodaten"
                + "?deleteAfterWrite=false&region=EU_CENTRAL_1" //https://docs.aws.amazon.com/de_de/general/latest/gr/rande.html https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/regions/Regions.html
                + "&accessKey={{awsAccessKey}}"
                + "&secretKey=RAW({{awsSecretKey}})")
        .log("done.");



        
        
    }

}
