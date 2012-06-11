import scala.util.matching.Regex
import scala.sys.process._
import scala.collection.JavaConverters._
import java.util.concurrent.CountDownLatch
import scala.collection.mutable.ConcurrentMap
import java.util.concurrent.ConcurrentHashMap
import scala.actors.Actor._
import java.net.URL
import scala.collection.mutable.HashMap

object CheckPings extends App {
  val timeStart = System.nanoTime: Double
  val urlListStr = """http://mirrors.163.com
http://box-soft.com
http://cygwin.mirrors.hoobly.com
ftp://cygwin.mirrorcatalogs.com
http://cygwin.mirrorcatalogs.com
http://www.netgull.com
ftp://cygwin.mirrors.pair.com
http://cygwin.parentingamerica.com
http://servingzone.com
http://cygwin.skazkaforyou.com
http://mirror.symnds.com
http://tweedo.com
ftp://mirrors.xmission.com
http://mirrors.xmission.com
ftp://lug.mtu.edu
http://lug.mtu.edu
ftp://mirror.cs.vt.edu
http://mirror.cs.vt.edu
ftp://mirror.mcs.anl.gov
http://mirror.mcs.anl.gov
ftp://mirror.internode.on.net
http://mirror.internode.on.net
http://piotrkosoft.net
ftp://ftp.iasi.roedu.net
ftp://mirror.steadfast.net
http://mirror.steadfast.net
ftp://mirrors.syringanetworks.net
http://mirrors.syringanetworks.net
ftp://sourceware.mirrors.tds.net
http://sourceware.mirrors.tds.net
http://cygwin.vc.ukrtel.net
ftp://artfiles.org
http://artfiles.org
http://cygwin.cybermirror.org
ftp://mirrors.dotsrc.org
http://mirrors.dotsrc.org
ftp://ftp.fedoramd.org
http://repo.fedoramd.org
ftp://mirrors.kernel.org
http://mirrors.kernel.org
ftp://ftp.mirrorservice.org
http://www.mirrorservice.org
ftp://mirror.team-cymru.org
http://mirror.team-cymru.org
ftp://ftp.univie.ac.at
http://ftp.univie.ac.at
ftp://mirror.aarnet.edu.au
http://mirror.aarnet.edu.au
ftp://ftp.easynet.be
http://mirror.cpsc.ucalgary.ca
ftp://mirror.csclub.uwaterloo.ca
http://mirror.csclub.uwaterloo.ca
ftp://mirror.switch.ch
http://mirrors.ustc.edu.cn
ftp://ftp.gwdg.de
http://ftp.gwdg.de
ftp://ftp-stud.hs-esslingen.de
http://ftp-stud.hs-esslingen.de
ftp://linux.rz.ruhr-uni-bochum.de
http://linux.rz.ruhr-uni-bochum.de
ftp://ftp.inf.tu-dresden.de
http://ftp.inf.tu-dresden.de
ftp://ftp.uni-kl.de
http://ftp.uni-kl.de
ftp://ftp.rediris.es
http://ftp.rediris.es
http://cygwin.cict.fr
ftp://mirror.cict.fr
ftp://ftp.ntua.gr
http://ftp.cc.uoc.gr
ftp://ftp.fsn.hu
http://ftp.fsn.hu
ftp://ftp.heanet.ie
http://ftp.heanet.ie
http://mirror.isoc.org.il
ftp://ftp.iitm.ac.in
http://ftp.iitm.ac.in
ftp://ftp.jaist.ac.jp
http://ftp.jaist.ac.jp
ftp://ftp.yz.yamagata-u.ac.jp
http://ftp.yz.yamagata-u.ac.jp
ftp://ftp.iij.ad.jp
http://ftp.iij.ad.jp
http://cygwin.xl-mirror.nl
ftp://cygwin.uib.no
http://cygwin.uib.no
ftp://ucmirror.canterbury.ac.nz
http://ucmirror.canterbury.ac.nz
ftp://ftp.eq.uc.pt
http://ftp.eq.uc.pt
ftp://mirrors.fe.up.pt
http://mirrors.fe.up.pt
ftp://ftp.sunet.se
http://mirrors.mojhosting.sk
http://cygwin.petsads.us"""

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
  val pingMap: ConcurrentMap[String, Int] = new ConcurrentHashMap[String, Int]().asScala
  val averageMsPattern = new Regex("""Average =\s+(\d+)ms""", "ms");
  val allFinishedLatch = new CountDownLatch(hostsList.size)
  print("Started")

  for (host <- hostsList) {
    actor {
      val fullText = Seq("ping", host).lines_!.mkString(" ")
      val firstResult = averageMsPattern.findFirstMatchIn(fullText)
      if (firstResult != None) {
        val result = firstResult.get
        val pingMs = result.group("ms").toInt
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
}