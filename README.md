# scala-ts

Gradlized version of [miloszpp/scala-ts](https://github.com/miloszpp/scala-ts)

TODO: better documentation is NOT coming.


tl;dr
=========
##### Requirements
- Scala 2.11

##### Getting started using Gradle
```
gradle fatJar
```

##### Usage
###### ... in a command Line
```
    java -cp ... com.mpc.scalats.CLI
```

###### ... as a Gradle plugin
I don't know how to write a Gradle plugin so I just use a JavaExec task.
```
configurations {
    customClasspath
}

dependencies {
    compile project(":myRESTProject")

    customClasspath files("scala-ts_${scalaVersion}-all-0.3.2.2.jar")
}
	
task generate(type: JavaExec) {

    classpath = sourceSets.main.runtimeClasspath + configurations.customClasspath
    main = 'com.mpc.scalats.CLI'

    args '--emit-interfaces'
    args '--emit-classes'
    args '--option-to-nullable'
    args 'com.example.MyFirstRESTDTO'
    args 'com.example.MySecondsRESTDTO'

    doFirst {
        mkdir "${project.buildDir}/ts"
        standardOutput = new FileOutputStream("${buildDir}/ts/model.ts")
    }
}
```
