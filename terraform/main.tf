terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

resource "aws_dynamodb_table" "workflow_state" {
  name         = var.table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "requestNumber"
  range_key    = "taskNumber"

  attribute {
    name = "requestNumber"
    type = "S"
  }

  attribute {
    name = "taskNumber"
    type = "S"
  }

  attribute {
    name = "loanNumber"
    type = "S"
  }

  attribute {
    name = "createdAt"
    type = "S"
  }

  global_secondary_index {
    name            = "loanNumber-index"
    hash_key        = "loanNumber"
    range_key       = "createdAt"
    projection_type = "ALL"
  }

  point_in_time_recovery {
    enabled = true
  }

  server_side_encryption {
    enabled = true
  }

  ttl {
    attribute_name = "ttl"
    enabled        = true
  }

  tags = {
    Project     = "ldc-workflow"
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}
