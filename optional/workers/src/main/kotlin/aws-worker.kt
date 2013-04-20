package node.workers.aws

import node.workers.Queue
import java.util.concurrent.Future
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.CreateQueueRequest
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.DeleteMessageRequest
import kotlin.concurrent.thread

/**
 * A queue to be used with the worker system that uses Amazon's SQS
 */

class SQSQueue(val sqs: AmazonSQSClient, queueName: String): Queue {
  val queueUrl = {
    sqs.createQueue(CreateQueueRequest(queueName))!!.getQueueUrl()!!
  }()
  var running = false

  override var jobHandler: (String) -> Future<Boolean> = {
    throw UnsupportedOperationException()
  }

  override fun post(data: String): Future<Boolean> {
    throw UnsupportedOperationException()
  }

  fun run() {
    running = true
    thread(name = "aws worker thread", daemon = true, block = {
      while (running) {
        val messages = sqs.receiveMessage(ReceiveMessageRequest(queueUrl).withWaitTimeSeconds(20))
        messages?.getMessages()?.forEach {
          if (jobHandler(it.getBody()!!).get()!!) {
            sqs.deleteMessage(DeleteMessageRequest())
          }
        }
      }
    })
  }
}