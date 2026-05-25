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

  default_tags {
    tags = local.common_tags
  }
}

locals {
  env         = "dev"
  name_prefix = "swiftlink-${local.env}"
  common_tags = {
    Project     = "SwiftLink"
    Environment = local.env
    ManagedBy   = "Terraform"
    Owner       = var.owner
  }
}

import {
  to = module.ecr.aws_ecr_repository.this
  id = "swiftlink-dev"
}

module "ecr" {
  source          = "../../modules/ecr"
  repository_name = local.name_prefix
  tags            = local.common_tags
}

module "dynamodb" {
  source        = "../../modules/dynamodb"
  name_prefix   = local.name_prefix
  enable_pitr   = false    # cost-saving for dev
  alarm_sns_arn = ""
  tags          = local.common_tags
}

module "iam" {
  source             = "../../modules/iam"
  name_prefix        = local.name_prefix
  ecr_repository_arn = module.ecr.repository_arn
  dynamodb_table_arns = [
    module.dynamodb.urls_table_arn,
    module.dynamodb.analytics_table_arn
  ]
  tags = local.common_tags
}

module "lambda" {
  source               = "../../modules/lambda"
  function_name        = local.name_prefix
  lambda_role_arn      = module.iam.lambda_role_arn
  ecr_repository_url   = module.ecr.repository_url
  image_tag            = var.image_tag
  memory_size          = 1024
  timeout              = 60
  base_url             = module.api_gateway.api_endpoint
  url_table_name       = module.dynamodb.urls_table_name
  analytics_table_name = module.dynamodb.analytics_table_name
  log_retention_days   = 7
  alarm_sns_arn        = ""
  tags                 = local.common_tags
}

module "api_gateway" {
  source               = "../../modules/api_gateway"
  api_name             = local.name_prefix
  lambda_invoke_arn    = module.lambda.invoke_arn
  lambda_function_name = module.lambda.function_name
  throttle_burst_limit = 200
  throttle_rate_limit  = 50
  log_retention_days   = 7
  alarm_sns_arn        = ""
  tags                 = local.common_tags
}
