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


## Prerequisites

* Linux or Mac OS (Windows might do, but it's untested)
* Java 7
* if you want to build Sushi: Maven 3

## Usage

Sushi is available from Maven Central. To use it in a Maven project, add this dependency:

    <dependency>
      <groupId>net.oneandone</groupId>
      <artifactId>sushi</artifactId>
      <version>2.8.18</version>
    </dependency>

## Optional Dependencies

Sushi itself has several transitive dependencies that are marked optional because not everybody needs them. You have to add them to your
project if you want to use the respective sushi feature:

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


# Tests

`mvn clean test`only runs a subset of the available tests.
Some of the test need special setup. To run them, adjust `test.properties` and run `mvn test -Dfull`

## Ssh Setup

Make sure to can ssh to the host specified in test.properties, authenticating with your public key, not a password. If the key is protected with a passphrase, store this passphrase in ~/.ssh/passphrase to make if available to SshNodes.

## Webdav Setup

### Ubuntu 10.10 or later

(see also: http://how-to.linuxcareer.com/webdav-server-setup-on-ubuntu-linux)

* `sudo apt-get install apache2`
* `sudo a2enmod dav`
* `sudo a2enmod dav_fs`
* `sudo mkdir /var/www/webdav`
* `sudo chown www-data:www-data /var/www/webdav`
* `sudoedit /etc/apache2/mods-available/dav_fs.conf`
>     DAVLockDB ${APACHE_LOCK_DIR}/DAVLock
>     <Directory "/var/www/webdav">
>       Dav On
>       Options +Indexes
>     </Directory>
>     Alias /webdav /var/www/webdav

* ensure the lock file exists:
  * `sudo touch /var/lock/apache2/DAVLock`
  * `sudo chown www-data:www-data /var/lock/apache2/DAVLock`
* `sudo /etc/init.d/apache2 reload`
* point your browser to `http://localhost/webdav` to verify. You should get an empty directory listing

### Mac OS

(tested with El Capitan)

* sudo apachectl start
* /etc/apache2/httpd.conf: make sure the following lines are present
    * LoadModule dav_module
    * LoadModule dav_fs_module
    * LoadModule dav_lock_module
    * Include /private/etc/apache2/extra/httpd-dav.conf

* In /private/etc/apache2/extra/httpd-dav.conf add
>     Alias /webdav "/Library/WebServer/WebDAV"
>     <Directory "/Library/WebServer/WebDAV">
>        Dav On
>        Options +Indexes
>        Require all granted
>     </Directory>
and change the lock file to
> DavLockDB "/var/webdav/DavLock"

* `sudo mkdir -p /var/webdav` for lock file
* `sudo chown -R _www:_www /var/webdav`
* `sudo mkdir -p /Library/WebServer/WebDAV` for data
* `sudo chown -R _www:_www /Library/WebServer/WebDAV`
* `sudo apachectl graceful`
* point your browser to `http://localhost/webdav`.
* trouble shooting
  * try "apachectl -S" to check the apache config
  * open http://localhost:80 to check if apache starts
  * if you get 'permission denied', you forgot Options +Indexes