## Changelog 

### 3.2.2 (pending) 

* http nodes
  * added Request.streamResponse to generically stream method responses
  * generalized Method.post to return a stream - caution, this is an incompatible change
  * Method.get() now throws StatusException only - wrapping with Node specific exceptions was moved into HttpNode.newInputStream
  * HttpNode: added withParameters(map), withParameters(prefix, map), withParameter(name, boolean) 
    and withParameter(name, int)
  * StatusException now also returns the respective HttpNode and the header list 
  * improved Http.get 404 handling: the FileNotFoundException now contains a wrapped StageException to report the exact return code
* ZipRoot now ignores "!"s in nodes paths; this fixes resource handling when running with the springboot class loaded
* FileNode: fixed argument name in `readFrom(InputStream src)` (was `dest`)
* update parent 1.2.0 to 1.2.1


### 3.2.1 (2019-01-29) 

* HttpNode tweaks
  * added withHeaders() to configure per-node headers; note that since nodes are immutable,
    the method returns a new modifed node
  * added withEncodedQuery
  * added withParameter
* update lazy-foss-parent 1.0.2 to 1.2.0
* added Buffer methods:
  * fill(InputStream in, int max);
  * flush(OutputStream dest, int max);
  * diff(byte[] bytes, int max);


### 3.2.0 (2018-11-12)

Caution - contains some minor incompatibility

* improved Java 9+ support
  * support jrt uris in World.resource methods
  * replaced World.locateClasspathItem method by locateClasspathEntry and locatePathEntry methods;
    locateClasspathEntry methods return jars only, locatePathEntry methods also return module files;
    the new methods throw distinguished RuntimeExceptions to report if a resource is not found (ResourceNotFoundException) 
    or if a resource is from a module (ResourceFromModuleException)
  * added OS.beforeJava9
* update svnkit dependency 1.8.12 to 1.9.3
* changed Serializer.serializeChildren(node/doc) to Serializer.serializeChildren(node/doc, format); the previous
  version added *some* formatting, the new version adds formatting if the second argument is true
* renamed all methods fooAnchestor to fooAncestor
* added `Serializer.serialize(node, format)`


### 3.1.7 (2018-04-25)

* added World.file(FileNode useWorking, File file), World.file(FileNode useWorking, String filePath),
  FileNode.file(File file) and FileNode.file(String filePath) to explicitly specify the directory to resolve 
  relative paths to
* improved wirelog: consume less space, fixed \u encoding; faster
* fixed String.escape() for characters 0 .. 31
* fixed SvnRoot not beeing closed on shutdown
* fixed ChunkedInputStream: don't read underlying stream once we've seen EOF from it
* added HttpNode.postStream


### 3.1.6 (2017-08-23)

* fixed Sushi for https://bugs.openjdk.java.net/browse/JDK-6233323:
  * ZipNode.isDirectory now first tries ZipEntry.isDirectory
  * World.resource removes tailing slashes returned by ClassLoader.getResources(resournce)

* FileNode.mkdir exceptions now propagates the nested exception


### 3.1.5 (2017-07-24)

* added LineReader.collect(Collection<String>)
* added Node.writeLines(Iterator<String>)
* added Node.appendLines(Iterator<String>)
* added OnShutdown.isDeleteAtExit and resetDeleteAtExit.
* StatusException with body bytes


### 3.1.4 (2017-01-11)

* added timed Launcher.Handle.await methods


### 3.1.3 (2016-12-27)

* fix: don't try to initialize ssh agent if the environment variable is not present

* improved REST support in HttpNode
  * delete status code 200 is also considered ok (as 204 only)
  * added `put` method (and changed the existing code to also consider status code 200 as a success response)
  * added `patch` method
  * convenience methods to put and post Strings.
  * replaced individual method implementations by one generic implementation
  * moved statuscodes into separate class

* update jsch 0.1.53 to 0.1.54


### 3.1.2 (2016-07-26)

* Launcher.launch methods added
* HttpNode fix for proxies: added missing port in CONNECT method


### 3.1.1 (2016-07-08)

* fs.http
  * Added HttpFilesystem.getProxy and setProxy to configure proxies independently from Java System properties.
  * Added HttpFilesystem.setSocketFactorySelector
  * HttpNode: tolerate headers with \n line delimiter instead of \r\n
* Node.link and Node.resolveLink are parametrized now
* Added Strings.toMap.
* Added Pid.pid()


### 3.1.0 (2016-06-10)

* Node is a generic type now: Node<T extends Node>.
* Update jsch agentproxy 0.0.7 to 0.0.9.
* Update svnkit 1.8.11 to 1.8.12.
* World.getHome() returns a FileNode now.
* Renamed Node.getURI to Node.getUri and changed the behaviour: it no longer includes the userInfo. (SshNode and SvnNode used to
  include the userInfo; HttpNode did not.) Added Node.getUriWithUserInfo for cases if you need the full information.
