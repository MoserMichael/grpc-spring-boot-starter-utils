#!/bin/bash

# get stack of all running java processes
set -x

JAVA_PROCS=$(jps -lv)

echo "$JAVA_PROCS"
while IFS= read -r line; do
    pid=$(echo "$line" | awk '{print $1;}')

    echo "$line" >jstack_${pid}
    echo "" >>jstack_${pid}
    jstack -l $pid >>jstack_${pid}

done <<< "$JAVA_PROCS"
