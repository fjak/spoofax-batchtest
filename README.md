# Spoofax CLI Grammar Tester

This is a hacky little tool to check a large amount of files against a
JSGLR parse table (*\*.tbl*).

## Build

For the grammar tester to build you have to resolve two dependencies by
hand:

1. org.spoofax.jsglr
2. org.spoofax.terms

Just put them into the *lib/* directory and remove the version
information, so the files are named *org.spoofax.jsglr.jar* and
*org.spoofax.terms.jar*.

These can be found as part of the spoofax eclipse plug-in
(*http://metaborg.org/wiki/spoofax/*).
Unfortunately, I have not found another form of release for these.

Gradle (*http://www.gradle.org*) is used for build automation.

