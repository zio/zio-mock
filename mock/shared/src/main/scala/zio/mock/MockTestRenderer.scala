package zio.mock

import zio.stacktracer.TracingImplicits.disableAutoTrace
import zio.test._
import zio.test.render.ExecutionResult.{ResultType, Status}
import zio.test.render.LogLine.Fragment.Style
import zio.test.render.LogLine.{Fragment, Line, Message}
import zio.test.render._

trait MockTestRenderer  extends TestRenderer {
  private val tabSize = 2

  override def render(results: Seq[ExecutionResult], testAnnotationRenderer: TestAnnotationRenderer): Seq[String] =
    results.map { result =>
      val message = Message(result.lines).intersperse(Line.fromString("\n"))

      val output = result.resultType match {
        case ResultType.Suite =>
          renderSuite(result.status, result.offset, message)
        case ResultType.Test  =>
          renderTest(result.status, result.offset, message)
        case ResultType.Other =>
          Message(result.lines)
      }

      val renderedAnnotations = renderAnnotations(result.annotations, testAnnotationRenderer)
      renderToStringLines(output ++ renderedAnnotations).mkString
    }

  private def renderSuite(status: Status, offset: Int, message: Message): Message =
    status match {
      case Status.Passed  => withOffset(offset)(info("+") + sp) +: message
      case Status.Failed  => withOffset(offset)(Line.empty) +: message
      case Status.Ignored =>
        withOffset(offset)(Line.empty) +: message :+ fr(" - " + TestAnnotation.ignored.identifier + " suite").toLine
    }

  private def renderTest(status: Status, offset: Int, message: Message) =
    status match {
      case Status.Passed  => withOffset(offset)(info("+") + sp) +: message
      case Status.Ignored => withOffset(offset)(warn("-") + sp) +: message
      case Status.Failed  => message
    }

  def renderToStringLines(message: Message): Seq[String] = {
    def renderFragment(f: Fragment): String =
      f.style match {
        case Style.Default             => f.text
        case Style.Primary             => MockConsoleUtils.blue(f.text)
        case Style.Warning             => MockConsoleUtils.yellow(f.text)
        case Style.Error               => MockConsoleUtils.red(f.text)
        case Style.Info                => MockConsoleUtils.green(f.text)
        case Style.Detail              => MockConsoleUtils.cyan(f.text)
        case Style.Dimmed              => MockConsoleUtils.dim(f.text)
        case Style.Bold(fr)            => MockConsoleUtils.bold(renderFragment(fr))
        case Style.Underlined(fr)      => MockConsoleUtils.underlined(renderFragment(fr))
        case Style.Ansi(fr, ansiColor) => MockConsoleUtils.ansi(ansiColor, renderFragment(fr))
      }

    message.lines.map { line =>
      renderOffset(line.offset)(line.optimized.fragments.foldLeft("")((str, f) => str + renderFragment(f)))
    }
  }

  private def renderAnnotations(
      annotations: List[TestAnnotationMap],
      annotationRenderer: TestAnnotationRenderer
  ): Message =
    annotations match {
      case annotations :: ancestors =>
        val rendered = annotationRenderer.run(ancestors, annotations)
        if (rendered.isEmpty) Message.empty
        else Message(rendered.mkString(" - ", ", ", ""))
      case Nil                      => Message.empty
    }

  private def renderOffset(n: Int)(s: String) =
    " " * (n * tabSize) + s

  def render(summary: Summary): String =
    s""" ${summary.success} tests passed. ${summary.fail} tests failed. ${summary.ignore} tests ignored."""
}
object MockTestRenderer extends MockTestRenderer
