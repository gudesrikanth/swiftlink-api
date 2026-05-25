output "repository_url" {
  description = "The full ECR repository URL"
  value       = aws_ecr_repository.this.repository_url
}

output "repository_arn" {
  description = "The ARN of the ECR repository"
  value       = aws_ecr_repository.this.arn
}

output "registry_id" {
  description = "The AWS account ID associated with the ECR registry"
  value       = aws_ecr_repository.this.registry_id
}
