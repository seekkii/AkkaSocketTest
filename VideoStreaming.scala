import java.io.{BufferedReader, File, InputStreamReader}

object VideoStreaming {
  private val isWindows: Boolean = System.getProperty("os.name").toLowerCase.startsWith("windows")
  private val builder = new ProcessBuilder()
  private val env = builder.environment()
  env.put("PATH", "/usr/local/bin/ffmpeg:" + System.getenv("PATH"))
  builder.directory(new File(System.getProperty("user.home")))

  def startProcess(path: String): Unit = {
    val command = s"ffmpeg -i $path -f rawvideo -pix_fmt yuv420p resultVideo.yuv"
    val cmdArray = if (isWindows) builder.command("cmd.exe", "/c", "dir") else builder.command("sh", "-c", command)

    //builder.command(cmdArray) // Pass the command as varargs

    val process: Process = builder.start()
    val inStreamReader = new InputStreamReader(process.getInputStream)
    val streamGobbler = new BufferedReader(inStreamReader)

    // Start a separate thread to continuously read the output stream
    val outputReader = new Thread(new Runnable {
      override def run(): Unit = {
        var line: String = null
       // streamGobbler.readLine().foreach(line => println(line))
        while ({line = streamGobbler.readLine(); line} != null) {
          println(line)
        }
      }
    })

    outputReader.start()

    // Wait for the process to finish
    outputReader.join()

    val exitCode = process.waitFor()

    // Join the output reader thread

    // Print the exit code
    println(s"Process exited with $exitCode")
  }

  def main(args: Array[String]): Unit = {
    VideoStreaming.startProcess("/Users/tung_ph/IdeaProjects/HelloWorld/src/main/scala/PE2_Leopard_4K.mkv")
  }
}
