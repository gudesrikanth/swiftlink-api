terraform {
  backend "s3" {
    # These values are injected via -backend-config in CI/CD or by the developer.
    # Run: terraform init \
    #   -backend-config="bucket=<YOUR_STATE_BUCKET>" \
    #   -backend-config="key=swiftlink/dev/terraform.tfstate" \
    #   -backend-config="region=us-east-1" \
    #   -backend-config="dynamodb_table=<YOUR_LOCK_TABLE>"
    bucket         = "REPLACE_WITH_STATE_BUCKET"
    key            = "swiftlink/dev/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "REPLACE_WITH_LOCK_TABLE"
    encrypt        = true
  }
}
