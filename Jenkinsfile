pipeline { 
    agent { docker 'maven:3.6.1-jdk-12' }
    stages { 
        stage('Build') { 
            steps { 
               sh 'mvn -B clean verify'
            }
        }
    }
}

