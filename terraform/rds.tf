resource "aws_db_subnet_group" "db_subnet_group" {
  name = "linky-db-subnet-group"

  subnet_ids = [
    aws_subnet.private_a.id,
    aws_subnet.private_b.id
  ]

  tags = {
    Name = "Linky RDS subnet group"
  }
}

resource "aws_db_instance" "linky_db" {
  identifier = "linky-db"

  allocated_storage = 20
  storage_type      = "gp2"

  engine         = "postgres"
  engine_version = "17"
  instance_class = "db.t3.micro"

  db_name  = "linky"
  username = "linky"
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.db_subnet_group.name
  vpc_security_group_ids = [aws_security_group.rds_sg.id]

  publicly_accessible = false
  multi_az            = false

  storage_encrypted = true

  skip_final_snapshot = true
  deletion_protection = false

  backup_retention_period = 1

  tags = {
    Name = "linky-postgres"
  }
}