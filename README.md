# Spoofax CLI Batch Tester

This is a hacky little tool to check a large amount of files against a
JSGLR parse table (\*.tbl).

Loading 1000+ files scattered around in different subfolders in Eclipse
can easily bring a modern desktop system to its knees. Here comes
headless to the rescue!

## Build

For the grammar tester to build you have to resolve two dependencies by
hand:

1. org.spoofax.jsglr
2. org.spoofax.terms

Just put them into the *lib/* directory and remove the version
information, so the files are named *org.spoofax.jsglr.jar* and
*org.spoofax.terms.jar*.

These can be found as part of the spoofax eclipse plug-in
(http://metaborg.org/wiki/spoofax/).
Unfortunately, I have not found another form of release for these.

Gradle (http://www.gradle.org) is used for build automation.

If everything is set up properly a `gradle build` in the project folder will
yield the desired and executable-ready fat jar in build/libs/.

## Run

I recommend to pipe standard output to a log file, e.g.

    $ java -jar de.fjak.spoofax.batchtest -p some.lang.project/include/Lang.tbl ↵
      some.lang.project/test/**/*.lang > batchtest.log

## Develop

If you are interested in forking or tweaking the batch tester, you may use
`gradle eclipse` to get a working eclipse configuration for your system.

