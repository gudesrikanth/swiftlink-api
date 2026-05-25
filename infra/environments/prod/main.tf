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
  env         = "prod"
  name_prefix = "swiftlink-${local.env}"
  common_tags = {
    Project     = "SwiftLink"
    Environment = local.env
    ManagedBy   = "Terraform"
    Owner       = var.owner
  }
}

resource "aws_sns_topic" "alarms" {
  name = "${local.name_prefix}-alarms"
  tags = local.common_tags
}

resource "aws_sns_topic_subscription" "alarms_email" {
  count     = var.alarm_email != "" ? 1 : 0
  topic_arn = aws_sns_topic.alarms.arn
  protocol  = "email"
  endpoint  = var.alarm_email
}

module "ecr" {
  source          = "../../modules/ecr"
  repository_name = local.name_prefix
  tags            = local.common_tags
}

module "dynamodb" {
  source        = "../../modules/dynamodb"
  name_prefix   = local.name_prefix
  enable_pitr   = true    # always on for prod
  alarm_sns_arn = aws_sns_topic.alarms.arn
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
  memory_size          = 1024   # more memory = faster CPU = less cold start
  timeout              = 30
  base_url             = "https://${var.custom_domain}"
  url_table_name       = module.dynamodb.urls_table_name
  analytics_table_name = module.dynamodb.analytics_table_name
  log_retention_days   = 30
  error_alarm_threshold = 5
  duration_alarm_ms    = 3000
  alarm_sns_arn        = aws_sns_topic.alarms.arn
  tags                 = local.common_tags
}

module "api_gateway" {
  source               = "../../modules/api_gateway"
  api_name             = local.name_prefix
  lambda_invoke_arn    = module.lambda.invoke_arn
  lambda_function_name = module.lambda.function_name
  throttle_burst_limit = 1000
  throttle_rate_limit  = 200
  log_retention_days   = 30
  error_alarm_threshold = 10
  alarm_sns_arn        = aws_sns_topic.alarms.arn
  tags                 = local.common_tags
}
