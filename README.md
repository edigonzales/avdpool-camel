[![Build Status](https://travis-ci.org/edigonzales/avdpool-camel.svg?branch=master)](https://travis-ci.org/edigonzales/avdpool-camel)
# avdpool-camel
Import of cadastral surveying data into PostgreSQL with Apache Camel.

## Schema vorbereiten

```
java -jar /Users/stefan/apps/ili2pg-4.0.0-20190404.083713-30-bindist/ili2pg-4.0.0-SNAPSHOT.jar --createBasketCol --createDatasetCol --createFk --createFkIdx --createGeomIdx --createImportTabs --createEnumTabs --beautifyEnumDispName --createMetaInfo --createNumChecks --defaultSrsCode 2056 --nameByTopic --createscript create_schema.sql --dbschema agi_dm01avso24 --models DM01AVSO24LV95 
```

TODO: 
- Index auf bbart und eoart
- Grant Permissions für sogis-Umgebung.
