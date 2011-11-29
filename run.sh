#!/bin/bash

if [ ! -f "./apache-jena-2.6.5-incubating-20111123.112838-1-distribution.zip" ]; then
    echo "==== Downloading Apache Jena v2.6.5-incubating-SNAPSHOT distribution ..."
    wget https://repository.apache.org/content/repositories/snapshots/org/apache/jena/apache-jena/2.6.5-incubating-SNAPSHOT/apache-jena-2.6.5-incubating-20111123.112838-1-distribution.zip
else
    echo "==== [skipped] Downloading Apache Jena v2.6.5-incubating-SNAPSHOT distribution ..."

fi

if [ ! -d "./apache-jena" ]; then
    echo "==== Unzipping Apache Jena v2.6.5-incubating-SNAPSHOT ..."
    unzip apache-jena-2.6.5-incubating-20111123.112838-1-distribution.zip
    mv apache-jena-2.6.5-incubating-SNAPSHOT apache-jena
else
    echo "==== [skipped] Unzipping Apache Jena v2.6.5-incubating-SNAPSHOT ..."
fi

PWD=`pwd`
export JENAROOT="$PWD/apache-jena"
export ARQROOT="$PWD/apache-jena"
export TDBROOT="$PWD/apache-jena"


echo "Validating cheese.ttl file ..." 
$JENAROOT/bin/riot --validate cheese.ttl
for file in *.trig; do 
    echo "Validating $file file ..." 
    $JENAROOT/bin/riot --validate "${file}"
done

echo "Loading cheese.ttl file ..." 
$JENAROOT/bin/tdbloader --loc /tmp/cheese *.trig
$JENAROOT/bin/tdbloader --loc /tmp/cheese cheese.ttl

$JENAROOT/bin/tdbdump --loc /tmp/cheese > cheese.nq

$JENAROOT/bin/tdbquery --loc /tmp/cheese --query ./sparql/lista_formaggi.rq
$JENAROOT/bin/tdbquery --loc /tmp/cheese --query ./sparql/list_cheeses.rq


