output "ec2_public_ip" {

  value = aws_instance.devsecops_server.public_ip
}

output "ecr_repository_url" {

  value = aws_ecr_repository.hackathon_repo.repository_url
}

output "eks_cluster_name" {

  value = aws_eks_cluster.devsecops_cluster.name
}