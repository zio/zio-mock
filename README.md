[//]: # (This file was autogenerated using `zio-sbt-website` plugin via `sbt generateReadme` command.)
[//]: # (So please do not edit it manually. Instead, change "docs/index.md" file or sbt setting keys)
[//]: # (e.g. "readmeDocumentation" and "readmeSupport".)

# ZIO Mock

[![Development](https://img.shields.io/badge/Project%20Stage-Development-green.svg)](https://github.com/zio/zio/wiki/Project-Stages) ![CI Badge](https://github.com/zio/zio-mock/workflows/CI/badge.svg) [![Sonatype Releases](https://img.shields.io/nexus/r/https/oss.sonatype.org/dev.zio/zio-mock_2.13.svg?label=Sonatype%20Release)](https://oss.sonatype.org/content/repositories/releases/dev/zio/zio-mock_2.13/) [![Sonatype Snapshots](https://img.shields.io/nexus/s/https/oss.sonatype.org/dev.zio/zio-mock_2.13.svg?label=Sonatype%20Snapshot)](https://oss.sonatype.org/content/repositories/snapshots/dev/zio/zio-mock_2.13/) [![javadoc](https://javadoc.io/badge2/dev.zio/zio-mock-docs_2.13/javadoc.svg)](https://javadoc.io/doc/dev.zio/zio-mock-docs_2.13) [![ZIO Mock](https://img.shields.io/github/stars/zio/zio-mock?style=social)](https://github.com/zio/zio-mock)

## Installation

In order to use this library, we need to add the following line in our `build.sbt` file:

```scala
libraryDependencies += "dev.zio" %% "zio-mock" % "1.0.0-RC12"
```

## The Problem

Whenever possible, we should strive to make our functions pure, which makes testing such function easy. So we just need to assert on the return value. However, in larger applications there is a need for intermediate layers that delegate the work to specialized services.

For example, in an HTTP server, the first layers of indirection are so-called _routes_, whose job is to match the request and delegate the processing to downstream layers. Below this layer, there is often a second layer of indirection, so-called _controllers_, which comprises several business logic units grouped by their domain. In a RESTful API, that would be all operations on a certain model. The _controller_ to perform its job might call on further specialized services for communicating with the database, sending email, logging, etc.

If the job of the _capability_ is to call on another _capability_, how should we test it?

Let's say we have a `Userservice` defined as follows:

```scala
import zio._

trait UserService {
  def register(username: String, age: Int, email: String): IO[String, Unit]
}

object UserService {
  def register(username: String, age: Int, email: String): ZIO[UserService, String, Unit] =
    ZIO.serviceWithZIO(_.register(username, age, email))
}
```

The live implementation of the `UserService` has two collaborators, `EmailService` and `UserRepository`:

```scala
trait EmailService {
  def send(to: String, body: String): IO[String, Unit]
}

case class User(username: String, age: Int, email: String)

trait UserRepository {
  def save(user: User): IO[String, Unit]
}
```

Following is how the live version of `UserService` is implemented:

```scala
case class UserServiceLive(emailService: EmailService, userRepository: UserRepository) extends UserService {
  override def register(username: String, age: Int, email: String): IO[String, Unit] =
    if (age < 18) {
      emailService.send(email, "You are not eligible to register!")
    } else if (username == "admin") {
      ZIO.fail("The admin user is already registered!")
    } else {
      for {
        _ <- userRepository.save(User(username, age, email))
        _ <- emailService.send(email, "Congratulation, you are registered!")
      } yield ()
    }
}

object UserServiceLive {
  val layer: URLayer[EmailService with UserRepository, UserService] =
    ZLayer.fromFunction(UserServiceLive.apply _)
}
```

A pure function is such a function which operates only on its inputs and produces only its output. Command-like methods, by definition are impure, as their job is to change state of the collaborating object (performing a _side effect_). For example:

The signature of `register` method `(String, Int, String) => IO[String, Unit]` hints us we're dealing with a command. It returns `Unit` (well, wrapped in the `IO`, but it does not matter here). We can't do anything useful with `Unit`, and it does not contain any information. It is the equivalent of returning nothing.

It is also an unreliable return type, as when Scala expects the return type to be `Unit` it will discard whatever value it had (for details see [Section 6.26.1][link-sls-6.26.1] of the Scala Language Specification), which may shadow the fact that the final value produced (and discarded) was not the one we expected.

Inside the `IO` there may be a description of any side effects. It may open a file, print to the console, or connect to databases. **So the problem is "How is it possible to test a service along with its collaborators"?**

In this example, the `register` method has a service call to its collaborators, `UserRepository` and `EmailService`.  So, how can we test the live version of `UserService.register` while it has some side effects in communicating with its collaborators?

Mockists would probably claim that testing how collaborators are called during the test process allows us to test the UserService. Let's move on to the next section and see the mockists' solution in greater detail.

## The Solution

In this sort of situations we need mock implementations of our _collaborator service_. As _Martin Fowler_ puts it in his excellent article [Mocks Aren't Stubs][link-test-doubles]:

> **Mocks** are (...) objects pre-programmed with expectations which form a specification of the calls they are expected to receive.

So to test the `register` function, we can mock the behavior of its two collaborators. So instead of using production objects, we use pre-programmed mock versions of these two collaborators with some expectations. In this way, in each test case, we expect these collaborators will be called with expected inputs.

In this example, we can define these three test cases:
1. If we register a user with an age of less than 18, we expect that the `save` method of `UserRepository` shouldn't be called. Additionally, we expect that the `send` method of `EmailService` will be called with the following content: "You are not eligible to register."
2. If we register a user with a username of "admin", we expect that both `UserRepository` and `EmailService` should not be called. Instead, we expect that the `register` call will be failed with a proper failure value: "The admin user is already registered!"
3. Otherwise, we expect that the `save` method of `UserRepository` will be called with the corresponding `User` object, and the `send` method of `EmailService` will be called with this content: "Congratulation, you are registered!".

ZIO Test provides a framework for mocking our modules. In the next section, we are going to test `UserService` by mocking its collaborators.

## Mocking Collaborators

In the previous section, we learned we can test the `UserService` by mocking its collaborators. Let's see how we can mock the `EmailService` and also the `UserRepository`.

We should create a mock object by extending `Mock[EmailService]` (`zio.mock.Mock`). Then we need to define the following objects:
1. **Capability tags** — They are value objects which extend one of the `Capability[R, I, E, A]` data types, such as `Effect`, `Method`, `Sink`, or `Stream`. For each of the service capabilities, we need to create an object extending one of these data types. They encode the type of _environments_, _arguments_ (inputs), the _error channel_, and also the _success channel_ of each capability of the service.

For example, to encode the `send` capability of `EmailService` we need to extend the `Effect` capability as bellow:

  ```scala
  object Send extends Effect[(String, String), String, Unit]
  ```
2. **Compose layer** — In this step, we need to provide a layer in which used to construct the mocked object. In order to do that, we should obtain the `Proxy` data type from the environment and then implement the service interface (i.e. `EmailService`) by wrapping all capability tags with proxy.


Let's see how we can mock the `EmailService`:

```scala
// Test Sources
import zio._
import zio.mock._

object MockEmailService extends Mock[EmailService] {
  object Send extends Effect[(String, String), String, Unit]

  val compose: URLayer[Proxy, EmailService] =
    ZLayer {
      for {
         proxy <- ZIO.service[Proxy]
      } yield new EmailService {
        override def send(to: String, body: String): IO[String, Unit] =
          proxy(Send, to, body)
      }
    }
}
```

And, here is the mock version of the `UserRepository`:

```scala
object MockUserRepository extends Mock[UserRepository] {
  object Save extends Effect[User, String, Unit]

  val compose: URLayer[Proxy, UserRepository] =
    ZLayer {
      for {
        proxy <- ZIO.service[Proxy]
      } yield new UserRepository {
        override def save(user: User): IO[String, Unit] =
          proxy(Save, user)
      }
    }
}
```

## Testing the Service

After writing the mock version of collaborators, now we can use their _capability tags_ to convert them to the `Expectation`, and finally create the mock layer of the service.

For example, we can create an expectation from the `Send` capability tag of the `MockEmailService`:

```scala
import zio.test._

val sendEmailExpectation: Expectation[EmailService] =
  MockEmailService.Send(
    assertion = Assertion.equalTo(("john@doe", "You are not eligible to register!")),
    result = Expectation.unit
  )
```

The `sendEmailExpectation` is an expectation, which requires a call to `send` method with `("john@doe", "You are not eligible to register!")` arguments. If this service will be called, the returned value would be `unit`.

There is an extension method called `Expectation#toLayer` which implicitly converts an expectation to the `ZLayer` environment:

```scala
import zio.test._

val mockEmailService: ULayer[EmailService] =
  MockEmailService.Send(
    assertion = Assertion.equalTo(("john@doe", "You are not eligible to register!")),
    result = Expectation.unit
  ).toLayer
```

So we do not require to convert them to `ZLayer` explicitly. It will convert them whenever required.

1. Now let's test the first scenario discussed in the [solution](#the-solution) section:

> If we register a user with an age of less than 18, we expect that the `save` method of `UserRepository` shouldn't be called. Additionally, we expect that the `send` method of `EmailService` will be called with the following content: "You are not eligible to register."

```scala

test("non-adult registration") {
  val sut              = UserService.register("john", 15, "john@doe")
  val liveUserService  = UserServiceLive.layer
  val mockUserRepo     = MockUserRepository.empty
  val mockEmailService = MockEmailService.Send(
    assertion = Assertion.equalTo(("john@doe", "You are not eligible to register!")),
    result = Expectation.unit
  )

  for {
    _ <- sut.provide(liveUserService, mockUserRepo, mockEmailService)
  } yield assertTrue(true)
}
```

We used `MockUserRepository.empty` since we expect no call to the `UserRepository` service.

2. The second scenario is:

> If we register a user with a username of "admin", we expect that both `UserRepository` and `EmailService` should not be called. Instead, we expect that the `register` call will be failed with a proper failure value: "The admin user is already registered!"

```scala

test("user cannot register pre-defined admin user") {
  val sut = UserService.register("admin", 30, "admin@doe")

  for {
    result <- sut.provide(
      UserServiceLive.layer,
      MockEmailService.empty,
      MockUserRepository.empty
    ).exit
  } yield assertTrue(
    result match {
      case Exit.Failure(cause)
        if cause.contains(
          Cause.fail("The admin user is already registered!")
        ) => true
      case _ => false
    }
  )
}
```

3. Finally, we have to check the _happy path_ scenario:

> We expect that the `save` method of `UserRepository` will be called with the corresponding `User` object, and the `send` method of `EmailService` will be called with this content: "Congratulation, you are registered!".


```scala

test("a valid user can register to the user service") {
  val sut              = UserService.register("jane", 25, "jane@doe")
  val liveUserService  = UserServiceLive.layer
  val mockUserRepo     = MockUserRepository.Save(
    Assertion.equalTo(User("jane", 25, "jane@doe")),
    Expectation.unit
  )
  val mockEmailService = MockEmailService.Send(
    assertion = Assertion.equalTo(("jane@doe", "Congratulation, you are registered!")),
    result = Expectation.unit
  )

  for {
    _ <- sut.provide(liveUserService, mockUserRepo, mockEmailService)
  } yield assertTrue(true)
}
```

## Documentation

Learn more on the [ZIO Mock homepage](https://zio.dev/zio-mock/)!

## Contributing

For the general guidelines, see ZIO [contributor's guide](https://zio.dev/about/contributing).

## Code of Conduct

See the [Code of Conduct](https://zio.dev/about/code-of-conduct)

## Support

Come chat with us on [![Badge-Discord]][Link-Discord].

[Badge-Discord]: https://img.shields.io/discord/629491597070827530?logo=discord "chat on discord"
[Link-Discord]: https://discord.gg/2ccFBr4 "Discord"

## License

[License](LICENSE)
