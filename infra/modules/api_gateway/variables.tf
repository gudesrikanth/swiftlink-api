variable "api_name" {
  description = "API Gateway name"
  type        = string
}

variable "stage_name" {
  description = "API Gateway stage name (e.g. dev, prod)"
  type        = string
  default     = "$default"
}

variable "lambda_invoke_arn" {
  description = "Lambda invoke ARN"
  type        = string
}

variable "lambda_function_name" {
  description = "Lambda function name (for permission)"
  type        = string
}

variable "cors_allow_origins" {
  description = "List of allowed CORS origins"
  type        = list(string)
  default     = ["*"]
}

variable "throttle_burst_limit" {
  description = "API Gateway burst throttle limit (requests)"
  type        = number
  default     = 500
}

variable "throttle_rate_limit" {
  description = "API Gateway steady-state throttle rate (requests/second)"
  type        = number
  default     = 100
}

variable "log_retention_days" {
  description = "CloudWatch log retention in days"
  type        = number
  default     = 14
}

variable "error_alarm_threshold" {
  description = "5XX error count before alarm triggers"
  type        = number
  default     = 10
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
