#!/usr/bin/bash

cd $(dirname "$BASH_SOURCE")
mkdir -p lib
cd lib

curl --location --remote-name \
  http://update.sugarj.org/plugins/org.spoofax.jsglr_1.2.0.201401151249.jar
curl --location --remote-name \
  http://update.sugarj.org/plugins/org.spoofax.terms_1.2.0.201401151249.jar

mv org.spoofax.jsglr_1.2.0.201401151249.jar org.spoofax.jsglr.jar
mv org.spoofax.terms_1.2.0.201401151249.jar org.spoofax.terms.jar
