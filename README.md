[![Build Status](https://travis-ci.org/edigonzales/avdpool-camel.svg?branch=master)](https://travis-ci.org/edigonzales/avdpool-camel)
# avdpool-camel
Import of cadastral surveying data into PostgreSQL with Apache Camel.

## Schema vorbereiten
```
java -jar /Users/stefan/apps/ili2pg-4.0.0-20190328.174729-25-bindist/ili2pg-4.0.0-SNAPSHOT.jar --dbhost 192.168.50.8 --dbdatabase edit --dbusr ddluser --dbpwd ddluser --createBasketCol --createDatasetCol --createFk --createFkIdx --createGeomIdx --createEnumTabs --defaultSrsCode 2056 --nameByTopic --createscript fubar.sql --dbschema agi_dm01avso24 --models DM01AVSO24LV95 --schemaimport
```

TODO: Index auf bbart und eoart.
