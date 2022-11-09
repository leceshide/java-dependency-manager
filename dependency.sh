#!/bin/sh

gitUrl=$1
branch=$2
projectName=$3

echo "工程GitUrl:"$gitUrl
echo "分支:"$branch
echo "工程名称:"$projectName


rm -rf $CUR_DIR/$projectName

git clone  --single-branch -b $branch  $gitUrl
echo "git clone success！"
echo "进入目录 && 分析 dependency... "
CUR_DIR=$(cd $(dirname $0);cd .; pwd)
echo "进入目录"$CUR_DIR/$projectName
cd $CUR_DIR/$projectName && gradle dependencies -x test

rm -rf $CUR_DIR/$projectName







