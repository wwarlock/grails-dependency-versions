Usage: grails list-dependency-versions -group=(scopes|combined) -filter=(multiple|single)
-group=(scopes|combined)
    scopes     - Display dependencies grouped by scopes
    combined   - Display dependencies from all scopes as a single list

-filter=(multiple|single)
    multiple   - Display dependency only if it has multiple versions
    single     - Display dependency even if it has the single version

The most usefull command is:
$ grails list-dependency-versions -group=combined -filter=multiple

The plugin lists all dependency versions used by your app like `grails dependency-report`.
But you may have different plugins in different scopes with different subdependencies.
And one of the subdependency may has different versions in different scopes.
If there many usages of one of the dependency then you'll see that versions.

