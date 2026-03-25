resource "null_resource" "devsecops_setup" {

  depends_on = [aws_instance.devsecops_server]

  connection {
    type        = "ssh"
    host        = aws_instance.devsecops_server.public_ip
    user        = "ubuntu"
    private_key = tls_private_key.generated.private_key_pem
  }  
  provisioner "remote-exec" {
    inline = [

      "sudo apt update -y",
      "sudo apt install docker.io unzip wget apt-transport-https gnupg lsb-release -y",
      "sudo systemctl start docker",
      "sudo systemctl enable docker",

      # Install AWS CLI v2
      "curl \"https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip\" -o \"awscliv2.zip\"",
      "unzip awscliv2.zip",
      "sudo ./aws/install",

      # Install kubectl
      "curl -LO https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl",
      "chmod +x kubectl",
      "sudo mv kubectl /usr/local/bin/",

      # Install eksctl
      "curl --silent --location https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_Linux_amd64.tar.gz | tar xz -C /tmp",
      "sudo mv /tmp/eksctl /usr/local/bin",

      # Install Helm
      "curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash",

      # Install Trivy
      "wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | gpg --dearmor | sudo tee /usr/share/keyrings/trivy.gpg > /dev/null",
      "echo 'deb [signed-by=/usr/share/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main' | sudo tee /etc/apt/sources.list.d/trivy.list",
      "sudo apt update",
      "sudo apt install trivy -y",

      # Run SonarQube container
      "sudo docker run -d --name sonarqube -p 9000:9000 sonarqube:lts-community",
      # Run Prometheus
      "sudo docker run -d --name prometheus -p 9090:9090 prom/prometheus",
      # Run Grafana
      "sudo docker run -d --name grafana -p 3000:3000 grafana/grafana"

    ]
  }
}