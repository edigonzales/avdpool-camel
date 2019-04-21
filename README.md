[![Build Status](https://travis-ci.org/edigonzales/avdpool-camel.svg?branch=master)](https://travis-ci.org/edigonzales/avdpool-camel)
# avdpool-camel
Import of cadastral surveying data into PostgreSQL with Apache Camel.

## Setup
SQL-Datei für DDL erzeugen:
```
java -jar /Users/stefan/apps/ili2pg-4.0.0/ili2pg-4.0.0.jar \
--dbschema agi_dm01avso24 --models DM01AVSO24LV95 \
--defaultSrsCode 2056 --createGeomIdx --createFk --createFkIdx --createEnumTabs --beautifyEnumDispName --createMetaInfo --createNumChecks --nameByTopic \
--createBasketCol --createDatasetCol --createImportTabs --createscript sql/agi_dm01avso24.sql
```

Hinweise:
- Pre- und Postscripts werden nicht in das erzeugte SQL geschrieben.
- `--strokeArcs`: Im Erfassungsmodell sollen die Kreisbogen erhalten bleiben. Im Publikationsmodell ("MOpublic") werden die Kreisbogen segmentiert.
- `--createUnique`: Kann nicht verwendet werden, da einige Attribute kantonsweit nicht eindeutig sein können.

## Betrieb

## TODO 
- pre- und postscript.sql für Edit-DB? Rollen?
- Index auf bbart und eoart
- Grant Permissions für sogis-Umgebung.
- Anpassung Modellierungshandbuch an ili2pg-4.0
