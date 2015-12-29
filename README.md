# Sushi

Sushi is a scripting library for Java. It provides a simple API for

* [file system operations](https://github.com/mlhartme/sushi/blob/master/src/test/java/net/oneandone/sushi/FsSample.java)
* ssh-, webdav, and svn filesystems
* [property files] (https://github.com/mlhartme/sushi/blob/master/src/test/java/net/oneandone/sushi/PropertiesSample.java)
* process launching
* [command line parsing](https://github.com/mlhartme/sushi/blob/master/src/test/java/net/oneandone/sushi/CliSample.java)
* Diff
* ...

Rationale: I prefer to do my scripting stuff in Java, not with a scripting languages and not with a special-purpose-language like Ant. Sushi enables me to do so: it provides similar functionality like Ant or Apache Commons - but packaged in an API that makes my "scripts" almost as readable and concise as other approaches. 

Note that Sushi releases are not necessarily backward compatible. In particular, if i find a better - but incompatible - api for a given task, I'll change the api. I can't keep old apis just for compatibility (not enough time) and I don't want to have multiple alternative api's if I have a clear preference. But I'll mark incompatible changes by bumping the minor or major version number. (Version number format is major.minor.micro)

* [Maven Site](http://mlhartme.github.com/sushi/)
* [Tests](https://github.com/mlhartme/sushi/wiki/Tests)

Prerequisites
-------------

* Linux or Mac OS (Windows might do, but it's untested)
* Java 7
* if you want to build Sushi: Maven 3

Usage
-----

Sushi is available from Maven Central. To use it in a Maven project, add this dependency:

    <dependency>
      <groupId>net.oneandone</groupId>
      <artifactId>sushi</artifactId>
      <version>2.8.18</version>
    </dependency>

Optional Dependencies
---------------------

Sushi itself has several transitive dependencies that are marked optional because not everybody needs them. You have to add them to your project if you want to use the respective sushi feature:

For SshNodes (e.g. world.node("ssh//user@host/my/path")):

      <dependency>
        <groupId>com.jcraft</groupId>
        <artifactId>jsch</artifactId>
        <version>0.1.51</version>
      </dependency>

Additionally, you can add

    <dependency>
      <groupId>com.jcraft</groupId>
      <artifactId>jsch.agentproxy.core</artifactId>
      <version>0.0.7</version>
    </dependency>
    <dependency>
      <groupId>com.jcraft</groupId>
      <artifactId>jsch.agentproxy.jsch</artifactId>
      <version>0.0.7</version>
    </dependency>
    <dependency>
      <groupId>com.jcraft</groupId>
      <artifactId>jsch.agentproxy.sshagent</artifactId>
      <version>0.0.7</version>
    </dependency>
    <dependency>
      <groupId>com.jcraft</groupId>
      <artifactId>jsch.agentproxy.usocket-jna</artifactId>
      <version>0.0.7</version>
    </dependency>

to make passphrases stored in ssh agent available to Sushi.

For WebdavNodes:

    <dependency>
      <groupId>org.apache.httpcomponents</groupId>
      <artifactId>httpcore</artifactId>
      <version>4.3.2</version>
    </dependency>

For SvnNodes (e.g. world.node("svn:https//host/my/path")):

    <dependency>
      <groupId>org.tmatesoft.svnkit</groupId>
      <artifactId>svnkit</artifactId>
      <version>1.8.5</version>
    </dependency>