provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "Linky"
      Environment = "demo"
      ManagedBy   = "Terraform"
    }
  }
}