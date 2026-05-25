terraform {
  backend "s3" {
    key     = "swiftlink/prod/terraform.tfstate"
    region  = "us-east-1"
    encrypt = true
    # bucket and dynamodb_table are injected via -backend-config at init time.
    # See infra-prod.yml, or run locally:
    #   terraform init \
    #     -backend-config="bucket=<TF_STATE_BUCKET>" \
    #     -backend-config="dynamodb_table=<TF_LOCK_TABLE>"
  }
}
