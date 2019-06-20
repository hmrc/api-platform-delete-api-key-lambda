package uk.gov.hmrc.apiplatform.deleteapikey

import java.util.UUID

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest._
import org.scalatest.mockito.MockitoSugar
import software.amazon.awssdk.services.apigateway.ApiGatewayClient
import software.amazon.awssdk.services.apigateway.model.{ApiKey, _}
import uk.gov.hmrc.aws_gateway_proxied_request_lambda.JsonMapper

import scala.collection.JavaConversions.seqAsJavaList

class DeleteApiKeyHandlerSpec extends WordSpecLike with Matchers with MockitoSugar with JsonMapper {

  trait Setup {
    val apiKeyId: String = UUID.randomUUID().toString
    val apiKeyName = "API_KEY"

    val requestBody = s"""{"apiKeyName": "$apiKeyName"}"""
    val message = new SQSMessage()
    message.setBody(requestBody)
    val sqsEvent = new SQSEvent()
    sqsEvent.setRecords(List(message))

    val mockAPIGatewayClient: ApiGatewayClient = mock[ApiGatewayClient]
    val deleteApiKeyHandler = new DeleteApiKeyHandler(mockAPIGatewayClient)
    val mockContext = mock[Context]
    when(mockContext.getLogger).thenReturn(mock[LambdaLogger])
  }

  "Delete API Key Handler" should {
    "delete the API Key from API Gateway when found" in new Setup {
      when(mockAPIGatewayClient.getApiKeys(any[GetApiKeysRequest])).thenReturn(buildMatchingGetApiKeysResponse(apiKeyId, apiKeyName))
      val deleteRequestCaptor: ArgumentCaptor[DeleteApiKeyRequest] = ArgumentCaptor.forClass(classOf[DeleteApiKeyRequest])
      when(mockAPIGatewayClient.deleteApiKey(deleteRequestCaptor.capture())).thenReturn(DeleteApiKeyResponse.builder().build())

      deleteApiKeyHandler.handleInput(sqsEvent, mockContext)

      deleteRequestCaptor.getValue.apiKey() shouldEqual apiKeyId
    }

    "not do anything when API Key is not found" in new Setup {
      when(mockAPIGatewayClient.getApiKeys(any[GetApiKeysRequest])).thenReturn(buildNonMatchingGetApiKeysResponse(1))

      deleteApiKeyHandler.handleInput(sqsEvent, mockContext)

      verify(mockAPIGatewayClient, times(0)).deleteApiKey(any[DeleteApiKeyRequest])
    }

    "throw an Exception if multiple messages have been retrieved from SQS" in new Setup {
      sqsEvent.setRecords(List(message, message))

      val exception = intercept[IllegalArgumentException](deleteApiKeyHandler.handleInput(sqsEvent, mockContext))

      exception.getMessage shouldEqual "Invalid number of records: 2"
    }

    "throw an Exception if no messages have been retrieved from SQS" in new Setup {
      sqsEvent.setRecords(List())

      val exception = intercept[IllegalArgumentException](deleteApiKeyHandler.handleInput(sqsEvent, mockContext))

      exception.getMessage shouldEqual "Invalid number of records: 0"
    }

    "propagate any exceptions thrown by SDK" in new Setup {
      when(mockAPIGatewayClient.getApiKeys(any[GetApiKeysRequest])).thenReturn(buildMatchingGetApiKeysResponse(apiKeyId, apiKeyName))

      val errorMessage = "You're an idiot"
      when(mockAPIGatewayClient.deleteApiKey(any[DeleteApiKeyRequest])).thenThrow(UnauthorizedException.builder().message(errorMessage).build())

      val exception = intercept[UnauthorizedException](deleteApiKeyHandler.handleInput(sqsEvent, mockContext))

      exception.getMessage shouldEqual errorMessage
    }
  }

  def buildMatchingGetApiKeysResponse(matchingId: String, matchingName: String): GetApiKeysResponse = {
    GetApiKeysResponse.builder()
      .items(ApiKey.builder().id(matchingId).name(matchingName).build())
      .build()
  }

  def buildNonMatchingGetApiKeysResponse(count: Int): GetApiKeysResponse = {
    val items: Seq[ApiKey] = (1 to count).map(c => ApiKey.builder().id(s"$c").name(s"Item $c").build())

    GetApiKeysResponse.builder()
      .items(seqAsJavaList(items))
      .build()
  }
}