* Added Strings.addRightOpt and addLeftOpt.


### 3.0.2 (2016-05-25)

* Fixed EOF handling in ChunkedInputStream.


### 3.0.1 (2016-04-11)

* Added HttpRoot.addExtraHeader().
* Added HttpNode.post().


### 3.0.0 (2016-03-01)

* Remove metadata stuff (xml mapping, csv, properties), it's a separate project now: https://github.com/mlhartme/metadata
* Remove cli stuff, it's a separate project now: https://github.com/mlhartme/inline
* Moved LineReader and LineFormat from fs package to io package.
* Dumped rss packages. It's out-dated and probably unused.
* Added Buffer.skip().
* Automatically detect dav support in nodes - it's no longer configured via protocol.
* Support http proxy specified by the properties http.proxyHost, http.proxyPort, http.nonProxyHosts,
  https.proxyHost, https.proxyPort, and https.nonProxyHosts
* HttpNode.size() and getLastModified() now use head requests if webdav is not available.
* Renamed webdav to http, changed naming prefixes accordingly.
  Replaced the implementation by built-in classes inspired by Apache HTTP core (which adds about 16k to sushi.jar).
* Simplified Buffer.file methods: dump the static methods (create a buffer instead) and the eof argument (test if result &lt; size()
  instead).
* Optimized Buffer.readBytes(): avoid addtional ByteArrayOutputStream if the file fits into the buffer.
* Dumped Buffer.readLine() - it was unused and the implementation was poor (and buggy for multibyte utf-8 characters).
* Cleaned up World construction: you'll normally create a world with World.create() now. The actual constructor is for internal use now.
* Simplified names in NetRc.
* Dumped World.setHome, it's read-only now.
* Dumped OnShutdown singleton, access it via World now.
* Speed-up tmp file handling by implementing it with java.nio.Files.
* Added Node.copyInto and Node.moveInto.
* Node.move, Node.copy, Node.copyDirectory and Node.copyFile now throw proper ...NotFoundExceptions if the source does not exist
  (instead of throwing Move- or CopyExceptions).
* FileNotFoundException and DirectoryNotFoundException now extend NodeNotFoundException.
* Calling Node.newOutputStream for a directory now throws NewDirectoryOutputStreamExceptions instead of FileNotFoundExceptions.
* Renamed Node.writeTo to Node.copyFileTo, SshNode.readFrom to SshNode.copyFileFrom, SvnNode.readFrom to SvnNode.copyFileFrom.
  Renamed WriteToException to CopyFileToException and ReadFromException to CopyFileFromException.
  Node.copyFile now calls Node.copyFileTo which improves performace for SvnNodes and SshNodes.
* Fixed MemoryNodes to properly reclaim memory when deleted.
* Dumped Base64 class, use java.util.Base64 instead.
* Added Node.newLineReader() methods.
* Align method names with Java 7 NIO terminology: Renamed createInputStream() to newInputStream(),
  createInputStreamDeleteOnClose() to newInputStreamDeleteOnClose()
  createOutputStream() to newOutputStream(), createAppendStream() to newAppendStream(),
  createReader() to newReader(), createWriter() to newWriter(), createAppender() to newAppender(),
  createObjectOutputStream() to newObjectOutputStream(), createObjectInputStream() to newObjectInputStream()
  and length() to size(). Also renamed CreateOutputStreamException to NewOutputStreamException,
  CreateInputStreamException to NewInputStreamException and LengthException to SizeException.
* Cli.run no longer catches RuntimeException (except ArgumentException).
* Changed World.getHome and setHome to work with FileNodes.
* Changed World.getWorking and setWorking to work with FileNodes.
* Update svnkit 1.8.5 to 1.8.11. No code changes, just the dependency update.
  CAUTION: this introduces a dependency update from jna 3.4.0 to 4.1.0.
* Update jsch 0.1.51 to 0.1.53. No code changes, just the dependency update.
* Compile for Java 8.


### 2.8.19 (2016-02-11)

* Make World fileFilesystem and memoryFilesystem configurable for improved testability.
* Add support for ssh port in authority string in SshFilesystem.


### 2.8.18 (2015-01-30)

* SvnNode did not remove temp file when the create is closed.


### 2.8.17 (2014-08-14)

* SshFilesystem: do not accumulate identifies when ssh-agent is used. This was cause by using the RemoteIdentityRepository - and 
  added identities were propagated to the clients. The fix sticks with LocalIdentityRepository, and adds all remote identities to
  it when starting.


