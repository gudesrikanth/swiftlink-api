output "api_endpoint" {
  description = "API Gateway URL"
  value       = module.api_gateway.api_endpoint
}

output "ecr_repository_url" {
  description = "ECR repository URL"
  value       = module.ecr.repository_url
}

output "lambda_function_name" {
  description = "Lambda function name"
  value       = module.lambda.function_name
}
