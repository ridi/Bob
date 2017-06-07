package v1.bob

import play.api.libs.json.{JsValue, Json, Writes}

case class BobPoll(poll: Poll, candidates: Seq[Candidate], votes: Seq[Vote]) {
  val id: Long = poll.id
  val channel: String = poll.channelId
  val messageTs: String = poll.messageTs.getOrElse("")
  val isOpen: Boolean = poll.isOpen
  val candidatesOptional: Seq[Candidate] =
    if (isOpen) candidates :+ Candidate(poll.id, -1, -1, "Close", "danger") else candidates
  val method: String = if (poll.messageTs.isEmpty) "chat.postMessage" else "chat.update"
  val resultSet: Set[Vote] =
    votes
      .groupBy(identity)
      .mapValues(_.length)
      .filter(_._2 % 2 == 1)
      .keySet
  val resultByUser: Map[String, Set[Vote]] = resultSet.groupBy(_.userId)
  val resultBySelection: Map[Int, Set[Vote]] = resultSet.groupBy(_.selection)
  val candidateBobIdMap: Map[Int, Long] = candidates.groupBy(_.serialNo).mapValues(_.head.bobId)
}

object BobPoll {
  implicit val implicitWrites = new Writes[BobPoll] {
    override def writes(o: BobPoll): JsValue = {
      val buttons = Json.obj(
        "fallback" -> "Vote Failed!",
        "callback_id" -> s"bob_poll:${o.id}",
        "color" -> "#3AA3E3",
        "attachment_type" -> "default",
        "actions" -> o.candidatesOptional
      )
      val voters = {
        val votedUsers = o.resultSet.groupBy(_.selection).flatMap(_._2).map(_.userMentionStr).toSet
        val votedUserStr = if (votedUsers.isEmpty) "Nobody" else votedUsers.mkString(", ")
        Json.obj(
          "text" -> s"$votedUserStr voted."
        )
      }
      Json.toJson(
        Json.arr(buttons, voters)
      )
    }
  }
}
