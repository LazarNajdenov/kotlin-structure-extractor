# kotlin-structure-extractor

The main use of the `kotlin-structure-extractor` is to statically analyze, through the 
[Kotlin Embedded Compiler](https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-compiler-embeddable),
the Program Structure Interface(`PSI`) in a Kotlin Repository/Project, from which some metrics can be extracted for 
a secondary purpose.

From the parse tree generated for each `.kt` file, it dumps into a JSON file, i.e. `entities.json`, the following properties:
```
[{
  "entityName": "aName",
  "fullyQualifiedName": "aFullyQualifiedName",
  "container": "aFullyQualifiedNameThatContainsTheEntity",
  "type": "aType",
  
  // In case it's not a class or object, by default the following are null:
  "extends": "aSuperClassName",
  "implements": ["anInterfaceName1", ...],
  "numberOfMethods": int,
  "numberOfAttributes": int,
  
  // In case it's not a method, by default the following are null:
  "numberOfParameters": int
}, ...]
```
where each `JSON` object can be either a `CLASS`, `METHOD`, `PACKAGE`, `ATTRIBUTE`, `OBJECT`.

## Building and Running Instructions

### Requirements
  - `Gradle 7.4.2`
  - `Java 11`
### Commands
In order to build and run the tool successfully, you must run from the root of the tool directory:
```
./gradlew run --args="absolute/path/to/repo/to/analyze"
```
which, if successfull will generate an `entities.json` file inside the `output` folder of the tool.
