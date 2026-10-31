variable "aws_region" {
  type    = string
  default = "eu-central-1"
}

variable "db_password" {
  description = "PostgreSQL RDS master password"
  type        = string
  sensitive   = true
}

variable "ssh_public_key_path" {
  description = "Path to the SSH public key"
  type        = string
}

variable "allowed_ssh_cidr" {
  description = "Your public IP address in CIDR notation"
  type        = string
}

variable "instance_type" {
  type    = string
  default = "t3.small"
}