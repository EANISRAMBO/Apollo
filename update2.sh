#!/bin/bash

# first parameter is a current directory, where wallet is executing now (directory, which we should update)
# second parameter is a update directory which contains unpacked jar for update
# third parameter is a boolean flag, which indicates desktop mode
if  [ -d $1 ]
then
    
    unamestr=`uname`
    
      VERSION=$(cat VERSION)

    rm -rf $1/jre
    rm -rf $1/lib
    
    if [[ "$unamestr" == 'Linux' ]]; then
	curl -o $1/jre.tar.gz https://download.java.net/java/GA/jdk11/13/GPL/openjdk-11.0.1_linux-x64_bin.tar.gz
	tar -C $1 -zxvf $1/jre.tar.gz 
	mv $1/jdk-11.0.1 $1/jre
    fi
    
    if [[ "$unamestr" == 'Darwin' ]]; then
	curl -o $1/jre.tar.gz https://download.java.net/java/GA/jdk11/13/GPL/openjdk-11.0.1_osx-x64_bin.tar.gz
	tar -C $1 -zxvf jre.tar.gz
	mv $1/jdk-11.0.1/Contents/Home $1/jre
	rm -rf $1/jdk-11.0.1
    fi
    
    rm $1/jre.tar.gz

    if [[ "$unamestr" == 'Darwin' ]]; then
	chmod 755 $1/jre/bin/* $1/jre/lib/lib*
	chmod 755 $1/jre/lib/jspawnhelper $1/jre/lib/jli/* $1/jre/lib/lib*
    elif [[ "$unamestr" == 'Linux' ]]; then
	chmod 755 $1/jre/bin/*
    fi
    
    curl -o $1/libs.tar.gz https://s3.amazonaws.com/updates.apollowallet.org/libs/ApolloWallet-$VERSION-libs.tar.gz
    tar -C $1 -zxvf $1/libs.tar.gz
    mv $1/ApolloWallet-$VERSION-libs $1/lib

else
    echo Invalid input parameters $1
fi
