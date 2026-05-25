variable "name_prefix" {
  description = "Prefix for IAM resource names"
  type        = string
}

variable "dynamodb_table_arns" {
  description = "List of DynamoDB table ARNs to grant access"
  type        = list(string)
}

variable "ecr_repository_arn" {
  description = "ECR repository ARN"
  type        = string
}

variable "tags" {
  description = "Resource tags"
  type        = map(string)
  default     = {}
}
