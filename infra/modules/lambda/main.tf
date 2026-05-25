resource "aws_lambda_function" "this" {
  function_name = var.function_name
  description   = "SwiftLink URL Shortener API"
  role          = var.lambda_role_arn
  package_type  = "Image"
  image_uri     = "${var.ecr_repository_url}:${var.image_tag}"
  timeout       = var.timeout
  memory_size   = var.memory_size
  architectures = ["x86_64"]

  environment {
    variables = {
      SPRING_PROFILES_ACTIVE      = "aws"
      SWIFTLINK_BASE_URL          = var.base_url
      SWIFTLINK_URL_TABLE         = var.url_table_name
      SWIFTLINK_ANALYTICS_TABLE   = var.analytics_table_name
      PORT                        = "8080"
      READINESS_CHECK_PATH        = "/actuator/health/liveness"
      REMOVE_BASE_PATH            = var.api_stage_name
    }
  }

  tracing_config {
    mode = "Active"
  }

  tags = var.tags

  lifecycle {
    ignore_changes = [image_uri]  # managed by GitHub Actions
  }
}

resource "aws_lambda_function_url" "this" {
  count              = var.enable_function_url ? 1 : 0
  function_name      = aws_lambda_function.this.function_name
  authorization_type = "NONE"

  cors {
    allow_credentials = false
    allow_origins     = ["*"]
    allow_methods     = ["*"]
    allow_headers     = ["*"]
    max_age           = 3600
  }
}

resource "aws_cloudwatch_log_group" "this" {
  name              = "/aws/lambda/${var.function_name}"
  retention_in_days = var.log_retention_days
  tags              = var.tags
}

resource "aws_cloudwatch_metric_alarm" "errors" {
  alarm_name          = "${var.function_name}-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = 60
  statistic           = "Sum"
  threshold           = var.error_alarm_threshold
  alarm_description   = "Lambda function errors"
  treat_missing_data  = "notBreaching"

  dimensions = {
    FunctionName = aws_lambda_function.this.function_name
  }

  alarm_actions = var.alarm_sns_arn != "" ? [var.alarm_sns_arn] : []
  tags          = var.tags
}

resource "aws_cloudwatch_metric_alarm" "duration" {
  alarm_name          = "${var.function_name}-duration"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 3
  metric_name         = "Duration"
  namespace           = "AWS/Lambda"
  period              = 60
  statistic           = "p95"
  threshold           = var.duration_alarm_ms
  alarm_description   = "Lambda p95 duration exceeded"
  treat_missing_data  = "notBreaching"

  dimensions = {
    FunctionName = aws_lambda_function.this.function_name
  }

  alarm_actions = var.alarm_sns_arn != "" ? [var.alarm_sns_arn] : []
  tags          = var.tags
}
