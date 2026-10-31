resource "aws_security_group" "ec2_sg" {
  name        = "linky-ec2-sg"
  description = "Linky EC2 security group"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "SSH from trusted IP"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ssh_cidr]
  }

  egress {
    description = "Outbound connections"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "linky-ec2-sg"
  }
}

resource "aws_security_group" "rds_sg" {
  name        = "linky-rds-sg"
  description = "PostgreSQL access from Linky EC2"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "PostgreSQL from Linky EC2"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2_sg.id]
  }

  tags = {
    Name = "linky-rds-sg"
  }
}