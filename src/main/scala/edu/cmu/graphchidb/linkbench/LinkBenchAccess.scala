package edu.cmu.graphchidb.linkbench

import edu.cmu.graphchidb.GraphChiDatabase
import edu.cmu.graphchidb.queries.Queries
import edu.cmu.graphchidb.queries.internal.{SimpleSetReceiver, SimpleArrayReceiver}
import java.io.{FileWriter, BufferedWriter}
import edu.cmu.graphchi.queries.QueryCallback
import java.{lang, util}

/**
 * For console use
 * @author Aapo Kyrola
 */
object LinkBenchAccess {

  /*
    import edu.cmu.graphchidb.linkbench.LinkBenchAccess._
    DB.initialize()
            DB.shardTree.map( shs => (shs.size, shs.map(_.numEdges).sum) )

   */

  val baseFilename = "/Users/akyrola/graphs/DB/linkbench/linkbench"
  val DB = new GraphChiDatabase(baseFilename, disableDegree = true)

  DB.initialize()

  def inAndOutTest(): Unit = {
    DB.flushAllBuffers()
    val r = new java.util.Random(260379)
    var i = 1

    val outlog = new BufferedWriter(new FileWriter("out_linkbench.tsv"))
    val inlog = new BufferedWriter(new FileWriter("in_linkbench.tsv"))

    while(i <= 50000) {
      val v = DB.originalToInternalId(math.abs(r.nextLong() % 100000000))
      val tInSt = System.nanoTime()
      val inRecv = new SimpleSetReceiver(outEdges = false)
      DB.queryIn(v, 0, inRecv)
      val tIn = System.nanoTime() - tInSt

      val outRecv = new SimpleSetReceiver(outEdges = true)
      DB.queryOut(v, 0, outRecv)
      val tOut = System.nanoTime() - tInSt

      outlog.write("%d,%f\n".format(outRecv.set.size, tOut * 0.001))
      inlog.write("%d,%f\n".format(inRecv.set.size, tIn * 0.001))
      i += 1
      if (i%1000 == 0) println("%d/%d".format(i, 50000))
    }
    inlog.close()
    outlog.close()
  }



  class BitSetOrigIdReceiver(outEdges: Boolean) extends QueryCallback {
    val bitset = new util.BitSet()
    def immediateReceive() = true
    def receiveEdge(src: Long, dst: Long, edgeType: Byte, dataPtr: Long) = {
      if (outEdges)   bitset.set(DB.internalToOriginalId(dst).toInt)
      else bitset.set(DB.internalToOriginalId(src).toInt)
    }

    def receiveInNeighbors(vertexId: Long, neighborIds: util.ArrayList[lang.Long], edgeTypes: util.ArrayList[lang.Byte], dataPointers: util.ArrayList[lang.Long])= throw new IllegalStateException()
    def receiveOutNeighbors(vertexId: Long, neighborIds: util.ArrayList[lang.Long], edgeTypes: util.ArrayList[lang.Byte], dataPointers: util.ArrayList[lang.Long])= throw new IllegalStateException()

  }
  def fofTest(): Unit = {
    var i = 1
    val t = System.currentTimeMillis()
    val r = new java.util.Random(260379)
    val foflog = new BufferedWriter(new FileWriter("fof_linkbench.tsv"))

    while(i < 50000) {
      val v = math.abs(r.nextLong() % 9900000) + 1
      val st = System.nanoTime()
      val friendReceiver = new SimpleArrayReceiver(outEdges = true)
      DB.queryOut(DB.originalToInternalId(v), 0, friendReceiver)
      val a = new BitSetOrigIdReceiver(outEdges = true)
      DB.queryOutMultiple(friendReceiver.arr, 0.toByte, a)
      val tFof = System.nanoTime() - st
      val cnt = a.bitset.size
      if (i % 1000 == 0 && cnt >= 0) {
        printf("%d %d fof:%d\n".format(System.currentTimeMillis() - t, i, cnt))
      }
      i += 1
      if (cnt > 0)
          foflog.write("%d,%f\n".format(cnt, tFof * 0.001))

    }

    foflog.close()

  }
}
