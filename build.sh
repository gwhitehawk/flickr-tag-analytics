#!/bin/bash
NUMBER_OF_NEIGHBORS=$1
WEIGHT_THRESHOLD=$2
SOURCE_PATH=$3
TARGET_FILE=$4

javac TaggerApp.java
java TaggerApp $NUMBER_OF_NEIGHBORS $WEIGHT_THRESHOLD $SOURCE_PATH $TARGET_FILE.txt
python parse_graph.py $TARGET_FILE.txt $TARGET_FILE.dot
dot -Tps $TARGET_FILE.dot -o $TARGET_FILE.ps
