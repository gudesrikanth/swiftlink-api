terraform {
  backend "s3" {
    # All values are injected at `terraform init` time via -backend-config flags.
    # See infra-dev.yml workflow, or run locally:
    #
    #   terraform init \
    #     -backend-config="bucket=<TF_STATE_BUCKET>" \
    #     -backend-config="key=swiftlink/dev/terraform.tfstate" \
    #     -backend-config="region=us-east-1" \
    #     -backend-config="dynamodb_table=<TF_LOCK_TABLE>"
  }
}
