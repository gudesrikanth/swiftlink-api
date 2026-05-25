variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project name used as a prefix for all bootstrap resources"
  type        = string
  default     = "swiftlink"
}
