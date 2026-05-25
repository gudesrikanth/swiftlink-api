variable "name_prefix" {
  description = "Prefix for all DynamoDB table names (e.g. swiftlink-dev)"
  type        = string
}

variable "enable_pitr" {
  description = "Enable Point-In-Time Recovery"
  type        = bool
  default     = false
}

variable "alarm_sns_arn" {
  description = "SNS topic ARN for CloudWatch alarms. Leave empty to skip."
  type        = string
  default     = ""
}

variable "tags" {
  description = "Resource tags"
  type        = map(string)
  default     = {}
}
