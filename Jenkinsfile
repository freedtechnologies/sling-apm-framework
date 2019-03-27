node {

  stage('Checkout') {
    checkout scm
  }

  stage('Package') {
    sh "mvn package"
  }

  cleanWs()

}
