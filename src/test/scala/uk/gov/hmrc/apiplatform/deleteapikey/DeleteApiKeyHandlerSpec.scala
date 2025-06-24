package uk.gov.hmrc.apiplatform.deleteapikey

import java.util.UUID

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.mockito.scalatest.MockitoSugar
import software.amazon.awssdk.services.apigateway.ApiGatewayClient
import software.amazon.awssdk.services.apigateway.model.{ApiKey, _}
import uk.gov.hmrc.api_platform_manage_api.utils.JsonMapper

import scala.collection.JavaConverters._

class DeleteApiKeyHandlerSpec extends AnyWordSpecLike with Matchers with MockitoSugar with JsonMapper {

  trait Setup {
    val apiKeyId: String = UUID.randomUUID().toString
    val apiKeyName = "API_KEY"

    val requestBody = s"""{"apiKeyName": "$apiKeyName"}"""
    val message = new SQSMessage()
    message.setBody(requestBody)
    val sqsEvent = new SQSEvent()
    sqsEvent.setRecords(List(message).asJava)

    val mockAPIGatewayClient: ApiGatewayClient = mock[ApiGatewayClient]
    val deleteApiKeyHandler = new DeleteApiKeyHandler(mockAPIGatewayClient)
    val mockContext = mock[Context]
    val mockLogger = mock[LambdaLogger]
    when(mockContext.getLogger).thenReturn(mockLogger)
  }

  "Delete API Key Handler" should {
    "delete the API Key from API Gateway when found" in new Setup {
      when(mockAPIGatewayClient.getApiKeys(any[GetApiKeysRequest])).thenReturn(buildMatchingGetApiKeysResponse(apiKeyId, apiKeyName))
      when(mockAPIGatewayClient.deleteApiKey(any[DeleteApiKeyRequest])).thenReturn(DeleteApiKeyResponse.builder().build())
      
      deleteApiKeyHandler.handleInput(sqsEvent, mockContext)


      val deleteRequestCaptor: ArgumentCaptor[DeleteApiKeyRequest] = ArgumentCaptor.forClass(classOf[DeleteApiKeyRequest])
      verify(mockAPIGatewayClient).deleteApiKey(deleteRequestCaptor.capture())
      
      deleteRequestCaptor.getValue.apiKey() shouldEqual apiKeyId
    }

    "not do anything when API Key is not found" in new Setup {
      when(mockAPIGatewayClient.getApiKeys(any[GetApiKeysRequest])).thenReturn(GetApiKeysResponse.builder().build())

      deleteApiKeyHandler.handleInput(sqsEvent, mockContext)

      verify(mockAPIGatewayClient, times(0)).deleteApiKey(any[DeleteApiKeyRequest])
      verify(mockLogger, times(1)).log(s"API Key with name $apiKeyName not found")
    }

    "throw an Exception if multiple messages have been retrieved from SQS" in new Setup {
      sqsEvent.setRecords(List(message, message).asJava)

      val exception = intercept[IllegalArgumentException](deleteApiKeyHandler.handleInput(sqsEvent, mockContext))

      exception.getMessage shouldEqual "Invalid number of records: 2"
    }

    "throw an Exception if no messages have been retrieved from SQS" in new Setup {
      sqsEvent.setRecords(List.empty.asJava)

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

}
