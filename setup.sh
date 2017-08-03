sudo apt-get install unzip
curl -L -O -H "Cookie: oraclelicense=accept-securebackup-cookie" \
     -k "http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jre-8u131-linux-x64.tar.gz"
curl -LO http://nlp.stanford.edu/software/stanford-corenlp-full-2017-06-09.zip
curl -LO http://nlp.stanford.edu/software/stanford-chinese-corenlp-2017-06-09-models.jar
unzip curl -LO stanford-corenlp-full-2017-06-09.zip
ln -s stanford-corenlp-full-2017-06-09/stanford-corenlp-3.8.0-models.jar