### 2.8.16 (2014-08-06)

* Fixed memory leak in SvnNode and added SvnRoot.dispose to work-around another leak.
* Updated jsch 0.1.50 to 0.1.51.
* Updated svnkit 1.8.3-1 to 1.8.5.


### 2.8.15 (2014-07-23)

* Fixed webdav nodes to work with httpcore 4.3.2 (simply by fixing the deprecation warning issued by httpcore 4.2.5).
* Added FileNode.zip method.
* Fixed Strings.removeLeft and removeRight when removing empty Strings.


### 2.8.14 (2014-05-05)

* Fix stack overflow when moving files between filesystems.
* Added ~/.netrc support for Ssh and Webdav nodes.
* Added support for username/password authentication in SshNodes.
  
  
### 2.8.13 (2014-01-31)

* Autoflush was missing for verbose console output.
* Support binary files Copy class.
* ZipNode.isFile properly propagates IOExceptions now.


### 2.8.12 (2013-10-21)

* Added overwrite switch to Node.move().


### 2.8.11 (2013-10-08)

* Added Filter.matches(path).


### 2.8.10 (2013-08-11)

* Node.copy() copies all files now, default excludes are no longer used.
* Fixed FileNode.move() across file systems.
* Update jsch 0.1.49 to 0.1.50 and agent-proxy 0.0.5 to 0.0.6.
* Added optimized SshNode.writeBytes.
* SshNode.readFrom throws an ReadFrom exception now.


### 2.8.9 (2013-04-19)

* Improved PrefixWriter: it's a PrintWriter now, arbirtary newline strings, and the prefix is modifyable now.


### 2.8.8 (2013-04-08)

* Properties Saver now works with HashMap instead of Properties; it can preserve property ordering by
  using a LinkedHashMap now.


### 2.8.7 (2013-02-12)

* Fix missing flush in Console.readline.
* Do not try to connect to ssh agent unless SSH_AUTH_SOCKET is defined.


### 2.8.6 82013-02-06)

* Graph package was removed from Sushi - it's in a separate project now: https://github.com/mlhartme/graph
* Console: type of info and error fields changed from PrintStream to PrintWriter
  - because that simplifies filtering like PrefixWriter.
  CAUTION: as a consequence, console.info.print will no longer flush the output
  (println has not changed).
* Launcher streams to Writers and from Readers.
* TeeWriter and PrefixWriter.


### 2.8.5 (2013-01-15)

* Cli: print stacktrace only for RuntimeExceptions, not Exceptions.
* Cli: added -e default option to print a stacktrace for all error.
* Improved property loading:
  * property keys use dot to separate camel case now; 
    indexes are also separated with a dot
  * changed package from metadata.store to metadata.properties
  * improved naming
  * simplified
  * load exception reports the best possible result
* Instance.toProperties() added.
* File system initialization propagates constructor exceptions now.
* Ssh Agent support.
* SshNode.deleteFile: fix exception when called on a directory.
* SshNode.exec with OutputStream.


### 2.8.4 (2012-12-17)

* Launcher: fix input inheritance.


### 2.8.3 (2012-11-12)

* Launcher: unless otherwise specified, input is inherited now (which will support e.g. entering your sudo password)
* Added OnShutdown.dontDeleteAtExit.
* Fix typo in FileNotFoundException message.


### 2.8.2 (2012-11-08)

* Fix missing DirectoryStream.close in FileNode.list and deleteTree.


### 2.8.1 (2012-10-18)

* Http createInputStream without HEAD requests.
* Ssh Credentials reworked: Credentials are configured for SshFilesystem now, not root.
  Dumped ssh.Credentials, use jsch.Identity instead.
  And propertly detect and report invalid or missing passphrase when creating SshRoots.


### 2.8.0 (2012-10-06)

* Changed License from LGPL to Apache 2.0.
* Renamed net.sf.beezle.sushi to net.oneandone.sushi.
* Compile for Java 7 (uses "1.7" for both source and target in javac config).
* FileNode changed to operate on paths, not files now. Consequently, the constructor takes a path argument
  now, and FileNode.getFile() was replaced by FileNode.toPath().
* Changed Node.getGid, setGid, getUid and setUid to getGroup, setGroup, getOwner and setOwner.
* Changed Node.getMode and setMode to getPermissions and setPermissions.
* Unified not-found and already-exists exceptions.
* FileNode.move with paths (replacing extra processes); FileNode.rename dumped.
* FileNode.readBytes optimized.
* Various IOExceptions thrown by FileNode changed their type.
* FileNode.launcher added.


(See https://github.com/mlhartme/sushi/blob/sushi-2.8.0/src/changes/changes.xml for older changes)
