variable "function_name" {
  description = "Lambda function name"
  type        = string
}

variable "lambda_role_arn" {
  description = "IAM role ARN for Lambda execution"
  type        = string
}

variable "ecr_repository_url" {
  description = "ECR repository URL (without tag)"
  type        = string
}

variable "image_tag" {
  description = "Docker image tag to deploy"
  type        = string
  default     = "latest"
}

variable "memory_size" {
  description = "Lambda memory in MB (512–3008 recommended for Spring Boot)"
  type        = number
  default     = 512
}

variable "timeout" {
  description = "Lambda timeout in seconds"
  type        = number
  default     = 30
}

variable "base_url" {
  description = "Public base URL of the API (e.g. https://api.swiftlink.io)"
  type        = string
}

variable "url_table_name" {
  description = "DynamoDB URLs table name"
  type        = string
}

variable "analytics_table_name" {
  description = "DynamoDB analytics table name"
  type        = string
}

variable "api_stage_name" {
  description = "API Gateway stage name to strip from path (e.g. /api)"
  type        = string
  default     = ""
}

variable "log_retention_days" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 14
}

variable "enable_function_url" {
  description = "Enable Lambda function URL (alternative to API Gateway)"
  type        = bool
  default     = false
}

variable "error_alarm_threshold" {
  description = "Number of errors before alarm triggers"
  type        = number
  default     = 5
}

variable "duration_alarm_ms" {
  description = "p95 duration threshold (ms) before alarm triggers"
  type        = number
  default     = 5000
}

variable "alarm_sns_arn" {
  description = "SNS topic ARN for alarms. Leave empty to skip."
  type        = string
  default     = ""
}

variable "tags" {
  description = "Resource tags"
  type        = map(string)
  default     = {}
}
