package node.workers.aws

import node.workers.Queue
import java.util.concurrent.Future
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.DeleteMessageRequest
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.amazonaws.services.sqs.model.CreateQueueRequest
import kotlin.concurrent.thread
import java.util.concurrent.Executors
import kotlin.concurrent.*
import node.util._logger

/**
 * A queue to be used with the worker system that uses Amazon's SQS
 */

class SQSQueue(val sqs: AmazonSQSClient, queueName: String): Queue {
  val sender = Executors.newCachedThreadPool()
  val queueUrl = {
    sqs.createQueue(CreateQueueRequest(queueName))!!.getQueueUrl()!!
  }()
  var running = false

  override var jobHandler: (String) -> Future<Boolean> = {
    throw UnsupportedOperationException()
  }

  override fun post(data: String): Future<Boolean> {
    return sender.submit {
      sqs.sendMessage(SendMessageRequest(queueUrl, data))
      true
    }
  }

  fun run() {
    running = true
    thread(name = "aws worker thread", daemon = true, block = {
      this._logger.info("Starting worker for $queueUrl")
      while (running) {
        val messages = sqs.receiveMessage(ReceiveMessageRequest(queueUrl).withWaitTimeSeconds(20))
        messages?.getMessages()?.forEach {
          if (jobHandler(it.getBody()!!).get()!!) {
            sqs.deleteMessage(DeleteMessageRequest(queueUrl, it.getReceiptHandle()))
          }
        }
      }
    })
  }
}