[![Build Status](https://travis-ci.org/edigonzales/avdpool-camel.svg?branch=master)](https://travis-ci.org/edigonzales/avdpool-camel)
# avdpool-camel
Import of cadastral surveying data into PostgreSQL with Apache Camel.

## Schema vorbereiten

```
java -jar /Users/stefan/apps/ili2pg-4.0.0/ili2pg-4.0.0.jar --createBasketCol --createDatasetCol --createFk --createFkIdx --createGeomIdx --createImportTabs --createEnumTabs --beautifyEnumDispName --createMetaInfo --createNumChecks --defaultSrsCode 2056 --nameByTopic --createscript create_schema.sql --dbschema agi_dm01avso24 --models DM01AVSO24LV95 
```




Some other test:
```
java -jar /Users/stefan/apps/ili2pg-4.0.0/ili2pg-4.0.0.jar --dbhost 192.168.50.8 --dbdatabase edit --dbusr ddluser --dbpwd ddluser --disableValidation  --createFk --createFkIdx --createGeomIdx --createImportTabs --createEnumTabs --beautifyEnumDispName --createMetaInfo --createNumChecks --defaultSrsCode 2056 --nameByTopic --dbschema av_test1 --models DM01AVSO24LV95 --createTidCol --importTid --doSchemaImport --import 254900.itf
```



TODO: 
- Index auf bbart und eoart
- Grant Permissions für sogis-Umgebung.
