package ch.so.agi.avdpool.camel;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.s3.S3Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

//@Component
public class S3Route extends RouteBuilder {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${app.pathToUnzipFolder}")
    private String pathToUnzipFolder;

    @Value("${app.awsAccessKey}")
    private String awsAccessKey;

    @Value("${app.awsSecretKey}")
    private String awsSecretKey;
    
    @Value("${app.awsBucketNameSo}")
    private String awsBucketNameSo;

    @Override
    public void configure() throws Exception {
//      from("file://"+pathToUnzipFolder+"/?noop=true&delay=30000&initialDelay=5000&readLock=changed&idempotentRepository=#fileConsumerRepo&idempotentKey=s3-${file:name}-${file:size}-${file:modified}")
//      .convertBodyTo(byte[].class)
//      .setHeader(S3Constants.CONTENT_LENGTH, simple("${in.header.CamelFileLength}"))
//      .setHeader(S3Constants.KEY,simple("${in.header.CamelFileNameOnly}"))
//      .setHeader(S3Constants.CANNED_ACL,simple("public-read")) // TODO: does this work?
//      .to("aws-s3://" + awsBucketName
//              + "?deleteAfterWrite=false&region=EU_CENTRAL_1" //https://docs.aws.amazon.com/de_de/general/latest/gr/rande.html https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/regions/Regions.html
//              + "&accessKey={{awsAccessKey}}"
//              + "&secretKey=RAW({{awsSecretKey}})")
//      .log(LoggingLevel.INFO, "File uploaded: ${in.header.CamelFileNameOnly}");
    }
}
