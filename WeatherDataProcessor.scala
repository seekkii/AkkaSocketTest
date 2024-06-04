package scala

import _root_.jdk.incubator.vector.{ByteVector, VectorSpecies}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{FileIO, Flow, Framing, Sink}
import org.apache.pekko.util.ByteString

import java.nio.file.Paths
import java.util
import java.util.{Comparator, concurrent}
import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.{implicitConversions, postfixOps}
import scala.math.Ordering.comparatorToOrdering
import scala.jdk.CollectionConverters

import java.util.{List => JList}



class Data(var min: Float, var max: Float, var mean: Float, var count: Int){
  override def toString: String = {
    f"$min%1.1f/$max%1.1f/${mean/count}%1.1f"
  }
}

def sort[T](): Unit = {

}

object WeatherDataProcessor {
  implicit val actorSystem: ActorSystem = ActorSystem("WeatherDataProcessor")
  implicit val materializer: Materializer = Materializer(actorSystem)
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  implicit class ByteStringTupleOps(bsTuple: (ByteString, ByteString)) {
    def asStrings: (String, String) = (bsTuple._1.utf8String, bsTuple._2.utf8String)
  }

  implicit val ord: java.util.Comparator[ByteString] = new Comparator[ByteString] {
    override def compare(x: ByteString, y: ByteString): Int = {
      val length = math.min(x.length, y.length)
      for (i <- 0 until length) {
        if (x(i) != y(i)) return (x(i) - y(i))
      }
      x.length - y.length
    }
  }

  val SPECIES: VectorSpecies[java.lang.Byte] = ByteVector.SPECIES_256

  private def simdProcessFrame(bs: Array[Byte]): ByteString = {
    val state = true
    val firstBuilder = new Array[Byte](bs.length)
    println(firstBuilder.mkString("Array(", ", ", ")"))
    for (i<- bs.indices by SPECIES.length()){
      val vec = ByteVector.fromArray(SPECIES,bs,i)
      vec.intoArray(firstBuilder,i)
    }
    ByteString.fromArrayUnsafe(firstBuilder)
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

  def main(args: Array[String]): Unit = {
    val start = System.currentTimeMillis()
    val unOrderedRes = mutable.HashMap[ByteString,Data]()//ConcurrentHashMap[ByteString,Data]()
    val path = "/Users/tung_ph/IdeaProjects/fkoff/resources/weather_stations.csv"

    val source = FileIO.fromPath(Paths.get(path))
    val separator = ';'.toByte

    val flow = Flow[ByteString]
      .via(Framing.delimiter(ByteString('\n'), maximumFrameLength = 1049))
      .async
      .map(bs =>
        //simdProcessFrame(bs.toArray)
                val pos = bs.indexOf(separator)
                val (first, second) = bs.splitAt(pos)
                val fah: Float = second.drop(1).utf8String.toFloat
                val data = unOrderedRes.getOrElse(first,null)

//                val value = unOrderedRes.(first, _ => new Data(fah, fah, 0, 0))
//
//                if (value.min > fah) value.min = fah
//                if (value.max < fah) value.max = fah
//                value.mean += fah
//                value.count += 1

                data match {
                  case null => unOrderedRes.put(first, new Data(fah, fah, 0,1))
                  case value =>
                    if (value.min > fah) value.min = fah
                    if (value.max < fah) value.max = fah
                    value.mean += fah
                    value.count+=1
                }
      )

    val stream = source
      .async
      .via(flow)
      .runWith(Sink.ignore)
    stream.onComplete { _ =>
//      val keys = new util.ArrayList[ByteString](unOrderedRes.keySet())
//      keys.sort(ord)
//      keys.

      unOrderedRes
        .keys
        .toArray
        .sortBy(identity)
        
        .foreach(key => println(s"${key.utf8String}=${unOrderedRes.get(key)}, "))

      val end = System.currentTimeMillis()
      println(s"Elapsed time: ${end - start} milliseconds")
      actorSystem.terminate()
    }
  }
}


