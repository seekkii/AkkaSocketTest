package Services

import java.io.{BufferedReader, File, InputStreamReader}

object VideoStreaming {
  private val isWindows: Boolean = System.getProperty("os.name").toLowerCase.startsWith("windows")
  private val builder = new ProcessBuilder()
  private val env = builder.environment()
  builder.directory(new File(System.getProperty("user.home")))
  val currentDir = System.getProperty("user.dir")

  val ffmpegPath = if (isWindows)
    currentDir + "\\app\\ffmpeg\\win32-x64\\lib\\ffmpeg.exe"
  else
    "/path/to/your/project/directory/ffmpeg"
  def startProcess(path: String): Unit = {
    val command = s"$ffmpegPath -i $path -f rawvideo -"
    val cmdArray = if (isWindows) Array("cmd.exe", "/c", command) else Array("sh", "-c", command)

    builder.command(cmdArray: _*) // Pass the command as varargs

    val process: Process = builder.start()
    val inStreamReader = new InputStreamReader(process.getInputStream)
    val streamGobbler = new BufferedReader(inStreamReader)

    val errorStreamReader = new InputStreamReader(process.getErrorStream)
    val errorGobbler = new BufferedReader(errorStreamReader)


    val outputReader = new Thread(() => {
      val buffer = new Array[Byte](1920*1080*4)
      var bytesRead = 0
      while ( {
        bytesRead = process.getInputStream.read(buffer); bytesRead != -1
      }) {
        println(s"Read $bytesRead bytes: ${buffer.take(bytesRead + 128).mkString(", ")}")
      }
    })

    val errorReader = new Thread(new Runnable {
      override def run(): Unit = {
        var line: String = null
        while ({line = errorGobbler.readLine(); line} != null) {
          println(s"ERROR: $line")
        }
      }
    })

    outputReader.start()
    errorReader.start()

    outputReader.join()
    errorReader.join()

    val exitCode = process.waitFor()

    // Print the exit code
    println(s"Process exited with $exitCode")
  }

  def main(args: Array[String]): Unit = {
    println(ffmpegPath)
    val path = if (isWindows) "C:\\Users\\Admin\\Downloads\\sample_960x400_ocean_with_audio.mkv" else "/Users/tung_ph/IdeaProjects/HelloWorld/src/main/scala/PE2_Leopard_4K.mkv"
    VideoStreaming.startProcess(path)
  }
}
