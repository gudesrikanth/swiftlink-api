output "state_bucket_name" {
  description = "S3 bucket name for Terraform state — copy to backend.tf files"
  value       = aws_s3_bucket.state.id
}

output "lock_table_name" {
  description = "DynamoDB table for Terraform state locking — copy to backend.tf files"
  value       = aws_dynamodb_table.tf_locks.name
}
