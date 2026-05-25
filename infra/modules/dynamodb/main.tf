resource "aws_dynamodb_table" "urls" {
  name         = "${var.name_prefix}-urls"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "shortCode"

  attribute {
    name = "shortCode"
    type = "S"
  }

  ttl {
    attribute_name = "ttlEpoch"
    enabled        = true
  }

  point_in_time_recovery {
    enabled = var.enable_pitr
  }

  server_side_encryption {
    enabled = true
  }

  tags = var.tags
}

resource "aws_dynamodb_table" "analytics" {
  name         = "${var.name_prefix}-analytics"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "shortCode"
  range_key    = "sortKey"

  attribute {
    name = "shortCode"
    type = "S"
  }

  attribute {
    name = "sortKey"
    type = "S"
  }

  ttl {
    attribute_name = "ttlEpoch"
    enabled        = true
  }

  point_in_time_recovery {
    enabled = var.enable_pitr
  }

  server_side_encryption {
    enabled = true
  }

  tags = var.tags
}

resource "aws_cloudwatch_metric_alarm" "urls_table_throttle" {
  alarm_name          = "${var.name_prefix}-urls-throttle"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "SystemErrors"
  namespace           = "AWS/DynamoDB"
  period              = 60
  statistic           = "Sum"
  threshold           = 0
  alarm_description   = "DynamoDB system errors on URLs table"

  dimensions = {
    TableName = aws_dynamodb_table.urls.name
  }

  alarm_actions = var.alarm_sns_arn != "" ? [var.alarm_sns_arn] : []
  tags          = var.tags
}
