package zio.mock

import zio.stacktracer.TracingImplicits.disableAutoTrace

import scala.{Console => SConsole}

private[mock] trait ConsoleFormatter {
  def reset(s: Any): String

  def underlined(s: Any): String

  def green(s: Any): String

  def yellow(s: Any): String

  def red(s: Any): String

  def blue(s: Any): String

  def magenta(s: Any): String

  def cyan(s: Any): String

  def dim(s: Any): String

  def bold(s: Any): String

  def ansi(ansiColor: String, s: Any): String

  def white(s: Any): String
}

private[mock] object ConsoleFormatter {

  val bland = new ConsoleFormatter {

    override def reset(s: Any): String = s.toString

    override def underlined(s: Any): String = s.toString

    override def green(s: Any): String = s.toString

    override def yellow(s: Any): String = s.toString

    override def red(s: Any): String = s.toString

    override def blue(s: Any): String = s.toString

    override def magenta(s: Any): String = s.toString

    override def cyan(s: Any): String = s.toString

    override def dim(s: Any): String = s.toString

    override def bold(s: Any): String = s.toString

    override def ansi(ansiColor: String, s: Any): String = s.toString

    override def white(s: Any): String = s.toString

  }

  val colorful = new ConsoleFormatter {
    def reset(s: Any): String = SConsole.RESET + s.toString()

    def underlined(s: Any): String =
      SConsole.UNDERLINED + s.toString + SConsole.RESET

    def green(s: Any): String =
      SConsole.GREEN + s.toString + SConsole.RESET

    def yellow(s: Any): String =
      SConsole.YELLOW + s.toString + SConsole.RESET

    def red(s: Any): String =
      SConsole.RED + s.toString + SConsole.RESET

    def blue(s: Any): String =
      SConsole.BLUE + s.toString + SConsole.RESET

    def magenta(s: Any): String =
      SConsole.MAGENTA + s + SConsole.RESET

    def cyan(s: Any): String =
      SConsole.CYAN + s.toString + SConsole.RESET

    def dim(s: Any): String =
      "\u001b[2m" + s.toString + SConsole.RESET

    def bold(s: Any): String =
      SConsole.BOLD + s.toString + SConsole.RESET

    def ansi(ansiColor: String, s: Any): String =
      ansiColor + s.toString + SConsole.RESET

    def white(s: Any): String =
      SConsole.WHITE + s.toString + SConsole.RESET
  }

}
