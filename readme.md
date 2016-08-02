# arachne-buildtools

This project contains [boot](https://github.com/boot-clj/boot) code
used for building Arachne itself and its provided modules.

This library is only of interest if you want to contribute to
Arachne's core modules. If you just want to use Arachne to build your
own projects or modules, you should probably ignore it (though feel
free to use it for inspiration.)

## Usage

This tooling allows projects to store important values related to
their configuration as data in an EDN file, and have them be read and
applied by a boot script at build time.

As such, projects using this infrastructure will have a `build.boot`
file that looks something like this:

```clojure
;; sample build.boot

;; Include arachne-buildtools as a non-transitive dependency
(set-env! :dependencies '[[org.arachne-framework/arachne-buildtools "0.1.0" :scope "test"]])

;; Require the arachne-buildtools namespace
;; Refer `print-version` and `build` tasks so they are available for client projects
(require '[arachne.buildtools :as bt :refer [print-version build]])

;; Read the project.edn file to actually configure the projects settings and dependencies
(bt/read-project! "project.edn")

```

Of course, client projects will also put additional builds and
configuration in their `build.boot`. arachne-buildtools merely ensures
that basic builds are performed consistently, and allows client
projects to be the target of git dependencies (desscribed below.)

The `project.edn` file should have the following keys:

- `:project` - Mandatory. The groupId/artifactId the project, as a Clojure symbol.
- `:version` - Mandatory. A map containing `:major`, `:minor`,
  `:patch` and `:qualifier` values, used to build the version
  string. If `:qualifier` equal to `:dev`, then the emitted qualifier
  will be "dev-$GITSHA", using the SHA value of the latest commit to
  the repository. This is used in liu of SNAPSHOT dependencies. Other
  values of `:qualifier` (e.g, "alpha-1") are appended to the version
  without modification.
- `:description` - Optional. A short project description to be added to the emitted POM file.
- `:license` - Optional. A map of license names to URLs where the full
  license may be found. Added to the emitted POM file.
- `:deps` - A vector of dependency forms for the project, as used by
  the `:dependencies` key in a boot environment. Ensure that you use
  `:scope "test"` for all dependencies that are not intended to be
  transitive. May also contain git dependency forms (described below)
  which will be transformed to normal dependency forms that boot can
  understand, before being passed to boot.

```clojure
;; sample project.edn
{:project org.arachne-framework/arachne-core
:version {:major 0, :minor 1, :patch 0, :qualifier :dev}
:description "The core module for the Arachne web framework"
:license {"Apache Software License 2.0" "http://www.apache.org/licenses/LICENSE-2.0"}
:deps [[org.clojure/clojure "1.9.0-alpha9"]
       [aysylu/loom "0.5.4"]
       [com.stuartsierra/component "0.3.1"]
       [org.clojure/tools.logging "0.3.1"]
       [adzerk/boot-test "1.1.2" :scope "test"]]}
```

## Tasks

The most important boot task that this library provides is
`build`. `boot build` compiles the project into a Maven artifact and
installs it to the local maven repository.

Note that `build` will abort if the project has uncommitted changes,
to avoid inadvertently creating mutable dependencies.

## Git Dependencies

Git dependencies allow a project to identify a git repository & git
reference as the source for a dependency, rather than an artifact in a
Maven repository.

The following restrictions apply:

- You must still specify a maven artifact name.
- The target repository must contain a project that uses this same
  arachne-buildtools library as its build process.

When using a git dependency, the target project will be cloned to a
temporary directory, the ref will be checked out, the project will be
built, and the dependency replaced with a reference to whatever
artifact was installed in the local maven repository.

To use a git dependency, place a vector of `[repository ref]` in a
dependency form *instead* of a maven version.

For example:

```clojure
{:deps [[org.arachne-framework/arachne-core ["https://github.com/arachne-framework/arachne-core.git" "900a0ee"]]]}
```

The ref value can be a git SHA, a tag, or anything else that can be
passed to `git checkout`. As a matter of convention, it is strongly
recommended to avoid "unstable" references such as `HEAD` or branch
names so that dependencies indicate a repeatable, deterministic
reference.

## Local Dependencies

For an improved local workflow, you may also specify a relative path to another
buildtools project, using the keyword `:local` and a value. For example,

```
[org.arachne-framework/arachne-core "0.1.0-dev-249d617" :local "../arachne-core"]
```

In this case, buildtools will configure your boot project to add the
`:resource-paths` of the target project directly to your project, allowing you
to make changes to both projects without stopping to install and update
dependencies. This works transitively across multiple projects.

Because this technique relies upon a normal artifact dependency to establish
transitive dependencies, you must install the dependency as a normal artifact
firs. This also means that you will still need to reinstall the artifact and
restart your JVM if you change the dependencies of a local dependency.

Note that you cannot use `boot build` if your project has a local dependency
declared.

## Testing

Buildtools provides enhanced testing support, wrapping the `adzerk/boot-test` Boot extension.

To run the test suite, run `boot test`. This supports all the options
supported by `adzerk/boot.test`, with the following modifications:

- Tests with `:integration` metadata will not be run by default.
- If the `-i` argumement is provided (`boot test -i`), _only_ tests with `^:integration` metadata will be run.
- If the `-a` argument is provided (`boot test -a`), all tests will be run regardless of their metadata.
