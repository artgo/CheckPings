import scala.util.matching.Regex
import scala.sys.process._
import scala.collection.JavaConverters._
import java.util.concurrent.CountDownLatch
import scala.collection.mutable.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap
import scala.actors.Actor._
import java.net.URL
import scala.collection.mutable.HashMap

  println("Init")

  val timeStart = System.nanoTime: Double
  val urlListStr = """http://apache.osuosl.org
http://apache.petsads.us
http://mirror.cogentco.com
http://www.eng.lsu.edu
http://apache.tradebit.com
http://www.gtlib.gatech.edu
http://mirror.cc.columbia.edu
http://www.motorlogy.com
http://apache.mirrors.lucidnetworks.net
http://mirror.symnds.com
http://mirror.metrocast.net
http://apache.ziply.com
http://www.alliedquotes.com
http://www.trieuvan.com
http://www.bizdirusa.com
http://www.carfab.com
http://mirrors.sonic.net
http://www.fightrice.com
http://mirror.sdunix.com
http://www.globalish.com
http://mirrors.ibiblio.org
http://apache.mesi.com.ar
http://mirror.olnevhost.net
http://apache.spinellicreations.com
http://apache.cs.utah.edu
http://download.nextag.com
http://apache.mirrors.hoobly.com
http://www.reverse.net
http://apache.mirrors.tds.net
http://apache.mirrors.pair.com
http://mirror.nexcess.net
http://apache.claz.org
http://mirrors.gigenet.com
ftp://apache.mirrors.pair.com
ftp://linux-files.com
ftp://apache.mirrors.tds.net
ftp://apache.cs.utah.edu
ftp://apache.mirrorcatalogs.com
ftp://ftp.osuosl.org
http://www.us.apache.org
http://www.eu.apache.org"""

  val allUrls = urlListStr.trim().split("\\s+")
  val hostToUrl = HashMap[String, String]()
  allUrls.foreach(urlStr => {
    if (urlStr != None) {
      val hostName = new URL(urlStr).getHost();
      // FTP should be more efficient and robust
      if (urlStr.toLowerCase().startsWith("ftp") || !hostToUrl.contains(hostName)) {
        hostToUrl.put(hostName, urlStr)
      }
    }
  })
  val hostsList = hostToUrl.keySet
  val pingMap: ConcurrentMap[String, Double] = new ConcurrentHashMap[String, Double]().asScala
  // WIN: val averageMsPattern = new Regex("""Average =\s+(\d+)ms""", "ms");
  // format rtt min/avg/max/mdev = 86.038/86.164/86.267/0.353 ms
  val averageMsPattern = new Regex("""rtt\s+min\/avg\/max\/mdev\s+=\s+[\d\.]+\/([\d\.]+)\/[\d\.]+\/[\d\.]+\s+ms""", "ms");

  val allFinishedLatch = new CountDownLatch(hostsList.size)
  print("Started")

  for (host <- hostsList) {
    actor {
      val fullText = Seq("ping", "-c", "10", "-i", "1", "-W", "30", host).lines_!.mkString(" ")
      val firstResult = averageMsPattern.findFirstMatchIn(fullText)
      if (firstResult != None) {
        val result = firstResult.get
        val pingMs = result.group("ms").toDouble
        pingMap.put(host, pingMs)
      }
      print(".")
      allFinishedLatch.countDown()
    }
  }

  allFinishedLatch.await()
  println("\n\nBest times:");
  val best = pingMap.toList.sortBy(t => t._2).slice(0, 5)
  best.foreach(t => println(t._2 + "\t" + hostToUrl.get(t._1).mkString))
  val timeEnd = System.nanoTime: Double
  println("\nElapsed time " + (timeEnd - timeStart) / 1000000000.0 + " secs")
  