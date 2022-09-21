#!/bin/bash

#!/bin/bash

num_commits=30
path_prefix="../projects/"

options="-Drat.skip=true -Dcheckstyle.skip=true -Djacoco.skip=true -Dmaven.plugin.skip=true -Dgpg.skip=true -Dmaven.deploy.skip=true -Dinvoker.skip=true -Dshade.skip=true -Denforcer.skip=true -Djetty.skip=true -Dmaven.site.skip=true -Dassembly.skipAssembly=true -Dplexus.skip=true -Dskip.installyarn=true -Dskip.installnodenpm=true -Dskip.installnodepnpm=true -Dskip.jspm=true -Dskip.karma=true -Dskip.bower=true -Dskip.gulp=true -Dskip.grunt=true -Dskip.webpack=true -Dskip.ember=true -Dskip.npx=true -Dskip.yarn=true -Dskip.npm=true -Dskip.pnpm=true -Dmaven.antrun.skip=true"

guice=(
    "guice/apollo master"
    "guice/bobcat master"
    "guice/barge master"
    "guice/ninja develop"
    "guice/dropwizard-guice master"
    )

cdi=(
    "cdi/weld-testing master"
    "cdi/smallrye-fault-tolerance main"
    "cdi/smallrye-reactive-messaging main"
)

spring=(
    "spring/rocketmq-dashboard master"
    "spring/infovore master"
    "spring/spring-cloud-aws main"
    "spring/J-MR-Tp main"
)

for elem in "${spring[@]}";
do
    elemarray=($elem)
    echo "Processsing now: ${elem}"
    ./walk.sh ${num_commits} spring ${path_prefix}${elem} ${path_prefix}commits/${elemarray[0]}.txt ${options}
done

for elem in "${guice[@]}";
do
    elemarray=($elem)
    echo "Processsing now: ${elem}"
    ./walk.sh ${num_commits} guice ${path_prefix}${elem} ${path_prefix}commits/${elem[0]}.txt ${options}
done

for elem in "${cdi[@]}";
do
    elemarray=($elem)
    echo "Processsing now: ${elem}"
    ./walk.sh ${num_commits} cdi ${path_prefix}${elem} ${path_prefix}commits/${elem[0]}.txt ${options}
done
