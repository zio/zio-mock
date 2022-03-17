/*
 * Copyright 2019-2022 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zio.mock.internal

import zio.mock.Capability
import zio.stacktracer.TracingImplicits.disableAutoTrace
import zio.test.Assertion

/** An `InvalidCall` represents failed expectation.
  */
sealed abstract class InvalidCall

object InvalidCall {

  final case class InvalidArguments(
      invoked: Capability.Signature,
      args: Any,
      assertion: Assertion[Any]
  ) extends InvalidCall

  final case class InvalidCapability[R, In, E, A](
      invoked: Capability.Signature,
      expected: Capability[R, In, E, A],
      assertion: Assertion[In]
  ) extends InvalidCall

  final case class InvalidPolyType[R, In, E, A](
      invoked: Capability.Signature,
      args: Any,
      expected: Capability[R, In, E, A],
      assertion: Assertion[In]
  ) extends InvalidCall
}
