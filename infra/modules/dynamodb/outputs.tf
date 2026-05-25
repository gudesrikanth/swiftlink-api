output "urls_table_name" {
  description = "Name of the URLs DynamoDB table"
  value       = aws_dynamodb_table.urls.name
}

output "urls_table_arn" {
  description = "ARN of the URLs DynamoDB table"
  value       = aws_dynamodb_table.urls.arn
}

output "analytics_table_name" {
  description = "Name of the analytics DynamoDB table"
  value       = aws_dynamodb_table.analytics.name
}

output "analytics_table_arn" {
  description = "ARN of the analytics DynamoDB table"
  value       = aws_dynamodb_table.analytics.arn
}
