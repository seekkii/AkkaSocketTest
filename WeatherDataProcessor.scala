package scala

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Framing, Sink, StreamConverters}
import org.apache.pekko.util.ByteString

import java.io.{File, FileInputStream}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.language.{implicitConversions, postfixOps}


class data(var min: Float, var max: Float, var mean: Float, var count: Int){
  override def toString: String = {
    s"$min $max ${mean/count}"
  }
}

object WeatherDataProcessor {
  implicit val actorSystem: ActorSystem = ActorSystem("WeatherDataProcessor")
  implicit val materializer: Materializer = Materializer(actorSystem)
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  implicit class ByteStringTupleOps(bsTuple: (ByteString, ByteString)) {
    def asStrings: (String, String) = (bsTuple._1.utf8String, bsTuple._2.utf8String)
  }

  private def processFrame(bs: ByteString): (ByteString, ByteString) = {
    var state = true
    val firstBuilder = ArrayBuffer[Byte]()
    val secondBuilder = ArrayBuffer[Byte]()
    bs.foreach { b =>
      if (b == ';') state = false
      if (state)
        firstBuilder += b
      else
        secondBuilder += b
    }
    val first = ByteString.fromArrayUnsafe(firstBuilder.toArray)
    val second = ByteString.fromArrayUnsafe(secondBuilder.toArray)
    (first, second)
  }

  private def processFrameAsStringBuilder(bs: ByteString): (String, String) = {
    var state = true
    val firstBuilder = new StringBuilder()
    val secondBuilder = new StringBuilder()
    bs.foreach { b =>
      if (b == ';') state = false
      if (state)
        firstBuilder += b.toChar
      else
        secondBuilder += b.toChar
    }
    (firstBuilder.toString, secondBuilder.toString)
  }

  def main(args: Array[String]): Unit = {
    val start = System.currentTimeMillis()
    val orderedRes = mutable.HashMap[Byte,mutable.HashMap[String, data]]()
    val result = mutable.HashMap[String,data]()

    val path = System.getProperty("user.dir") + "\\app\\resources\\weather_stations.csv"

    val file = new File(path)
    val inputStream = new FileInputStream(file)

    val source = StreamConverters.fromInputStream(() => inputStream)
      .via(Framing.delimiter(ByteString('\n'), maximumFrameLength = 1024))

    val sink = Sink.foreach { bs : ByteString =>
      val (first,second) = processFrame(bs).asStrings
      val fah = second.drop(1).toFloat
      val data: mutable.HashMap[String, data] = orderedRes.getOrElse(first(0).toByte, null)
      data match {
        case null =>

          hMap.getOrElse(first,null) match {
            case value =>
              if (value.min > fah) value.min = fah
              if (value.max < fah) value.max = fah
              value.mean += fah
            case _ =>
          }

      }
    }

    val stream = source.runWith(sink)
    stream.onComplete { _ =>
      print(orderedRes)
      val end = System.currentTimeMillis()
      println(s"Elapsed time: ${end - start} milliseconds")
      actorSystem.terminate()
    }
  }
}



