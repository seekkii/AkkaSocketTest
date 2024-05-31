import org.apache.pekko.actor.{ActorRef, ActorSystem}
import org.apache.pekko.stream.{ActorMaterializer, Materializer}
import org.apache.pekko.stream.scaladsl.{Framing, Sink, Source, StreamConverters}
import org.apache.pekko.util.ByteString

import java.io.{BufferedReader, File, FileInputStream}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.{implicitConversions, postfixOps}
import java.io.{File, FileInputStream}
import java.util.concurrent.Executors
import scala.collection.mutable


import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.language.postfixOps
import scala.runtime.Nothing$
import scala.util.control.Breaks.break



import scala.annotation.tailrec

abstract class LinkedList[+A]{
  def foreach(f: A => Unit): Unit
  def head: A
  def tail: LinkedList[A]
  def filter(f:A => Boolean) : LinkedList[A]
  def map[B](f: A=>B) : LinkedList[B]
  def size : Int
}

case object InvalidNode extends LinkedList[Nothing] {
  override def head: Nothing = null.asInstanceOf
  override def foreach(f: Nothing => Unit): Unit = {}
  override def tail: LinkedList[Nothing] = null.asInstanceOf
  override def filter(f: Nothing => Boolean): LinkedList[Nothing] = this
  override def map[B](f: Nothing => B): LinkedList[B] = this
  def size = 0
}

case class ValidNode[A](value: A, next: LinkedList[A]) extends LinkedList[A]
{
  override def tail: LinkedList[A] = next

  override def filter(f: A => Boolean): LinkedList[A] = {
    if (f(value)) ValidNode(value, next.filter(f)) else next.filter(f)
  }
  override def map[B]( f : A => B) : LinkedList[B] = {
    ValidNode(f(value), next.map(f))
  }

  def size: Int = 1 + next.size

  override def head: A = value

  final override def foreach(f: A => Unit): Unit = {
    f(value)
    next.foreach(f)
  }

}

object LinkedList {
  def apply[A](args: A*): LinkedList[A] =
    if (args.isEmpty) InvalidNode
    else {
      ValidNode(args.head, apply(args.tail: _*))
    }
}


object ScalaPracticeDay6 {
  def main(args : Array[String]): Unit = {
    val list = LinkedList(1, 2, 3, 4, 5)
    //list.foreach(println)
    //println(list.size)
    //println(list.head)
    //list.tail.foreach(x => println(x))
    list.filter(x=>x < 4).foreach(x => println(x))
    //list.map(x=>x*2).foreach(x => println(x))
  }
}



class Fah(var min: Double, var max: Double, var mean: Double){
  override def toString: String = {
    s"$min $max $mean"
  }
}

object OBRC {
  implicit val actorSystem: ActorSystem = ActorSystem("OBRC")
  implicit val materializer: Materializer = Materializer(actorSystem)
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global
  //ExecutionContext.fromExecutor(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(64)))


  def main(args: Array[String]): Unit = {
    //val bs : Byte = 1
    //val map =  mutable.Map[Byte, StringResult]()
    //val someValue1 = tValue(2,1,2,3)
    //val someValue2 = tValue(4,1,2,4)
    //val someValue3 = tValue(1,1,2,5)

    //map(bs) =  StringResult(map.getOrElse(bs,null),someValue1)
    //map(bs) =  StringResult(map.getOrElse(bs,null),someValue2)
    //map(bs) =  StringResult(map.getOrElse(bs,null),someValue3)

    //map(bs).foreach(println)
    //actorSystem.terminate()

    //}

  }
}


object WeatherDataProcessor {
  implicit val actorSystem: ActorSystem = ActorSystem("WeatherDataProcessor")
  implicit val materializer: Materializer = Materializer(actorSystem)
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

  def main(args: Array[String]): Unit = {
    val start = System.currentTimeMillis()
    val result = mutable.Map[String, Fah]()
    val file = new File(System.getProperty("user.dir") + "/resources/weather_stations.csv")
    val inputStream = new FileInputStream(file)

    val source = StreamConverters.fromInputStream(() => inputStream)
      .via(Framing.delimiter(ByteString('\n'), maximumFrameLength = 1024))
      .map(_.utf8String)

    val sink = Sink.foreach { line =>
      val l = line.asInstanceOf[String].split(';')
      println(l(2))

    }

    val stream = source.runWith(sink)
    stream.onComplete { _ =>
      result.foreach(println)
      val end = System.currentTimeMillis()
      println(s"Elapsed time: ${end - start} milliseconds")
      actorSystem.terminate()
    }
  }
}

