
# api-platform-delete-api-key-lambda

Lambda function to delete an AWS API Gateway API Key.

The `event` for the Lambda function is an SQS message, the body of the SQS message is JSON. For example:
```
{
    "apiKeyName": "AN_API_KEY_NAME"
}
```


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
