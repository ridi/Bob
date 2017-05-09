package v1

import play.api.i18n.Messages

/**
  * Package object for post.  This is a good place to put implicit conversions.
  */
package object bob {

  /**
    * Converts between PostRequest and Messages automatically.
    */
  implicit def requestToMessages[A](implicit r: BobRequest[A]): Messages = {
    r.messages
  }
}
