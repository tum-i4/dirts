# DIRTS (Dependency-Injection-aware Regression Test Selection)

DIRTS is a static class (type) or method (nontype) level tool for Regression Test Selection that is statically aware of Dependency Injection. 

## Configuration Options

### Relevant for every mojo
| Option                | Description                                                                        | Default |
|-----------------------|------------------------------------------------------------------------------------|---------|
| `logging`             | Logging level (values in `java.util.logging.Level`)                                | `INFO`  |
| `restrictive`         | Compare fully-qualified name or signature instead of simple name or signature     | `false` |
| `useSpringExtension`  | Analyze dependencies induced by Spring                                             | `false` |
| `useGuiceExtension`   | Analyze dependencies induced by Guice                                              | `false` |
| `useCDIExtension`     | Analyze dependencies induced by CDI                                                | `false` |

### Relevant for select mojos
| Option                | Description                                                                        | Default |
|-----------------------|------------------------------------------------------------------------------------|---------|
| `standalone`          | Run in standalone mode, otherwise only tests affected by changes related to DI would be selected | `false` |

### Relevant for graph mojos

| Option                | Description                                                                        | Default |
|-----------------------|------------------------------------------------------------------------------------|---------|
| `toFile`              | Store graph representation on the filesystem instead of printing it to stdout      | `false` |


## Usage examples

DIRTS is built to be used with Maven Surefire. The simplest way to use DIRTS in a Maven project is through the DIRTS Maven plugin:

```xml
<plugin>
    <groupId>edu.tum.sse.dirts</groupId>
    <artifactId>dirts-maven-plugin</artifactId>
    <version>${dirts.version}</version>
    <configuration>
        ...
    </configuration>
</plugin>
```

### Prerequisites
1. Surefire's `excludesFile`-property needs to be set, ideally for every submodule separately. Otherwise, test exclusion will not function.
2. Analysis for the desired DI-frameworks should be enabled, by setting the respective property (e.g. `useSpringExtension`) to `true`.

### Compile, select and test in one go
Even though DIRTS analyzes plain source code, in case of certain inter-module dependencies, it may be required to compile before executing the selection procedure.
```shell
$ mvn compile dirts:typeL_select test
```

### Select, then compile and test only those modules that have at least one test selected
DIRTS creates a list of these modules in `.dirts/affected_modules` inside the folder of the outermost module.
```shell
$ mvn dirts:typeL_select
$ mvn -am -pl "$(cat .dirts/affected_modules)" compile test
```

### Combined with git - Test selection before merging
1. Checkout the main branch.
2. Run selection to create checksums and graph, if these files do not already exist. E.g.: ```$ mvn dirts:typeL_select```
3. Checkout the feature branch.
4. Run selection and tests. E.g.: ```$ mvn compile dirts:typeL_select test```


## Setup
To build DIRTS simply run:
```shell
$ mvn clean install
```
This will build the code for all modules, run all tests, and install the JARs to your local Maven repository.
