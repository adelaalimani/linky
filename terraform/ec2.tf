resource "aws_key_pair" "ec2_deployer_key" {
  key_name   = "linky-deployer-key"
  public_key = file(var.ssh_public_key_path)
}

data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }

  filter {
    name   = "state"
    values = ["available"]
  }
}

resource "aws_instance" "ec2_instance" {
  ami                         = data.aws_ami.amazon_linux.id
  instance_type               = var.instance_type
  key_name                    = aws_key_pair.ec2_deployer_key.key_name
  associate_public_ip_address = true

  subnet_id              = aws_subnet.public_a.id
  vpc_security_group_ids = [aws_security_group.ec2_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  metadata_options {
    http_tokens = "required"
  }

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
    encrypted   = true
  }

  tags = {
    Name = "linky-ec2"
  }
}