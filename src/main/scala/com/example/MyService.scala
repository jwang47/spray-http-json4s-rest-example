/*
This is free and unencumbered software released into the public domain.

Anyone is free to copy, modify, publish, use, compile, sell, or
distribute this software, either in source code form or as a compiled
binary, for any purpose, commercial or non-commercial, and by any
means.

In jurisdictions that recognize copyright laws, the author or authors
of this software dedicate any and all copyright interest in the
software to the public domain. We make this dedication for the benefit
of the public at large and to the detriment of our heirs and
successors. We intend this dedication to be an overt act of
relinquishment in perpetuity of all present and future rights to this
software under copyright law.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

For more information, please refer to <http://unlicense.org>
*/
package com.example

import akka.actor.Actor
import spray.routing._
import org.json4s.Formats
import org.json4s.DefaultFormats
import spray.httpx.Json4sSupport
import scala.collection.mutable

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends Actor with MyService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}

object Json4sProtocol extends Json4sSupport {
  implicit def json4sFormats: Formats = DefaultFormats
}

object MockDb {
  val items = mutable.HashMap.empty[Long, TodoItem]
  var nextId: Long = 0

  def getNextId(): Long = {
    nextId += 1
    nextId
  }

  def create(item: TodoItem): TodoItem = {
    val newItem: TodoItem = item.copy(id = Option(nextId))
    nextId += 1
    items.put(newItem.id.get, newItem)
    newItem
  }

  def all: Iterable[TodoItem] = items.values
  def get(id: Long): Option[TodoItem] = items.get(id)
  def update(id: Long, item: TodoItem): Option[TodoItem] = {
    items.put(id, item)
    items.get(id)
  }

  def delete(id: Long): Option[TodoItem] = {
    items.remove(id)
  }
}
case class TodoItem(id: Option[Long], title: String)

// this trait defines our service behavior independently from the service actor
trait MyService extends HttpService {
  import Json4sProtocol._

  val myRoute = {
    path("todos") {
      get {
        complete {
          MockDb.all
        }
      }
    } ~
    path("todos") {
      post {
        entity(as[TodoItem]) { todoItem: TodoItem =>
          complete {
            MockDb.create(todoItem)
          }
        }
      }
    } ~
    path("todos" / Segment) { id =>
      get {
        complete {
          MockDb.get(id.toLong)
        }
      }
    } ~
    path("todos" / Segment) { id =>
      put {
        entity(as[TodoItem]) { todoItem: TodoItem =>
          complete {
            MockDb.update(id.toLong, todoItem)
          }
        }
      }
    }
  }
}