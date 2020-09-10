[![Build Status](https://travis-ci.org/sogis/avdpool-camel.svg?branch=master)](https://travis-ci.org/sogis/avdpool-camel)
# avdpool-camel
Ablösung für AV-Import/-Export.
 
1. Beschreibung
2. Betriebsdokumentation
3. Entwicklerdokumentation
4. TODO
5. Incidents

## Beschreibung
Importiert die Daten der amtlichen Vermessung in die Edit-Datenbank und erstellt verschiedene (DM01-Bund, DXF-Geobau (to be done)) abgeleitete Produkte. Die Originaldateien wie auch die Derivate werden auf AWS-S3 archiviert.

Umgesetzt wurde der Prozess als Apache Camel Pipeline, die in Spring Boot läuft. Der Import in die Datenbank findet nur dreimal täglich statt. Das Herunterladen, Umwandeln und Hochladen jede Minute. Alle Zyklen können in einer Properties-Datei konfiguriert werden.

## Betriebsdokumentation
Bei jedem Git-Push wird mittels Travis das Docker-Image neu gebildet und als sogis/avdpool mit den Tags "Travis-Buildnummer" und "latest" auf Docker Hub abgelegt. Auf der Testumgebung des AGI wird viertelstündlich das latest-Image neu deployed.

### Konfiguration
Die Verbindungsparameter werden über Spring Boot Profile gesteuert. Für jede Umgebung gibt es ein application-[dev|test|int|prod]properties. Diese spezielle, zusätzliche Propertiesfile kann mit der speziellen Spring-Boot-Umgebungsvariable SPRING_PROFILES_ACTIVE gesteuert werden: SPRING_PROFILES_ACTIVE=[dev|test|int|prod] vorhanden sein.

Der Import wird über eine Cron-Schedule-Expression gesteuert, allen anderen Zyklen über Millisekundenangaben.

### Persistenz
Es ist ein Persistence-Volume notwendig (siehe `docker run`-Befehl).

### Docker
```
docker run --restart -d -p 8888:8888 -v /mnt/avdpool_data:/avdpool_data \
-e "SPRING_PROFILES_ACTIVE=test" \
-e "awsAccessKey=XXXXXX" \
-e "awsSecretKey=XXXXXX" \
-e "emailSmtpSender=XXXXXX" \
-e "emailUserSender=XXXXXX" \
-e "emailPwdSender=XXXXXX" \
-e "emailUserRecipient=XXXXXX" \
-e "ftpUserInfogrips=XXXXXX" \
-e "ftpPwdInfogrips=XXXXXX" \
-e "dbUserEdit=XXXXXX" \
-e "dbPwdEdit=XXXXXX" \
-e "TZ=Europe/Amsterdam" \
sogis/avdpool
```

- `emailSmtpSender`: z.B. `smtps://smtp.gmail.com:465`
- `emailUserRecipient`: Kommaseparierte Liste
- `TZ=Europe/Amsterdam`: Damit sollte der Ausführzeitpunkt des Cronjobs (Datenimport) transparenter sein, da das Dockerimage nicht mehr UTC o.ä., sondern unsere Zeitzone erhält.

### ili2pg
```
java -jar /Users/stefan/apps/ili2pg-4.3.1/ili2pg-4.1.0.jar \
--dbschema agi_dm01avso24 --models DM01AVSO24LV95 \
--defaultSrsCode 2056 --createGeomIdx --createFk --createFkIdx --createEnumTabs --beautifyEnumDispName --createMetaInfo --createNumChecks --nameByTopic \
--createBasketCol --createDatasetCol --createImportTabs --createscript sql/agi_dm01avso24.sql
```

Hinweise:
- Pre- und Postscripts werden nicht in das erzeugte SQL geschrieben. Die SQL-Skripte deshalb ausführen mit
```
psql --single-transaction -h XXXXXX -d edit -f sql/prescript.sql -f sql/agi_dm01avso24.sql -f sql/postscript.sql
```
- `--strokeArcs`: Im Erfassungsmodell sollen die Kreisbogen erhalten bleiben. Im Publikationsmodell ("MOpublic") werden die Kreisbogen segmentiert.
- `--createUnique`: Kann nicht verwendet werden, da einige Attribute kantonsweit nicht eindeutig sein können.
- siehe Skript-Ablage `G:\sogis\daten_tools\skripte\db_schema_definition_edit\agi_dm01avso24\` für die aktuelle Version. 

## AWS-S3
Es gibt einen Benutzer `avdpool`, welcher der Gruppe `avdpool-group` zugehört. Der Gruppe ist die Policy `avdpool-S3` zugewiesen:

```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetBucketLocation",
                "s3:ListAllMyBuckets"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:PutObjectAcl",
                "s3:GetObject",
                "s3:GetObjectAcl",
                "s3:DeleteObject"
            ],
            "Resource": [
                "arn:aws:s3:::ch.so.agi.av.dm01avso24lv95-dev/*",
                "arn:aws:s3:::ch.so.agi.av.dm01avso24lv95-test/*",
                "arn:aws:s3:::ch.so.agi.av.dm01avso24lv95/*",
                "arn:aws:s3:::ch.so.agi.av.dm01avch24lv95d-dev/*",
                "arn:aws:s3:::ch.so.agi.av.dm01avch24lv95d-test/*",
                "arn:aws:s3:::ch.so.agi.av.dm01avch24lv95d/*",
                "arn:aws:s3:::ch.so.agi.av.dxfgeobau-dev/*",
                "arn:aws:s3:::ch.so.agi.av.dxfgeobau-test/*",
                "arn:aws:s3:::ch.so.agi.av.dxfgeobau/*"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:ListBucketMultipartUploads",
                "s3:AbortMultipartUpload",
                "s3:ListMultipartUploadParts"
            ],
            "Resource": [
                "arn:aws:s3:::ch.so.agi.av.dm01avso24lv95-dev",
                "arn:aws:s3:::ch.so.agi.av.dm01avso24lv95-dev/*",
                "arn:aws:s3:::ch.so.agi.av.dm01avso24lv95-test",
                "arn:aws:s3:::ch.so.agi.av.dm01avso24lv95-test/*",
                "arn:aws:s3:::ch.so.agi.av.dm01avso24lv95",
                "arn:aws:s3:::ch.so.agi.av.dm01avso24lv95/*",
                "arn:aws:s3:::ch.so.agi.av.dm01avch24lv95d-dev",
                "arn:aws:s3:::ch.so.agi.av.dm01avch24lv95d-dev/*",
                "arn:aws:s3:::ch.so.agi.av.dm01avch24lv95d-test",
                "arn:aws:s3:::ch.so.agi.av.dm01avch24lv95d-test/*",
                "arn:aws:s3:::ch.so.agi.av.dm01avch24lv95d",
                "arn:aws:s3:::ch.so.agi.av.dm01avch24lv95d/*",
                "arn:aws:s3:::ch.so.agi.av.dxfgeobau-dev",
                "arn:aws:s3:::ch.so.agi.av.dxfgeobau-dev/*",
                "arn:aws:s3:::ch.so.agi.av.dxfgeobau-test",
                "arn:aws:s3:::ch.so.agi.av.dxfgeobau-test/*",
                "arn:aws:s3:::ch.so.agi.av.dxfgeobau",
                "arn:aws:s3:::ch.so.agi.av.dxfgeobau/*"
            ]
        },
        {
            "Effect": "Allow",
            "Action": "s3:ListBucket",
            "Resource": "*"
        }
    ]
}
```

Access Key und Secret Access Key sind am üblichen Ort in der GDI abgelegt.

## Entwicklerdokumentation

### Lokale Datenbank
```
docker run --rm --name edit-db -p 54321:5432 --hostname primary \
-e PG_DATABASE=edit -e PG_LOCALE=de_CH.UTF-8 -e PG_PRIMARY_PORT=5432 -e PG_MODE=primary \
-e PG_USER=admin -e PG_PASSWORD=admin \
-e PG_PRIMARY_USER=repl -e PG_PRIMARY_PASSWORD=repl \
-e PG_ROOT_PASSWORD=secret \
-e PG_WRITE_USER=gretl -e PG_WRITE_PASSWORD=gretl \
-e PG_READ_USER=ogc_server -e PG_READ_PASSWORD=ogc_server \
sogis/oereb-db:latest
```

Es wird das `oereb-db`-Image verwendet. (Fast) jedes andere PostgreSQL/PostGIS-Image tut es wohl auch.

### Umgebungsvariablen
Entweder werden sie z.B. in die `.bashrc`-Datei o.ä. geschrieben oder in Eclipse unter `Run configurations...` definiert. Erste Methode funktioniert unter macOS nicht: Eclipse erkennt die Umgebungsvariablen nicht.

## TODO 
- Zusätzliche Indexe in den DB-Tabellen. Welche?
- Anpassung Modellierungshandbuch an ili2pg-4.0. Anfang ist gemacht.

## Incidents
### FTP lädt keine Daten mehr herunter
2020-09-07

Nach der Warnung
```
2020-09-07 08:45:59.289  WARN 1 --- [24lv95%5Citf%5C] o.a.c.component.file.remote.FtpConsumer  : ftp://vaso@ftp.infogrips.ch/%5Cdm01avso24lv95%5Citf%5C?antInclude=*.zip&autoCreate=false&binary=true&delay=60000&idempotentKey=ftp-%24%7Bfile%3Aname%7D-%24%7Bfile%3Asize%7D-%24%7Bfile%3Amodified%7D&idempotentRepository=%23fileConsumerRepo&initialDelay=5000&noop=true&passiveMode=true&password=xxxxxx&readLock=changed&separator=Windows&stepwise=false cannot begin processing file: RemoteFile[257900.zip] due to: File operation failed: 150 ASCII data connection opened for file list.
 Read timed out. Code: 150. Caused by: [org.apache.camel.component.file.GenericFileOperationFailedException - File operation failed: 150 ASCII data connection opened for file list.
 Read timed out. Code: 150]

org.apache.camel.component.file.GenericFileOperationFailedException: File operation failed: 150 ASCII data connection opened for file list.
 Read timed out. Code: 150
```
werden keine Daten mehr vom FTP heruntergeladen, obwohl weiter gepollt wird. Ein Problem ist, dass Apache Camel scheinbar die Exception nicht sauber weiterleitet (resp. Apache Commons) und damit auch keine E-Mail verschickt werden kann. Das Pollen nach der Warnung dauert Millisekunden, vor der Warnung dauerte das Pollen jeweils ein paar Sekunden.

Nach einem Restart funktioniert es wieder. Richtig viele Informationen findet man dazu nicht.




