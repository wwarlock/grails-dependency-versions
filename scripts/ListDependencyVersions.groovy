/**
 * List all dependency versions used by your app.
 *
 * @author Dmitriy Aleshkowsky
 *
 */
import grails.util.BuildSettings

includeTargets << grailsScript("_GrailsSettings")

USAGE = '''
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

'''

target(pluginVersion: "List all plugin versions used by your app") {
    (scopes, multiple) = parseAndCheckCmdLineArgs()
    def depsByScope = combineAndFilterList ( scopes, multiple, getDependencies() )

    println """

*************************************************
The dependency versions are
-------------------------------------------------
"""
    printDependencyReport(depsByScope)

    println """
*************************************************
"""
}

private LinkedHashMap combineAndFilterList(boolean scopes, boolean multiple, depsByScope) {
    if (scopes && !multiple) return depsByScope
    if (!scopes) {
        depsByScope = combineScopesToSingleList(depsByScope)
    }
    if (multiple) {
        depsByScope = filterByCountOfVersion(depsByScope)
    }
    return depsByScope
}

private void printDependencyReport(LinkedHashMap depList) {
    depList.each { String scope_name, deps ->
        def comment = BuildSettings.SCOPE_TO_DESC[scope_name]
        if (comment) { comment = " ($comment)" } else { comment = "" }
        println "--> Scope `$scope_name`$comment:\n"
        deps.each { name, version ->
            println "  ${name}: ${version.toString()}"
        }
        println ""
    }
}

private List parseAndCheckCmdLineArgs() {
    scopes = false
    multiple = false
    if (!args) {
        printError "Error: You have no args"
        System.exit(1)
    }
    def match = args =~ /(?m)\-group\=(scopes|combined)/
    if (match.size() > 0) {
        scopes = "scopes".equals(match[0][1])
    } else {
        printError "Error: You must select between `scopes` and `combined`"
        System.exit(1)
    }
    match = args =~ /(?m)\-filter\=(multiple|single)/
    if (match.size() > 0) {
        multiple = "multiple".equals(match[0][1])
    } else {
        printError "Error: You must select between `multiple` and `single`"
        System.exit(1)
    }
    return [scopes, multiple]
}

private printError (String errorMessage) {
    System.err.println """
$errorMessage

$USAGE
"""
}

private ByteArrayOutputStream getFullDependenciesTreeByScope(depManager) {
    final PrintStream originOut = System.out
    def baos = new ByteArrayOutputStream()
    System.setOut( new PrintStream( baos ) )
    try {
        depManager.produceReport()
    } finally {
        System.out.flush()
        System.setOut(originOut)
    }
    return baos
}

private removeASCIIcolor (String line) {
    return line.replaceAll("\u001B\\[[;\\d]*m", "")
}

private LinkedHashMap parseAndSortDependencies(ByteArrayOutputStream baos) {
    def depsByScope = [:]
    String currentScope

    removeASCIIcolor(baos.toString()).eachLine {
        if (!it) return

        // Scope name reading
        def match = it =~ /(?i)^(\w+) - .+\(total\: \d+\)$/
        if (match.size() > 0) {
            currentScope = match[0][1]
            if (!depsByScope[currentScope]) {
                depsByScope[currentScope] = new LinkedHashMap<String, HashSet<String>>()
            }
        }

        // Dependency info reading
        match = it =~ /(?i)\-{3}\ (.+):(.+):(.+)$/
        if (match.size() > 0) {
            def group = match[0][1]
            def name = match[0][2]
            def version = match[0][3]
            def deps = depsByScope[currentScope]
            def key = "${name} (${group})"
            if (!deps[key]) deps[key] = new HashSet<String>()
            deps[key] << "${version}"
            depsByScope[currentScope] = deps
        }
    }
    return depsByScope
}

private LinkedHashMap getDependencies() {
    def depManager = grailsSettings.dependencyManager
    def baos = getFullDependenciesTreeByScope(depManager)
    return parseAndSortDependencies(baos)
}

private LinkedHashMap combineScopesToSingleList(depsByScope) {
    def scopesDisabled = new LinkedHashMap<String, HashSet<String>>()
    depsByScope.each { String scope_name, def deps ->
        deps.each { name, version ->
            if (!scopesDisabled[name]) scopesDisabled[name] = new HashSet<String>()
            scopesDisabled[name] << "${version}"
        }
    }
    depsByScope = [:]
    depsByScope['scopes disabled'] = scopesDisabled
    return depsByScope
}

private LinkedHashMap filterByCountOfVersion(depsByScope) {
    def res = [:]
    depsByScope.each { String scope_name, def deps ->
        res[scope_name] = deps.findAll { name, version ->
            version.size() > 1
        }
    }
    return res
}

setDefaultTarget(pluginVersion)
