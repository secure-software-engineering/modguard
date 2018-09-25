# Ubuntu 16.04 with Java 9 installed.
# Build image with:  sudo docker build -t andann/modguard:latest .

FROM ubuntu:16.04
LABEL version="1.0"
LABEL description="TSE 2018: evaluation artifact \
 ModGuard: Identifying Integrity & Confidentiality Violations in Java Modules."


ADD jdk-9_linux-x64_bin.tar.gz /opt/

COPY jdk-8u161-linux-x64.tar.gz /tmp/


ADD tomcat_modularization_2017_09_19.tar.gz /root/


COPY /files /root/

ADD doop-4.0.0.tar /root/

COPY SuSi /root/SuSi


COPY pa-datalog_0.5-1xenial.deb /tmp


RUN \
    cd /tmp \
    && apt-get update \
    && apt-get install -y python3 python3-pip wget openssl ca-certificates \
    && apt-get install -y libtcmalloc-minimal4 libgoogle-perftools4 protobuf-compiler libprotobuf-dev \
                     libprotobuf-java libboost-date-time1.58.0 libboost-filesystem1.58.0 libboost-iostreams1.58.0 \
                     libboost-program-options1.58.0 libboost-date-time1.58.0 libboost-system1.58.0 libboost-thread1.58.0 \
                     libcppunit-1.13-0v5 libboost-regex1.58.0 realpath locales git time \
    && dpkg -i pa-datalog_0.5-1xenial.deb \
    && apt-get install -f -y \
#	&& wget --no-check-certificate -c --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u151-b12/e758a0de34e24606bca991d704f6dcbf/jdk-8u161-linux-x64.tar.gz \
    && tar xzf jdk-8u161-linux-x64.tar.gz -C /opt \
    && rm /tmp/jdk-8u161-linux-x64.tar.gz \
    && update-alternatives --install "/usr/bin/java" "java" "/opt/jdk1.8.0_161/bin/java" 2000 \
    && update-alternatives --install "/usr/bin/javac" "javac" "/opt/jdk1.8.0_161/bin/javac" 2000  \
    && update-alternatives --install "/usr/bin/jar" "jar" "/opt/jdk1.8.0_161/bin/jar" 2000 \
    && update-alternatives --install "/usr/bin/javap" "javap" "/opt/jdk1.8.0_161/bin/javap" 2000  \
    && update-alternatives --install "/usr/bin/jdeps" "javap" "/opt/jdk1.8.0_161/bin/jdeps" 2000  \
    && update-alternatives --install "/usr/bin/java" "java" "/opt/jdk-9/bin/java" 1 \
    && update-alternatives --install "/usr/bin/javac" "javac" "/opt/jdk-9/bin/javac" 1  \
    && update-alternatives --install "/usr/bin/jar" "jar" "/opt/jdk-9/bin/jar" 1 \
    && update-alternatives --install "/usr/bin/javap" "javap" "/opt/jdk-9/bin/javap" 1  \
    && update-alternatives --install "/usr/bin/jdeps" "javap" "/opt/jdk-9/bin/jdeps" 1  \
    && pip3 install gitpython urwid \
    && locale-gen en_US.UTF-8 \
    && echo "export DOOP_HOME=/root/doop-4.0.0" >> /root/.bashrc \
    && echo "source /opt/lb/pa-datalog/lb-env-bin.sh" >> /root/.bashrc \
    && mkdir /root/doop-4.0.0/cache \
    && mkdir /root/doop-4.0.0/out \
    && wget -q http://archive.apache.org/dist/ant/binaries/apache-ant-1.10.1-bin.tar.gz \
    && tar -xzf apache-ant-1.10.1-bin.tar.gz \
    && mv apache-ant-1.10.1 /opt/ant \
    && rm apache-ant-1.10.1-bin.tar.gz \
    && ln -s /opt/apache-ant-1.10.1 /opt/ant \
    && update-alternatives --install "/usr/bin/ant" "ant" "/opt/ant/bin/ant" 1 \
    && update-alternatives --set "ant" "/opt/ant/bin/ant"



 #  && useradd -ms /bin/bash eval  \
#    cd ~ && \
#    mkdir WALA_RESULTS &&\
 #   mkdir SOOT_RESULTS &&\
 #   mkdir WALA_ENTRYPOINTS &&\
 #   mkdir TEST_CASES_RESULTS && \
 #   chown -R eval:eval /home/eval
    

# USER eval
# WORKDIR /home/eval
WORKDIR /root

#ENTRYPOINT ["/bin/bash", "-c", "python3", "evaluation.py", "gui"]  
