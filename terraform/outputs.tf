data "aws_caller_identity" "current" {}

output "confirmed_account_id" {
  value = data.aws_caller_identity.current.account_id
}

output "ec2_public_ip" {
  value = aws_instance.ec2_instance.public_ip
}

output "ec2_public_dns" {
  value = aws_instance.ec2_instance.public_dns
}

output "rds_endpoint" {
  value = aws_db_instance.linky_db.endpoint
}

output "rds_address" {
  value = aws_db_instance.linky_db.address
}