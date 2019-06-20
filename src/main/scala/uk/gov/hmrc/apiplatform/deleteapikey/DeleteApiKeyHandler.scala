package uk.gov.hmrc.apiplatform.deleteapikey

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import software.amazon.awssdk.services.apigateway.ApiGatewayClient
import software.amazon.awssdk.services.apigateway.model._
import uk.gov.hmrc.api_platform_manage_api.AwsApiGatewayClient.awsApiGatewayClient
import uk.gov.hmrc.api_platform_manage_api.AwsIdRetriever
import uk.gov.hmrc.aws_gateway_proxied_request_lambda.{JsonMapper, SqsHandler}

import scala.language.postfixOps

class DeleteApiKeyHandler(override val apiGatewayClient: ApiGatewayClient) extends SqsHandler with AwsIdRetriever with JsonMapper {

  def this() {
    this(awsApiGatewayClient)
  }

  override def handleInput(event: SQSEvent, context: Context): Unit = {
    val logger = context.getLogger

    if (event.getRecords.size != 1) {
      throw new IllegalArgumentException(s"Invalid number of records: ${event.getRecords.size}")
    }

    val messageBody = fromJson[Body](event.getRecords.get(0).getBody)
    getAwsApiKeyByKeyName(messageBody.apiKeyName) match {
      case Some(apiKeyId) => apiGatewayClient.deleteApiKey(DeleteApiKeyRequest.builder().apiKey(apiKeyId.id()).build())
      case None => logger.log(s"API Key with name ${messageBody.apiKeyName} not found")
    }
  }
}

case class Body(apiKeyName: String)

