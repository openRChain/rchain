package coop.rchain.comm

import java.net.InetSocketAddress
import com.netaporter.uri.Uri
import scala.util.control.NonFatal

// TODO: Add Show instance
final case class NodeIdentifier(key: Seq[Byte]) {
  override def toString: String = key.map("%02x".format(_)).mkString
}

object NodeIdentifier {
  def apply(name: String): NodeIdentifier =
    NodeIdentifier(name.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte))
}

// TODO: Add Show instance
final case class Endpoint(host: String, tcpPort: Int, udpPort: Int) {
  val tcpSocketAddress = new InetSocketAddress(host, tcpPort)
  val udpSocketAddress = new InetSocketAddress(host, udpPort)
}

// TODO: Add Show instance
final case class PeerNode(id: NodeIdentifier, endpoint: Endpoint) {

  def key  = id.key
  val sKey = id.toString

  override def toString =
    s"#{PeerNode $sKey}"

  def toAddress: String =
    s"rnode://$sKey@${endpoint.host}:${endpoint.udpPort}"

}

/*
 * Inspiration from ethereum:
 *
 *   enode://92c9722c6cd441b9d64cc3f365cdd6cda822c7496b8131078a7362b576526092b58531aa2e5914c4e87e0e574bca4497e68f35a7b66c153ee73b6c7dfc958a34@10.0.0.3:30303
 *
 * =>
 *
 *   rnode://<key>@<host>:<udp-port>
 */
object PeerNode {

  final case class NetworkAddress(scheme: String, key: String, host: String, port: Int)

  def parse(str: String): Either[CommError, PeerNode] =
    // TODO replace try-catch with Try
    try {
      val uri = Uri.parse(str)

      val addy =
        for {
          scheme <- uri.scheme
          key    <- uri.user
          host   <- uri.host
          port   <- uri.port
        } yield NetworkAddress(scheme, key, host, port)

      addy match {
        case Some(NetworkAddress(_, key, host, port)) =>
          Right(PeerNode(NodeIdentifier(key), Endpoint(host, port, port)))
        case _ => Left(ParseError(s"bad address: $str"))
      }
    } catch {
      case NonFatal(_) => Left(ParseError(s"bad address: $str"))
    }
}
