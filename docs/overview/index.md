---
id: overview_index
title: "Summary"
---

## Getting started

Start by adding `zio-mock` as a dependency to your project:
  
```scala mdoc:passthrough
    println(s"""```scala""")
    if (zio.mock.BuildInfo.isSnapshot) {
        println(s"""resolvers += Resolver.sonatypeRepo("snapshots")""")
    }
    println(s"""libraryDependencies += "dev.zio" %% "zio-mock" % "${zio.mock.BuildInfo.version}"""")
    println(s"""```""")
```

The documentation for zio-mock is currently available on [zio.dev](https://zio.dev/ecosystem/officials/zio-mock).

