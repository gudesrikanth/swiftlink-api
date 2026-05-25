/**
 * Bootstrap — run ONCE manually before any other Terraform.
 * Creates the S3 bucket and DynamoDB table used as the Terraform remote backend.
 *
 * Usage:
 *   cd infra/bootstrap
 *   terraform init
 *   terraform apply -var="aws_region=us-east-1" -var="project_name=swiftlink"
 *
 * Then copy the outputs into the backend.tf files in each environment.
 */

terraform {
  required_version = ">= 1.9.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.80"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

data "aws_caller_identity" "current" {}

locals {
  state_bucket_name = "${var.project_name}-tf-state-${data.aws_caller_identity.current.account_id}"
  lock_table_name   = "${var.project_name}-tf-locks"
}

resource "aws_s3_bucket" "state" {
  bucket        = local.state_bucket_name
  force_destroy = false

  tags = {
    Name      = local.state_bucket_name
    Project   = var.project_name
    ManagedBy = "Terraform Bootstrap"
  }
}

resource "aws_s3_bucket_versioning" "state" {
  bucket = aws_s3_bucket.state.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "state" {
  bucket = aws_s3_bucket.state.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "state" {
  bucket                  = aws_s3_bucket.state.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_dynamodb_table" "tf_locks" {
  name         = local.lock_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  tags = {
    Name      = local.lock_table_name
    Project   = var.project_name
    ManagedBy = "Terraform Bootstrap"
  }
}
