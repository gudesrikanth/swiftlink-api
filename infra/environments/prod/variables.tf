variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "owner" {
  description = "Team or person owning these resources"
  type        = string
  default     = "platform"
}

variable "image_tag" {
  description = "Docker image tag to deploy"
  type        = string
}

variable "custom_domain" {
  description = "Custom domain for the API (e.g. api.swiftlink.io)"
  type        = string
  default     = ""
}

variable "alarm_email" {
  description = "Email for CloudWatch alarm notifications"
  type        = string
  default     = ""
}
