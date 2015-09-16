# gearpump-test-framework
## How to run the framework 

Make sure scala and sbt are installed, if not, please install them first.

1). Clone the repository

```bash
  git clone https://github.com/gearpump/gearpump-test-framework.git
  cd gearpump-test-framework
```

2). Build package

```bash
  ## The target package path: target/gearpump-test-framework-$VERSION.tar.gz
  sbt clean packArchive ## Or use: sbt clean pack-archive
```

  After the build, there will be a package file gearpump-test-framework-${version}.tar.gz generated under target/ folder.

3). Unpack the package file

  You need to flatten the .tar.gz file to use it, on Linux, you can

```bash
  # please replace ${version} below with actual version used
  tar  -zxvf gearpump-test-framework-${version}.tar.gz
```

4). Run the framework

  To run a suite of tests from the command line, you can use the org.scalatest.tools.Runner Application.
  The basic form of a Runner invocation is:

```bash
  scala [-cp scalatest-<version>.jar:...] org.scalatest.tools.Runner [arguments]
```

  The arguments Runner accepts please check the [http://www.scalatest.org/user_guide/using_the_runner#executingSuites](scalatest doc)

  For example, on Linux:

```bash
  scala -classpath "lib/scalatest_2.11-2.2.4.jar:lib/*" org.scalatest.tools.Runner -R lib/gearpump-linux-test-0.1.jar -f report.log -o
```

  This command will run all the test cases in gearpump-linux-test-0.1.jar, "-R" specifies the runpath and "-f" causes test results to be written to the named file, "-o" causes test results to be written to the standard output. You can also specifies a suite class to run by using "-s".

  Please check [http://www.scalatest.org/user_guide/using_the_runner#executingSuites](scalatest doc) for more information about the command.


