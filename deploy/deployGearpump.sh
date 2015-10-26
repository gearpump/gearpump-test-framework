#!/bin/bash 
alias ssh="ssh -i ~/.ssh/id_rsa";
alias scp="scp -i ~/.ssh/id_rsa";
gearpumpVersion=""
gearpumpSource=""
gearpumpDir="/root/gearpump-${gearpumpVersion}"
gearpumpPack="/root/gearpump-${gearpumpVersion}.tar.gz"

cluster=""
masters=""
ui=""
worker_num=
MASTER_CONFIG=""

function print_usage(){
  echo "Usage: deployGearpump.sh COMMAND"
  echo "       where COMMAND is one of:"
  echo "  deploy               deploy the Gearpump and start Master and Worker"
  echo "  stop                 stop all the Master and Worker"
  echo "  start                start Master and Workers"
}

if [ $# = 0 ]; then
  print_usage
  exit
fi

COMMAND=$1

function deployTar(){
  for node in $cluster; do
    ssh root@$node "rm -rf ${gearpumpDir}"
    ssh root@$node "rm -rf ${gearpumpPack}"
    scp ${gearpumpSource}/gearpump-${gearpumpVersion}.tar.gz root@$node:~
    ssh root@$node "tar -zxvf ${gearpumpPack}"
    #ssh root@$node "chmod -R 777 ${gearpumpDir}/bin"
  done
}

function startMaster(){
  for node in $masters; do
    jvmopt="\"\$JAVA_OPTS $MASTER_CONFIG -Dgearpump.hostname=$node\""    
    ssh root@$node "cd ${gearpumpDir};\\
                    export JAVA_OPTS=${jvmopt};\\
                    nohup bin/master -ip $node -port 3000 > master.log &"
  done
}

function startUI(){
  jvmopt="\"\$JAVA_OPTS $MASTER_CONFIG -Dgearpump.hostname=$ui -Dgearpump.services.host=$ui \""
  ssh root@$ui "cd ${gearpumpDir};\\
                export JAVA_OPTS=${jvmopt};\\
                nohup bin/services > service.log &" 
}

function startWorker(){
  worker=0
  while [ $worker -lt $worker_num ]; do
  for node in $cluster; do
    jvmopt="\"\$JAVA_OPTS $MASTER_CONFIG -Dgearpump.hostname=$node\""
    ssh root@$node "cd ${gearpumpDir};\\
                    export JAVA_OPTS=${jvmopt};\\
                    nohup bin/worker > worker${worker}.log &"
    done
    let worker=$worker+1
  done
}

function stopAll(){
  for node in $masters; do
    ssh root@$node "source /etc/profile; jps -Vv | grep -i 'gearpump' | awk '{print \$1}' | xargs kill"
  done
  for node in $cluster; do
    ssh root@$node "source /etc/profile; jps -Vv | grep -i 'gearpump'| awk '{print \$1}' | xargs kill"
    ssh root@$node "source /etc/profile; jps | grep -i 'ActorSystem'| awk '{print \$1}' | xargs kill -9"
  done
}

function stopAS(){
  for node in $cluster; do
    ssh root@$node "source /etc/profile; jps | grep -i 'ActorSystem'| awk '{print \$1}' | xargs kill -9"
  done
}

function initConfig(){
  index=0
  for master in $masters; do
    MASTER_CONFIG="$MASTER_CONFIG -Dgearpump.cluster.masters.${index}=$master:3000"
    let index=$index+1
  done
}

case $COMMAND in
  --help|-help|-h)
    print_usage
    exit
    ;;
  
  deploy)
    initConfig
    deployTar
    startMaster
    sleep 2
    startWorker
    startUI
    exit
    ;;
  
  start)
    initConfig
    startMaster
    sleep 2
    startWorker
    startUI
    exit
    ;;

  stop)
    stopAll
    exit
    ;;
  killAS)
    stopAS
    exit
    ;;
esac

