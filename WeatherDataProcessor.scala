package scala

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{FileIO, Flow, Framing, Sink, StreamConverters}
import org.apache.pekko.util.ByteString

import java.io.{File, FileInputStream}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.language.{implicitConversions, postfixOps}
import _root_.jdk.incubator.vector.VectorSpecies
import _root_.jdk.incubator.vector.ByteVector

import java.nio.file.Paths


class Data(var min: Float, var max: Float, var mean: Float, var count: Int){
  override def toString: String = {
    f"$min%1.1f $max%1.2f ${mean/count}%1.2f"
  }
}

object WeatherDataProcessor {
  implicit val actorSystem: ActorSystem = ActorSystem("WeatherDataProcessor")
  implicit val materializer: Materializer = Materializer(actorSystem)
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  implicit class ByteStringTupleOps(bsTuple: (ByteString, ByteString)) {
    def asStrings: (String, String) = (bsTuple._1.utf8String, bsTuple._2.utf8String)
  }

  val SPECIES: VectorSpecies[java.lang.Byte] = ByteVector.SPECIES_256

  private def simdProcessFrame(bs: Array[Byte]): (ByteString, ByteString) = {
    val state = true
    val firstBuilder = Array[Byte]()
    val secondBuilder = Array[Byte]()
    val bound = SPECIES.loopBound(bs.length)
    //println(s"bound : $bound length: ${bs.length} simdLength${SPECIES.length()}")
    for (i<- bs.indices by SPECIES.length()){
      val vec = ByteVector.fromArray(SPECIES,bs,i)
      println(vec)
      vec.intoArray(firstBuilder,i)
    }
    val first = ByteString.fromArrayUnsafe(firstBuilder)
    val second = ByteString.fromArrayUnsafe(secondBuilder)
    (first, second)
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

  def compare(firstBs: ByteString, secondBs: ByteString): Boolean = {
    val length = if (firstBs.length < secondBs.length) firstBs.length else secondBs.length
    for (i<-0 to length) {
      if (firstBs(0) < secondBs(0)){
        return true
      }else{
        return false
      }
    }
    true
  }

  def main(args: Array[String]): Unit = {
    val start = System.currentTimeMillis()
    val unOrderedRes = mutable.HashMap[ByteString,Data]()
    val result = mutable.HashMap[String,Data]()
    val path = "/Users/tung_ph/Downloads/1brc-main/data/measurements.txt"

//    val file = new File(path)
//    val inputStream = new FileInputStream(file)

//    val source = StreamConverters.fromInputStream(() => inputStream)
    val source = FileIO.fromPath(Paths.get(path))
    val seperator = ';'.toByte

    val flow = Flow[ByteString]
      .via(Framing.delimiter(ByteString('\n'), maximumFrameLength = 1024))
      .map(bs => {
        val pos = bs.indexOf(seperator)
        val (first, second) = bs.splitAt(pos)
        val fah: Float = second.drop(1).utf8String.toFloat
        val data = unOrderedRes.getOrElse(first, null)
        data match {
          case null => unOrderedRes.put(first, new Data(fah, fah, fah,1))
          case value =>
            if (value.min > fah) value.min = fah
            if (value.max < fah) value.max = fah
            value.mean += fah
            value.count+=1
        }
      })

    val stream = source
      .via(flow)
      .runWith(Sink.ignore)
    stream.onComplete { _ =>
      unOrderedRes.keys.toSeq
        .sortBy(s => s.head)
        .foreach(key => println(s"${key.utf8String} ${unOrderedRes(key)}"))

      val end = System.currentTimeMillis()
      println(s"Elapsed time: ${end - start} milliseconds")
      actorSystem.terminate()
    }
  }
}


